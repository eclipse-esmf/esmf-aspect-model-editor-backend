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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.eclipse.esmf.ame.constants.ApplicationConstants;
import org.eclipse.esmf.ame.exceptions.FileNotFoundException;
import org.eclipse.esmf.ame.exceptions.FileReadException;
import org.eclipse.esmf.ame.model.FileLoadError;
import org.eclipse.esmf.ame.repository.AspectModelRepository;
import org.eclipse.esmf.ame.services.file.FilePathResolver;
import org.eclipse.esmf.ame.services.models.AspectModelResult;
import org.eclipse.esmf.ame.services.models.FileEntry;
import org.eclipse.esmf.ame.services.models.FileInformation;
import org.eclipse.esmf.ame.services.models.Version;
import org.eclipse.esmf.ame.services.utils.ModelGroupingUtils;
import org.eclipse.esmf.ame.services.validation.ValidationOperations;
import org.eclipse.esmf.ame.services.workspace.WorkspaceReferenceService;
import org.eclipse.esmf.aspectmodel.AspectModelFile;
import org.eclipse.esmf.aspectmodel.UnsupportedVersionException;
import org.eclipse.esmf.aspectmodel.loader.AspectModelLoader;
import org.eclipse.esmf.aspectmodel.resolver.AspectModelFileLoader;
import org.eclipse.esmf.aspectmodel.resolver.exceptions.ModelResolutionException;
import org.eclipse.esmf.aspectmodel.resolver.exceptions.ParserException;
import org.eclipse.esmf.aspectmodel.resolver.modelfile.RawAspectModelFile;
import org.eclipse.esmf.aspectmodel.serializer.AspectSerializer;
import org.eclipse.esmf.aspectmodel.urn.AspectModelUrn;
import org.eclipse.esmf.aspectmodel.validation.services.AspectModelValidator;
import org.eclipse.esmf.metamodel.AspectModel;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.exception.ExceptionUtils;
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
   private final FileLoadErrorHandler fileLoadErrorHandler;
   private final WorkspaceReferenceService workspaceReferenceService;

   @Inject
   public ModelService( final AspectModelValidator aspectModelValidator, final AspectModelLoader aspectModelLoader,
         final AspectModelRepository aspectModelRepository, final AspectModelReader aspectModelReader,
         final FilePathResolver filePathResolver, final ValidationOperations validationOperations, final Path modelPath,
         final FileLoadErrorHandler fileLoadErrorHandler, final WorkspaceReferenceService workspaceReferenceService ) {
      this.aspectModelValidator = aspectModelValidator;
      this.aspectModelLoader = aspectModelLoader;
      this.aspectModelRepository = aspectModelRepository;
      this.aspectModelReader = aspectModelReader;
      this.filePathResolver = filePathResolver;
      this.validationOperations = validationOperations;
      this.modelPath = modelPath;
      this.fileLoadErrorHandler = fileLoadErrorHandler;
      this.workspaceReferenceService = workspaceReferenceService;
   }

   public ModelService( final AspectModelValidator aspectModelValidator, final AspectModelLoader aspectModelLoader,
         final AspectModelRepository aspectModelRepository, final AspectModelReader aspectModelReader,
         final FilePathResolver filePathResolver, final ValidationOperations validationOperations, final Path modelPath ) {
      this( aspectModelValidator, aspectModelLoader, aspectModelRepository, aspectModelReader, filePathResolver,
            validationOperations, modelPath, new FileLoadErrorHandler(), new WorkspaceReferenceService( modelPath ) );
   }

   public Path getModelPath() {
      return modelPath;
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
      return getModels( fileEntries, false );
   }

   /**
    * Loads the files that define the requested elements.
    *
    * @param fileEntries the requested elements
    * @param ignoreMissing if {@code true}, elements that no workspace file defines are left out of the result
    *       instead of failing the whole request, and a file whose own references cannot be resolved is returned
    *       unresolved. Other errors (e.g. syntax errors) still fail the request.
    * @return the files defining the requested elements
    */
   public List<FileInformation> getModels( final List<FileEntry> fileEntries, final boolean ignoreMissing ) {
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
            fileIdentifier = fileEntry.fileName() != null && !fileEntry.fileName().isBlank()
                  ? fileEntry.fileName()
                  : fileEntry.aspectModelUrn();
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
         } catch ( final Throwable t ) {
            if ( ignoreMissing && isResolutionFailure( t ) && addUnresolvedDefiningFile( urn, results ) ) {
               continue;
            }
            errors.addAll( fileLoadErrorHandler.extractErrors( fileIdentifier, t ) );
         }
      }

      if ( !errors.isEmpty() ) {
         throw fileLoadErrorHandler.createBatchLoadException( errors );
      }

      return results;
   }

   private static boolean isResolutionFailure( final Throwable throwable ) {
      return ExceptionUtils.indexOfType( throwable, FileNotFoundException.class ) >= 0
            || ExceptionUtils.indexOfType( throwable, ModelResolutionException.class ) >= 0;
   }

   /**
    * Adds the raw workspace file defining the element, if there is one.
    *
    * @return {@code false} if the workspace could not be searched, so the original error has to be reported
    */
   private boolean addUnresolvedDefiningFile( final AspectModelUrn urn, final List<FileInformation> results ) {
      try {
         findDefiningFile( urn ).map( path -> rawFileInformation( path, urn ) ).ifPresent( results::add );
         return true;
      } catch ( final RuntimeException readError ) {
         LOG.warn( "Could not look up the file defining {}: {}", urn, readError.getMessage() );
         return false;
      }
   }

   /**
    * Returns the raw content of the workspace file defining the element without resolving its references, so that a
    * file whose referenced elements are missing in the workspace can still be opened and repaired.
    *
    * @param urn the requested element
    * @return the defining file, empty if no workspace file defines the element
    */
   public Optional<AspectModelResult> getUnresolvedModel( final AspectModelUrn urn ) {
      return findDefiningFile( urn ).map( path -> {
         final String fileName = path.getFileName().toString();
         return new AspectModelResult( Optional.of( fileName ), readContent( path, fileName ), Optional.of( path.toUri() ) );
      } );
   }

   private Optional<Path> findDefiningFile( final AspectModelUrn urn ) {
      return workspaceReferenceService.findDefiningFile( urn.getNamespaceMainPart(), urn.getVersion(), urn.toString() );
   }

   private FileInformation rawFileInformation( final Path filePath, final AspectModelUrn requestedUrn ) {
      final String fileName = filePath.getFileName().toString();
      final String fileKey = String.format( "%s:%s:%s", requestedUrn.getNamespaceMainPart(), requestedUrn.getVersion(), fileName );
      final String rawContent = readContent( filePath, fileName );
      try {
         return new FileInformation( fileKey, requestedUrn.toString(), readSammVersion( filePath, filePath.toUri() ), rawContent, fileName );
      } catch ( final IOException e ) {
         throw new FileReadException( String.format( "Failed to read content of file '%s'", fileName ), e );
      }
   }

   private static String readContent( final Path filePath, final String fileName ) {
      try {
         return Files.readString( filePath, StandardCharsets.UTF_8 );
      } catch ( final IOException e ) {
         throw new FileReadException( String.format( "Failed to read content of file '%s'", fileName ), e );
      }
   }

   private String readSammVersion( final Path filePath, final URI sourceUri ) throws IOException {
      try ( final InputStream inputStream = Files.newInputStream( filePath ) ) {
         final RawAspectModelFile rawFile = AspectModelFileLoader.load( inputStream, sourceUri );
         return validationOperations.extractSammVersion( rawFile );
      }
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
         final String rawContent = readContent( filePath, fileName );

         final String sammVersion;
         try {
            sammVersion = readSammVersion( filePath, sourceUri );
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
