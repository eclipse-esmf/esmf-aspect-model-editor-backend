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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import org.eclipse.esmf.ame.exceptions.AspectModelBatchLoadException;
import org.eclipse.esmf.ame.exceptions.FileNotFoundException;
import org.eclipse.esmf.ame.exceptions.FileReadException;
import org.eclipse.esmf.ame.model.FileLoadError;
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

      final Optional<ModelResolutionException> mreOpt = findExceptionInCause( throwable, ModelResolutionException.class );
      if ( mreOpt.isPresent() ) {
         return extractErrorsFromResolutionException( fileIdentifier, mreOpt.get() );
      }

      final Optional<ParserException> peOpt = findExceptionInCause( throwable, ParserException.class );
      if ( peOpt.isPresent() ) {
         return List.of( extractErrorFromParserException( fileIdentifier, peOpt.get() ) );
      }

      final String fallbackMessage = throwable.getMessage() != null && !throwable.getMessage().isBlank()
            ? throwable.getMessage()
            : throwable.getClass().getSimpleName();
      return List.of( new FileLoadError( fileIdentifier, fileIdentifier, fallbackMessage ) );
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
         for ( final FileLoadError err : entry.getValue() ) {
            sb.append( "\n• Error: " ).append( err.message() );
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
         return List.of( new FileLoadError( fileIdentifier, fileIdentifier, message ) );
      }

      final List<FileLoadError> result = new ArrayList<>();
      for ( final ModelResolutionViolation violation : violations ) {
         final String sourceDoc = violation.location() != null
               ? violation.location().toString()
               : fileIdentifier;

         final String message;
         if ( violation.element().isPresent() ) {
            message = String.format( "Element '%s' not found", violation.element().get() );
         } else if ( violation.message() != null && !violation.message().isBlank() ) {
            message = violation.message();
         } else if ( violation.cause().isPresent() && violation.cause().get().getMessage() != null ) {
            message = violation.cause().get().getMessage();
         } else if ( mre.getMessage() != null && !mre.getMessage().isBlank() ) {
            message = mre.getMessage();
         } else {
            message = "Model resolution failed";
         }

         final FileLoadError error = new FileLoadError( fileIdentifier, sourceDoc, message );
         if ( !result.contains( error ) ) {
            result.add( error );
         }
      }
      return result;
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
