/*
 * Copyright (c) 2025 Robert Bosch Manufacturing Solutions GmbH
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
import java.util.Map;
import java.util.function.Supplier;

import org.eclipse.esmf.ame.exceptions.CreateFileException;
import org.eclipse.esmf.ame.exceptions.FileHandlingException;
import org.eclipse.esmf.ame.exceptions.FileNotFoundException;
import org.eclipse.esmf.ame.exceptions.FileReadException;
import org.eclipse.esmf.ame.exceptions.InvalidAspectModelException;
import org.eclipse.esmf.ame.services.models.MigrationResult;
import org.eclipse.esmf.ame.services.models.Version;
import org.eclipse.esmf.ame.services.utils.ModelGroupingUtils;
import org.eclipse.esmf.ame.services.utils.ModelUtils;
import org.eclipse.esmf.ame.validation.model.ViolationError;
import org.eclipse.esmf.ame.validation.model.ViolationReport;
import org.eclipse.esmf.ame.validation.utils.ValidationUtils;
import org.eclipse.esmf.aspectmodel.AspectModelFile;
import org.eclipse.esmf.aspectmodel.UnsupportedVersionException;
import org.eclipse.esmf.aspectmodel.edit.AspectChangeManager;
import org.eclipse.esmf.aspectmodel.edit.change.CopyFileWithIncreasedNamespaceVersion;
import org.eclipse.esmf.aspectmodel.edit.change.IncreaseVersion;
import org.eclipse.esmf.aspectmodel.loader.AspectModelLoader;
import org.eclipse.esmf.aspectmodel.resolver.exceptions.ModelResolutionException;
import org.eclipse.esmf.aspectmodel.serializer.AspectSerializer;
import org.eclipse.esmf.aspectmodel.shacl.violation.Violation;
import org.eclipse.esmf.aspectmodel.urn.AspectModelUrn;
import org.eclipse.esmf.aspectmodel.validation.services.AspectModelValidator;
import org.eclipse.esmf.metamodel.AspectModel;
import org.eclipse.esmf.samm.KnownVersion;

import io.micronaut.http.multipart.CompletedFileUpload;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service class for managing aspect models.
 * Provides methods to get, create, save, delete, validate, migrate, and format aspect models.
 */
@Singleton
public class ModelService {
   private static final Logger LOG = LoggerFactory.getLogger( ModelService.class );

   private static final String sammStructureInfo =
         "Please check whether the SAMM structure has been followed in the workspace: " + "Namespace/Version/Aspect model.";

   private final AspectModelValidator aspectModelValidator;
   private final AspectModelLoader aspectModelLoader;
   private final Path modelPath;

   public ModelService( final AspectModelValidator aspectModelValidator, final AspectModelLoader aspectModelLoader, final Path modelPath ) {
      this.aspectModelValidator = aspectModelValidator;
      this.aspectModelLoader = aspectModelLoader;
      this.modelPath = modelPath;
   }

   public String getModel( final AspectModelUrn aspectModelUrn, final String filePath ) {
      try {
         final AspectModel aspectModel = ( filePath != null ) ?
               ModelUtils.loadModelFromFile( modelPath, filePath, aspectModelLoader ) :
               loadModelFromUrn( aspectModelUrn );
         validateModel( aspectModel );

         return aspectModel.files().stream().filter( a -> a.elements().stream().anyMatch( e -> e.urn().equals( aspectModelUrn ) ) )
               .findFirst().map( AspectSerializer.INSTANCE::aspectModelFileToString )
               .orElseThrow( () -> new FileNotFoundException( "Aspect Model not found" ) );
      } catch ( final ModelResolutionException e ) {
         throw new FileNotFoundException( e.getMessage(), e );
      }
   }

   private AspectModel loadModelFromUrn( final AspectModelUrn aspectModelUrn ) {
      final Supplier<AspectModel> aspectModelSupplier = ModelUtils.getAspectModelSupplier( aspectModelUrn, aspectModelLoader );
      return aspectModelSupplier.get();
   }

   private void validateModel( final AspectModel aspectModel ) {
      final List<Violation> violations = aspectModelValidator.validateModel( aspectModel );
      if ( violations.stream().anyMatch( ValidationUtils.isInvalidSyntaxViolation() ) ) {
         throw new FileReadException( "Aspect Model is not valid" );
      }
   }

   public void createOrSaveModel( final String turtleData, final AspectModelUrn aspectModelUrn, final String fileName,
         final Path storagePath ) {
      try {
         final Path newFile = ModelUtils.createFilePath( aspectModelUrn, fileName, storagePath );

         final Supplier<AspectModel> aspectModelSupplier = ModelUtils.getAspectModelSupplier( turtleData, newFile.toFile(),
               aspectModelLoader );
         final List<Violation> violations = aspectModelValidator.validateModel( aspectModelSupplier );

         ModelUtils.throwIfViolationPresent( violations, ValidationUtils.isInvalidSyntaxViolation(), new FileReadException(
               violations.stream().filter( ValidationUtils.isInvalidSyntaxViolation() ).findFirst().map( Violation::message )
                     .orElse( "Aspect Model is not valid" ) ) );

         ModelUtils.throwIfViolationPresent( violations, ValidationUtils.isProcessingViolation(), new CreateFileException(
               violations.stream().filter( ValidationUtils.isProcessingViolation() ).findFirst().map( Violation::message )
                     .orElse( "Processing violation" ) ) );

         ModelUtils.createFile( newFile );
         AspectSerializer.INSTANCE.write( aspectModelSupplier.get().files().getFirst() );
      } catch ( final IOException e ) {
         throw new CreateFileException( String.format( "Cannot create file %s on workspace", aspectModelUrn ), e );
      }
   }

   public void deleteModel( final AspectModelUrn aspectModelUrn ) {
      final AspectModelFile aspectModelFile = aspectModelLoader.load( aspectModelUrn ).files().getFirst();
      ModelUtils.deleteEmptyFiles( aspectModelFile );
   }

   public ViolationReport validateModel( final URI uri, final CompletedFileUpload aspectModelFile ) {
      final Supplier<AspectModel> aspectModelSupplier = () -> aspectModelLoader.load(
            ModelUtils.openInputStreamFromUpload( aspectModelFile ), uri );
      final List<Violation> violations = aspectModelValidator.validateModel( aspectModelSupplier );
      final List<ViolationError> violationErrors = ValidationUtils.violationErrors( violations );
      return new ViolationReport( violationErrors );
   }

   public String migrateModel( final URI uri, final CompletedFileUpload aspectModelFile ) {
      final AspectModel aspectModel = aspectModelLoader.load( ModelUtils.openInputStreamFromUpload( aspectModelFile ), uri );

      return aspectModel.files().stream()
            .filter( a -> a.sourceLocation().map( source -> source.getScheme().equals( "blob" ) ).orElse( false ) ).findFirst()
            .map( AspectSerializer.INSTANCE::aspectModelFileToString )
            .orElseThrow( () -> new InvalidAspectModelException( "No aspect model found to migrate" ) );
   }

   public String getFormattedModel( final URI uri, final CompletedFileUpload aspectModelFile ) {
      final AspectModel aspectModel = aspectModelLoader.load( ModelUtils.openInputStreamFromUpload( aspectModelFile ), uri );

      return aspectModel.files().stream()
            .filter( a -> a.sourceLocation().map( source -> source.getScheme().equals( "blob" ) ).orElse( false ) ).findFirst()
            .map( AspectSerializer.INSTANCE::aspectModelFileToString )
            .orElseThrow( () -> new InvalidAspectModelException( "No aspect model found to formate" ) );
   }

   public Map<String, List<Version>> getAllNamespaces( final boolean onlyAspectModels ) {
      try {
         return new ModelGroupingUtils( aspectModelLoader ).groupModelsByNamespaceAndVersion( aspectModelLoader.listContents(),
               onlyAspectModels );
      } catch ( final UnsupportedVersionException e ) {
         LOG.error( "{} There is a loose .ttl file somewhere — remove it along with any other non-standardized files.", sammStructureInfo,
               e );
         throw new FileReadException( sammStructureInfo + " Remove all non-standardized files." );
      }
   }

   public MigrationResult migrateWorkspace( final boolean setNewVersion, final Path metaModelStoragePath ) {
      final List<String> errors = new ArrayList<>();

      try {
         getAllNamespaces( false ).forEach(
               ( namespace, versions ) -> versions.forEach(
                     version -> processVersion( namespace, version, setNewVersion, errors, metaModelStoragePath ) ) );
         return new MigrationResult( true, errors );
      } catch ( final Exception e ) {
         errors.add( e.getMessage() );
         return new MigrationResult( false, errors );
      }
   }

   private void processVersion( final String namespace, final Version version, final boolean setNewVersion, final List<String> errors,
         final Path metaModelStoragePath ) {
      version.getModels().forEach( model -> {
         try {
            final boolean isNotLatestKnownVersion = KnownVersion.fromVersionString( model.getVersion() )
                  .filter( v -> KnownVersion.getLatest().equals( v ) ).isPresent();

            if ( isNotLatestKnownVersion ) {
               return;
            }

            final Path aspectModelPath = ModelUtils.constructModelPath( modelPath, namespace, version.getVersion(), model.getModel() );
            final AspectModel aspectModel = aspectModelLoader.load( aspectModelPath.toFile() );

            if ( setNewVersion ) {
               applyNamespaceVersionChange( aspectModel, errors, metaModelStoragePath );
               return;
            }

            AspectSerializer.INSTANCE.write( aspectModel );
         } catch ( final Exception e ) {
            errors.add( String.format( "Error processing model: %s", model.getModel() ) );
         }
      } );
   }

   private void applyNamespaceVersionChange( final AspectModel aspectModel, final List<String> errors, final Path metaModelStoragePath ) {
      try {
         final AspectModelFile originalFile = aspectModel.files().getFirst();
         final AspectChangeManager changeManager = new AspectChangeManager( aspectModel );
         changeManager.applyChange( new CopyFileWithIncreasedNamespaceVersion( originalFile, IncreaseVersion.MAJOR ) );

         final List<AspectModelFile> newFiles = aspectModel.files().stream()
               .filter( file -> !file.namespaceUrn().getVersion().equals( originalFile.namespaceUrn().getVersion() ) ).toList();

         if ( newFiles.size() != 1 ) {
            return;
         }

         final AspectModelFile updatedFile = newFiles.getFirst();
         final URI sourceLocation = updatedFile.sourceLocation()
               .orElseThrow( () -> new IllegalStateException( "Source location missing" ) );

         if ( new File( sourceLocation ).exists() ) {
            errors.add( String.format( "A new version of the Aspect Model: %s with Version: %s already exists",
                  updatedFile.filename().orElse( "unknown" ), originalFile.namespaceUrn().getVersion() ) );
            return;
         }

         ModelUtils.createFile( updatedFile.namespaceUrn(),
               updatedFile.filename().orElseThrow( () -> new FileHandlingException( "Filename missing" ) ),
               metaModelStoragePath );

         AspectSerializer.INSTANCE.write( updatedFile );
      } catch ( final IOException e ) {
         throw new CreateFileException( "Cannot create file %s on workspace", e );
      }
   }
}
