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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Supplier;

import org.eclipse.esmf.ame.constants.ApplicationConstants;
import org.eclipse.esmf.ame.exceptions.AspectModelBatchLoadException;
import org.eclipse.esmf.ame.exceptions.FileNotFoundException;
import org.eclipse.esmf.ame.exceptions.FileReadException;
import org.eclipse.esmf.ame.model.FileLoadError;
import org.eclipse.esmf.ame.repository.AspectModelRepository;
import org.eclipse.esmf.ame.services.file.FilePathResolver;
import org.eclipse.esmf.ame.services.models.FileEntry;
import org.eclipse.esmf.ame.services.models.FileInformation;
import org.eclipse.esmf.ame.services.models.Version;
import org.eclipse.esmf.ame.services.utils.ModelGroupingUtils;
import org.eclipse.esmf.ame.services.validation.ValidationOperations;
import org.eclipse.esmf.aspectmodel.AspectModelFile;
import org.eclipse.esmf.aspectmodel.UnsupportedVersionException;
import org.eclipse.esmf.aspectmodel.loader.AspectModelLoader;
import org.eclipse.esmf.aspectmodel.resolver.AspectModelFileLoader;
import org.eclipse.esmf.aspectmodel.resolver.ModelResolutionViolation;
import org.eclipse.esmf.aspectmodel.resolver.exceptions.ModelResolutionException;
import org.eclipse.esmf.aspectmodel.resolver.exceptions.ParserException;
import org.eclipse.esmf.aspectmodel.resolver.modelfile.RawAspectModelFile;
import org.eclipse.esmf.aspectmodel.serializer.AspectSerializer;
import org.eclipse.esmf.aspectmodel.urn.AspectModelUrn;
import org.eclipse.esmf.aspectmodel.validation.services.AspectModelValidator;
import org.eclipse.esmf.metamodel.AspectModel;

import jakarta.inject.Singleton;
import org.apache.jena.riot.RiotException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service class for managing aspect models.
 * Provides methods to get, create, save, delete, validate, migrate, and format aspect models.
 */
@Singleton
public class ModelService {
   private static final Logger LOG = LoggerFactory.getLogger( ModelService.class );

   private final AspectModelValidator aspectModelValidator;
   private final AspectModelLoader aspectModelLoader;
   private final AspectModelRepository aspectModelRepository;
   private final AspectModelReader aspectModelReader;
   private final FilePathResolver filePathResolver;
   private final ValidationOperations validationOperations;
   private final Path modelPath;

   public ModelService( final AspectModelValidator aspectModelValidator, final AspectModelLoader aspectModelLoader,
         final AspectModelRepository aspectModelRepository, final AspectModelReader aspectModelReader,
         final FilePathResolver filePathResolver, final ValidationOperations validationOperations, final Path modelPath ) {
      this.aspectModelValidator = aspectModelValidator;
      this.aspectModelLoader = aspectModelLoader;
      this.aspectModelRepository = aspectModelRepository;
      this.aspectModelReader = aspectModelReader;
      this.filePathResolver = filePathResolver;
      this.validationOperations = validationOperations;
      this.modelPath = modelPath;
   }

   public Map<String, List<Version>> getAllNamespaces() {
      try {
         return new ModelGroupingUtils( aspectModelLoader, aspectModelValidator ).groupModelsByNamespaceAndVersion(
               aspectModelLoader.listContents() );
      } catch ( final ModelResolutionException e ) {
         LOG.error( e.getMessage() );
         throw new FileNotFoundException( "The models folder was not found. Please restart the application to create it automatically." );
      } catch ( final UnsupportedVersionException e ) {
         LOG.error( "{} There is a loose {} file somewhere — remove it along with any other non-standardized files.",
               ApplicationConstants.ErrorMessages.SAMM_STRUCTURE_INFO, ApplicationConstants.FileExtensions.TTL, e );
         throw new FileReadException( ApplicationConstants.ErrorMessages.SAMM_STRUCTURE_INFO + " Remove all non-standardized files." );
      }
   }

   public List<FileInformation> getModels( final List<FileEntry> fileEntries ) {
      final List<FileInformation> results = new ArrayList<>();
      final List<FileLoadError> errors = new ArrayList<>();

      for ( final FileEntry fileEntry : fileEntries ) {
         final Supplier<AspectModel> lazySupplier;
         final String fileIdentifier;
         final AspectModelUrn urn;

         if ( fileEntry.absoluteName() != null ) {
            final Path filePath = filePathResolver.resolveFromFileEntry( fileEntry, modelPath );
            lazySupplier = aspectModelRepository.loadFromFiles( List.of( filePath.toFile() ) );
            fileIdentifier = filePath.toString();

            urn = AspectModelUrn.from( fileEntry.aspectModelUrn() ).getOrElseThrow(
                  () -> new IllegalArgumentException( String.format( "Invalid aspect model URN: '%s'", fileEntry.aspectModelUrn() ) ) );
         } else {
            urn = AspectModelUrn.from( fileEntry.aspectModelUrn() ).getOrElseThrow(
                  () -> new IllegalArgumentException( String.format( "Invalid aspect model URN: '%s'", fileEntry.aspectModelUrn() ) ) );

            lazySupplier = aspectModelRepository.loadByUrns( List.of( urn ) );
            fileIdentifier = fileEntry.aspectModelUrn();
         }

         try {
            final AspectModel aspectModel = lazySupplier.get();

            final AspectModelFile aspectModelFile = aspectModel.files().stream()
                  .filter( file -> aspectModelReader.containsElement( file, urn ) )
                  .filter( aspectModelReader::hasValidCasing )
                  .findFirst()
                  .orElseThrow( () -> new FileNotFoundException(
                        String.format( "Aspect Model not found for URN '%s' in file '%s'", urn, fileIdentifier ) ) );

            results.add( convertToFileInformation( aspectModelFile, urn ) );
         } catch ( final FileNotFoundException fnfe ) {
            errors.add( new FileLoadError( fileIdentifier, fileIdentifier, fnfe.getMessage() ) );
         } catch ( final FileReadException fre ) {
            errors.add( new FileLoadError( fileIdentifier, fileIdentifier, fre.getMessage() ) );
         } catch ( final Throwable t ) {
            final Optional<ModelResolutionException> mreOpt = findModelResolutionException( t );
            if ( mreOpt.isPresent() ) {
               errors.addAll( extractErrorsFromResolutionException( fileIdentifier, mreOpt.get() ) );
            } else {
               final Optional<ParserException> peOpt = findParserException( t );
               if ( peOpt.isPresent() ) {
                  errors.add( extractErrorFromParserException( fileIdentifier, peOpt.get() ) );
               } else {
                  final String msg = t.getMessage() != null && !t.getMessage().isBlank()
                        ? t.getMessage()
                        : t.getClass().getSimpleName();
                  errors.add( new FileLoadError( fileIdentifier, fileIdentifier, msg ) );
               }
            }
         }
      }

      if ( !errors.isEmpty() ) {
         final Map<String, List<FileLoadError>> errorsByFile = new TreeMap<>();
         for ( final FileLoadError err : errors ) {
            errorsByFile.computeIfAbsent( err.fileIdentifier(), k -> new ArrayList<>() ).add( err );
         }
         for ( final List<FileLoadError> fileErrors : errorsByFile.values() ) {
            fileErrors.sort( Comparator.comparing( FileLoadError::sourceDocument )
                  .thenComparing( FileLoadError::message ) );
         }

         final String aggregatedMessage = formatErrorMessage( errorsByFile );
         throw new AspectModelBatchLoadException( aggregatedMessage, errors );
      }

      return results;
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

   private Optional<ModelResolutionException> findModelResolutionException( final Throwable throwable ) {
      Throwable current = throwable;
      while ( current != null ) {
         if ( current instanceof final ModelResolutionException mre ) {
            return Optional.of( mre );
         }
         current = current.getCause();
      }
      return Optional.empty();
   }

   private Optional<ParserException> findParserException( final Throwable throwable ) {
      Throwable current = throwable;
      while ( current != null ) {
         if ( current instanceof final ParserException pe ) {
            return Optional.of( pe );
         }
         current = current.getCause();
      }
      return Optional.empty();
   }

   private String formatErrorMessage( final Map<String, List<FileLoadError>> errorsByFile ) {
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

   private FileInformation convertToFileInformation( final AspectModelFile aspectModelFile, final AspectModelUrn requestedUrn ) {
      final AspectModelUrn aspectModelUrn = aspectModelFile.namespaceUrn();
      final URI sourceUri = getSourceUri( aspectModelFile );

      final Path filePath = sourceUri != null ? Path.of( sourceUri ) : null;
      final String fileName = aspectModelFile.filename()
            .orElse( filePath != null ? filePath.getFileName().toString() : "" );

      final String fileKey = String.format( "%s:%s:%s", aspectModelUrn.getNamespaceMainPart(), aspectModelUrn.getVersion(),
            fileName.isEmpty() ? requestedUrn.getName() : fileName );

      if ( filePath != null && Files.exists( filePath ) ) {
         final String rawContent;
         try {
            rawContent = Files.readString( filePath, StandardCharsets.UTF_8 );
         } catch ( final IOException e ) {
            throw new FileReadException( String.format( "Failed to read content of file '%s'", fileName ), e );
         }

         final String sammVersion;
         try ( final InputStream inputStream = Files.newInputStream( filePath ) ) {
            final RawAspectModelFile rawFile = AspectModelFileLoader.load( inputStream, sourceUri );
            sammVersion = validationOperations.extractSammVersion( rawFile );
         } catch ( final ParserException | RiotException | IOException e ) {
            return new FileInformation( fileKey, requestedUrn.toString(), validationOperations.extractSammVersion( aspectModelFile ),
                  rawContent, fileName );
         }

         return new FileInformation( fileKey, requestedUrn.toString(), sammVersion, rawContent, fileName );
      }

      final String sammVersion = validationOperations.extractSammVersion( aspectModelFile );
      final String content = AspectSerializer.INSTANCE.aspectModelFileToString( aspectModelFile );
      return new FileInformation( fileKey, requestedUrn.toString(), sammVersion, content, fileName );
   }

   private URI getSourceUri( final AspectModelFile aspectModelFile ) {
      if ( aspectModelFile.sourceUri() != null ) {
         return aspectModelFile.sourceUri();
      }
      return aspectModelFile.sourceLocation().orElse( null );
   }
}
