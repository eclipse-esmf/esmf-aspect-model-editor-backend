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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.esmf.ame.constants.ApplicationConstants;
import org.eclipse.esmf.ame.exceptions.CreateFileException;
import org.eclipse.esmf.ame.exceptions.FileHandlingException;
import org.eclipse.esmf.ame.exceptions.InvalidAspectModelException;
import org.eclipse.esmf.ame.repository.AspectModelRepository;
import org.eclipse.esmf.ame.services.file.FileOperations;
import org.eclipse.esmf.ame.services.file.FilePathResolver;
import org.eclipse.esmf.ame.services.models.MigrationResult;
import org.eclipse.esmf.ame.services.models.Version;
import org.eclipse.esmf.aspectmodel.AspectModelFile;
import org.eclipse.esmf.aspectmodel.edit.AspectChangeManager;
import org.eclipse.esmf.aspectmodel.edit.change.CopyFileWithIncreasedNamespaceVersion;
import org.eclipse.esmf.aspectmodel.edit.change.IncreaseVersion;
import org.eclipse.esmf.aspectmodel.loader.AspectModelLoader;
import org.eclipse.esmf.aspectmodel.serializer.AspectSerializer;
import org.eclipse.esmf.metamodel.AspectModel;
import org.eclipse.esmf.samm.KnownVersion;

import io.micronaut.http.multipart.CompletedFileUpload;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service responsible for migrating aspect models.
 * Handles migration of individual models and entire workspaces.
 */
@Singleton
public class AspectModelMigrator {
   private static final Logger LOG = LoggerFactory.getLogger( AspectModelMigrator.class );

   private final AspectModelRepository aspectModelRepository;
   private final AspectModelLoader aspectModelLoader;
   private final FileOperations fileOperations;
   private final FilePathResolver filePathResolver;
   private final Path modelPath;

   public AspectModelMigrator(
         final AspectModelRepository aspectModelRepository,
         final AspectModelLoader aspectModelLoader,
         final FileOperations fileOperations,
         final FilePathResolver filePathResolver,
         final Path modelPath ) {
      this.aspectModelRepository = aspectModelRepository;
      this.aspectModelLoader = aspectModelLoader;
      this.fileOperations = fileOperations;
      this.filePathResolver = filePathResolver;
      this.modelPath = modelPath;
   }

   /**
    * Migrates a single aspect model from an uploaded file.
    *
    * @param uri the source URI of the model
    * @param upload the uploaded file
    * @return the migrated model as a string
    * @throws InvalidAspectModelException if no migratable model is found
    */
   public String migrate( final URI uri, final CompletedFileUpload upload ) {
      LOG.info( "Migrating model from URI: {}", uri );

      final AspectModel aspectModel = aspectModelRepository.loadFromUpload( upload, uri );

      return aspectModel.files().stream()
            .filter( this::isFromValidSource )
            .findFirst()
            .map( AspectSerializer.INSTANCE::aspectModelFileToString )
            .orElseThrow( () -> new InvalidAspectModelException(
                  String.format( "No valid Aspect Model found to migrate in the uploaded file with URI '%s'", uri ) ) );
   }

   /**
    * Migrates all models in a workspace.
    *
    * @param allNamespaces map of namespaces to versions
    * @param setNewVersion whether to increase the version
    * @param metaModelStoragePath the storage path
    * @return migration result with success status and errors
    */
   public MigrationResult migrateWorkspace(
         final java.util.Map<String, List<Version>> allNamespaces,
         final boolean setNewVersion,
         final Path metaModelStoragePath ) {
      LOG.info( "Starting workspace migration (setNewVersion={})", setNewVersion );

      final List<String> errors = new ArrayList<>();

      try {
         allNamespaces.forEach( ( namespace, versions ) ->
               versions.forEach( version ->
                     processVersion( namespace, version, setNewVersion, errors, metaModelStoragePath )
               )
         );

         LOG.info( "Workspace migration completed with {} errors", errors.size() );
         return new MigrationResult( true, errors );
      } catch ( final Exception e ) {
         LOG.error( "Workspace migration failed", e );
         errors.add( "Workspace migration failed: " + ( e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName() ) );
         return new MigrationResult( false, errors );
      }
   }

   /**
    * Formats an aspect model (returns migrated/formatted content).
    *
    * @param uri the source URI
    * @param upload the uploaded file
    * @return the formatted model as a string
    */
   public String format( final URI uri, final CompletedFileUpload upload ) {
      LOG.debug( "Formatting model from URI: {}", uri );
      return migrate( uri, upload );
   }

   private boolean isFromValidSource( final AspectModelFile file ) {
      return file.sourceLocation()
            .map( source -> {
               final String scheme = source.getScheme();
               return ApplicationConstants.Schemes.BLOB.equals( scheme ) || ApplicationConstants.Schemes.FILE.equals( scheme );
            } )
            .orElse( false );
   }

   private void processVersion( final String namespace, final Version version,
         final boolean setNewVersion, final List<String> errors, final Path metaModelStoragePath ) {
      version.models().forEach( model -> {
         try {
            if ( !shouldMigrate( model.version() ) ) {
               return;
            }

            final Path aspectModelPath = filePathResolver.constructModelPath(
                  modelPath, namespace, version.version(), model.name() );
            final AspectModel aspectModel = aspectModelLoader.load( aspectModelPath.toFile() );

            if ( setNewVersion ) {
               migrateWithVersionChange( aspectModel, errors, metaModelStoragePath );
            } else {
               AspectSerializer.INSTANCE.write( aspectModel );
            }
         } catch ( final Exception e ) {
            final String errorMsg = String.format( "Error processing model '%s' in namespace '%s' (version %s): %s",
                  model.name(), namespace, version.version(), e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName() );
            LOG.error( errorMsg, e );
            errors.add( errorMsg );
         }
      } );
   }

   private boolean shouldMigrate( final String versionString ) {
      return KnownVersion.fromVersionString( versionString )
            .filter( v -> !KnownVersion.getLatest().equals( v ) )
            .isPresent();
   }

   private void migrateWithVersionChange( final AspectModel aspectModel,
         final List<String> errors, final Path metaModelStoragePath ) {
      try {
         final AspectModelFile originalFile = aspectModel.files().getFirst();
         final AspectChangeManager changeManager = new AspectChangeManager( aspectModel );
         changeManager.applyChange( new CopyFileWithIncreasedNamespaceVersion(
               originalFile, IncreaseVersion.MAJOR ) );

         final List<AspectModelFile> newFiles = aspectModel.files().stream()
               .filter( file -> !file.namespaceUrn().getVersion()
                     .equals( originalFile.namespaceUrn().getVersion() ) )
               .toList();

         if ( newFiles.size() != 1 ) {
            LOG.warn( "Expected 1 new file, got {}", newFiles.size() );
            return;
         }

         final AspectModelFile updatedFile = newFiles.getFirst();
         final URI sourceLocation = updatedFile.sourceLocation()
               .orElseThrow( () -> new IllegalStateException( "Source location missing" ) );

         if ( fileOperations.exists( Path.of( sourceLocation ) ) ) {
            final String errorMsg = String.format(
                  "A new version of the Aspect Model: %s with Version: %s already exists",
                  updatedFile.filename().orElse( "unknown" ),
                  originalFile.namespaceUrn().getVersion() );
            LOG.info( errorMsg );
            errors.add( errorMsg );
            return;
         }

         final Path filePath = filePathResolver.resolveFilePath(
               updatedFile.namespaceUrn(),
               updatedFile.filename().orElseThrow( () ->
                     new FileHandlingException( "Filename missing" ) ),
               metaModelStoragePath );

         fileOperations.createFile( filePath );
         AspectSerializer.INSTANCE.write( updatedFile );

         LOG.info( "Migrated file with version increase: {}", filePath );
      } catch ( final IOException e ) {
         throw new CreateFileException( "Cannot create file during migration", e );
      }
   }
}

