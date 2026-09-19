/*
 * Copyright (c) 2026 Robert Bosch Manufacturing Solutions GmbH
 *
 * See the AUTHORS file(s) distributed with this work for
 * additional information regarding authorship.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package org.eclipse.esmf.ame.services;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import org.eclipse.esmf.ame.exceptions.AspectModelBatchLoadException;
import org.eclipse.esmf.ame.exceptions.FileNotFoundException;
import org.eclipse.esmf.ame.exceptions.FileReadException;
import org.eclipse.esmf.ame.exceptions.InvalidAspectModelException;
import org.eclipse.esmf.ame.model.FileLoadError;
import org.eclipse.esmf.ame.services.utils.TurtleElementContext;
import org.eclipse.esmf.ame.services.utils.TurtleElementResolver;
import org.eclipse.esmf.aspectmodel.ValueParsingException;
import org.eclipse.esmf.aspectmodel.resolver.ModelResolutionViolation;
import org.eclipse.esmf.aspectmodel.resolver.exceptions.ModelResolutionException;
import org.eclipse.esmf.aspectmodel.resolver.exceptions.ParserException;

import jakarta.inject.Singleton;

/**
 * Service responsible for extracting, sorting, and formatting errors encountered during aspect model loading.
 */
@Singleton
public class FileLoadErrorHandler {

   /**
    * Extracts one or more {@link FileLoadError}s from a thrown exception.
    *
    * @param fileIdentifier the file identifier or path being loaded
    * @param throwable the caught exception
    * @return list of extracted file load errors
    */
   public List<FileLoadError> extractErrors( final String fileIdentifier, final Throwable throwable ) {
      if ( throwable instanceof final FileNotFoundException fnfe ) {
         return List.of( new FileLoadError( fileIdentifier, fileIdentifier, fnfe.getMessage() ) );
      }

      if ( throwable instanceof final FileReadException fre ) {
         return List.of( new FileLoadError( fileIdentifier, fileIdentifier, fre.getMessage() ) );
      }

      final Optional<ValueParsingException> vpeOpt = findExceptionInCause( throwable, ValueParsingException.class );
      if ( vpeOpt.isPresent() ) {
         return List.of( extractErrorFromValueParsingException( fileIdentifier, vpeOpt.get() ) );
      }

      final Optional<ModelResolutionException> mreOpt = findExceptionInCause( throwable, ModelResolutionException.class );
      if ( mreOpt.isPresent() ) {
         return extractErrorsFromResolutionException( fileIdentifier, mreOpt.get() );
      }

      final Optional<ParserException> peOpt = findExceptionInCause( throwable, ParserException.class );
      if ( peOpt.isPresent() ) {
         return List.of( extractErrorFromParserException( fileIdentifier, peOpt.get() ) );
      }

      final Optional<InvalidAspectModelException> iameOpt = findExceptionInCause( throwable, InvalidAspectModelException.class );
      if ( iameOpt.isPresent() ) {
         return extractErrorsFromInvalidAspectModel( fileIdentifier, iameOpt.get() );
      }

      final String rawMessage = throwable.getMessage();
      if ( rawMessage != null && !rawMessage.isBlank() ) {
         if ( rawMessage.contains( ";" ) || rawMessage.contains( "\n" ) ) {
            return splitMultiMessage( fileIdentifier, fileIdentifier, rawMessage );
         }
         return List.of( new FileLoadError( fileIdentifier, fileIdentifier, rawMessage ) );
      }

      final Throwable cause = throwable.getCause();
      if ( cause != null && cause != throwable ) {
         return extractErrors( fileIdentifier, cause );
      }

      return List.of( new FileLoadError( fileIdentifier, fileIdentifier, throwable.getClass().getSimpleName() ) );
   }

   /**
    * Groups errors by file in alphabetical order and sorts errors within each file.
    *
    * @param errors list of collected errors
    * @return sorted map of errors grouped by file identifier
    */
   public Map<String, List<FileLoadError>> groupAndSortErrors( final List<FileLoadError> errors ) {
      final Map<String, List<FileLoadError>> errorsByFile = new TreeMap<>();
      for ( final FileLoadError err : errors ) {
         errorsByFile.computeIfAbsent( err.fileIdentifier(), k -> new ArrayList<>() ).add( err );
      }

      for ( final List<FileLoadError> fileErrors : errorsByFile.values() ) {
         fileErrors.sort( Comparator.comparing( FileLoadError::sourceDocument )
               .thenComparing( FileLoadError::message ) );
      }

      return errorsByFile;
   }

   /**
    * Formats the collected errors into a structured, readable message suitable for frontend display.
    *
    * @param errors list of collected errors
    * @return formatted multiline error message
    */
   public String formatErrorMessage( final List<FileLoadError> errors ) {
      final Map<String, List<FileLoadError>> errorsByFile = groupAndSortErrors( errors );
      final StringBuilder sb = new StringBuilder();

      if ( errorsByFile.size() == 1 ) {
         sb.append( "Failed to load aspect model file:\n\n" );
      } else {
         sb.append( "Failed to load aspect model files:\n\n" );
      }

      boolean firstFile = true;
      for ( final Map.Entry<String, List<FileLoadError>> entry : errorsByFile.entrySet() ) {
         if ( !firstFile ) {
            sb.append( "\n\n" );
         }
         firstFile = false;

         sb.append( "File: " ).append( entry.getKey() );
         final Set<String> seenMessages = new LinkedHashSet<>();
         for ( final FileLoadError err : entry.getValue() ) {
            if ( seenMessages.add( err.message() ) ) {
               sb.append( "\n• Error: " ).append( err.message() );
            }
         }
      }

      return sb.toString();
   }

   /**
    * Creates an {@link AspectModelBatchLoadException} from the given list of errors.
    *
    * @param errors list of collected errors
    * @return configured batch load exception with formatted message and error details
    */
   public AspectModelBatchLoadException createBatchLoadException( final List<FileLoadError> errors ) {
      final String formattedMessage = formatErrorMessage( errors );
      return new AspectModelBatchLoadException( formattedMessage, errors );
   }

   private List<FileLoadError> extractErrorsFromResolutionException(
         final String fileIdentifier, final ModelResolutionException mre ) {
      final List<ModelResolutionViolation> violations = mre.getCheckedLocations();
      if ( violations == null || violations.isEmpty() ) {
         final String message = mre.getMessage() != null && !mre.getMessage().isBlank()
               ? mre.getMessage()
               : "Model resolution failed";
         return splitMultiMessage( fileIdentifier, fileIdentifier, message );
      }

      final List<FileLoadError> result = new ArrayList<>();
      for ( final ModelResolutionViolation violation : violations ) {
         final String sourceDoc = violation.location() != null
               ? violation.location().toString()
               : fileIdentifier;

         final String message = buildViolationMessage( violation, mre.getMessage() );
         final FileLoadError error = new FileLoadError( fileIdentifier, sourceDoc, message );
         if ( !result.contains( error ) ) {
            result.add( error );
         }
      }

      if ( result.isEmpty() ) {
         final String fallback = mre.getMessage() != null && !mre.getMessage().isBlank()
               ? mre.getMessage()
               : "Model resolution failed";
         result.add( new FileLoadError( fileIdentifier, fileIdentifier, fallback ) );
      }

      return result;
   }

   private String buildViolationMessage( final ModelResolutionViolation violation, final String fallback ) {
      if ( violation.cause().isPresent() ) {
         final Optional<ValueParsingException> vpeOpt = findExceptionInCause( violation.cause().get(), ValueParsingException.class );
         if ( vpeOpt.isPresent() ) {
            final ValueParsingException vpe = vpeOpt.get();
            String doc = vpe.getSourceDocument();
            if ( doc == null || doc.isBlank() ) {
               doc = tryReadContent( null, violation.location() );
            }
            final TurtleElementContext context = TurtleElementResolver.resolveContext(
                  doc, vpe.getLine(), vpe.getColumn() );
            return TurtleElementResolver.formatValueParsingMessage( vpe, context );
         }
      }

      final StringBuilder sb = new StringBuilder();

      if ( violation.element().isPresent() ) {
         sb.append( "Element '" ).append( violation.element().get() ).append( "'" );
      }

      final String detailMessage = violation.message();
      final String causeMessage = violation.cause()
            .map( Throwable::getMessage )
            .filter( m -> m != null && !m.isBlank() )
            .orElse( null );

      if ( detailMessage != null && !detailMessage.isBlank() ) {
         if ( sb.length() > 0 ) {
            sb.append( ": " );
         }
         sb.append( detailMessage );
      }

      if ( causeMessage != null && !causeMessage.isBlank() ) {
         if ( detailMessage == null || !detailMessage.contains( causeMessage ) ) {
            if ( sb.length() > 0 ) {
               sb.append( " (" ).append( causeMessage ).append( ")" );
            } else {
               sb.append( causeMessage );
            }
         }
      }

      if ( sb.length() == 0 ) {
         return ( fallback != null && !fallback.isBlank() ) ? fallback : "Model resolution failed";
      }

      return sb.toString();
   }

   private List<FileLoadError> extractErrorsFromInvalidAspectModel(
         final String fileIdentifier, final InvalidAspectModelException iame ) {
      final String rawMessage = iame.getMessage();
      if ( rawMessage == null || rawMessage.isBlank() ) {
         return List.of( new FileLoadError( fileIdentifier, fileIdentifier, "Aspect Model is invalid" ) );
      }

      String text = rawMessage;
      final String prefix = "Aspect Model has invalid syntax: ";
      if ( text.startsWith( prefix ) ) {
         text = text.substring( prefix.length() );
      }

      return splitMultiMessage( fileIdentifier, fileIdentifier, text );
   }

   private List<FileLoadError> splitMultiMessage( final String fileIdentifier, final String sourceDoc, final String rawMessage ) {
      final List<FileLoadError> list = new ArrayList<>();
      final String[] parts = rawMessage.split( ";\\s*|\\r?\\n+" );
      for ( final String part : parts ) {
         final String trimmed = part.trim();
         if ( !trimmed.isBlank() ) {
            list.add( new FileLoadError( fileIdentifier, sourceDoc, trimmed ) );
         }
      }
      if ( list.isEmpty() ) {
         list.add( new FileLoadError( fileIdentifier, sourceDoc, rawMessage.trim() ) );
      }
      return list;
   }

   private FileLoadError extractErrorFromParserException(
         final String fileIdentifier, final ParserException pe ) {
      final String sourceDoc = pe.getSourceLocation() != null
            ? pe.getSourceLocation().toString()
            : ( pe.getSourceDocument() != null && !pe.getSourceDocument().isBlank()
                  ? pe.getSourceDocument()
                  : fileIdentifier );

      final String message;
      if ( pe.getLine() > 0 ) {
         message = String.format( "Parsing error at line %d, column %d: %s",
               pe.getLine(), pe.getColumn(), pe.getMessage() );
      } else {
         message = pe.getMessage() != null ? pe.getMessage() : "Parsing failed";
      }
      return new FileLoadError( fileIdentifier, sourceDoc, message );
   }

   private FileLoadError extractErrorFromValueParsingException(
         final String fileIdentifier, final ValueParsingException vpe ) {
      String documentContent = vpe.getSourceDocument();
      if ( documentContent == null || documentContent.isBlank() ) {
         documentContent = tryReadContent( fileIdentifier, vpe.getSourceLocation() );
      }

      final TurtleElementContext context = TurtleElementResolver.resolveContext(
            documentContent, vpe.getLine(), vpe.getColumn() );

      final String sourceDoc = vpe.getSourceLocation() != null
            ? vpe.getSourceLocation().toString()
            : fileIdentifier;

      final String message = TurtleElementResolver.formatValueParsingMessage( vpe, context );
      return new FileLoadError( fileIdentifier, sourceDoc, message );
   }

   private String tryReadContent( final String fileIdentifier, final URI sourceLocation ) {
      if ( fileIdentifier != null ) {
         try {
            final Path path = Path.of( fileIdentifier );
            if ( Files.isRegularFile( path ) ) {
               return Files.readString( path );
            }
         } catch ( final Exception ignored ) {
            // Ignore and fall back to sourceLocation
         }
      }
      if ( sourceLocation != null ) {
         try {
            final Path path = Paths.get( sourceLocation );
            if ( Files.isRegularFile( path ) ) {
               return Files.readString( path );
            }
         } catch ( final Exception ignored ) {
            // Ignore fallback
         }
      }
      return null;
   }

   @SuppressWarnings( "unchecked" )
   private <T extends Throwable> Optional<T> findExceptionInCause( final Throwable throwable, final Class<T> targetClass ) {
      Throwable current = throwable;
      while ( current != null ) {
         if ( targetClass.isInstance( current ) ) {
            return Optional.of( (T) current );
         }
         current = current.getCause();
      }
      return Optional.empty();
   }
}
