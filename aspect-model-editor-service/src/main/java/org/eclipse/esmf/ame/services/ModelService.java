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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.eclipse.esmf.ame.exceptions.CreateFileException;
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
import org.eclipse.esmf.aspectmodel.UnsupportedVersionException;
import org.eclipse.esmf.aspectmodel.edit.AspectChangeManager;
import org.eclipse.esmf.aspectmodel.edit.change.CopyFileWithIncreasedNamespaceVersion;
import org.eclipse.esmf.aspectmodel.edit.change.IncreaseVersion;
import org.eclipse.esmf.aspectmodel.loader.AspectModelLoader;
import org.eclipse.esmf.aspectmodel.resolver.exceptions.ModelResolutionException;
import org.eclipse.esmf.aspectmodel.resolver.fs.StructuredModelsRoot;
import org.eclipse.esmf.aspectmodel.serializer.AspectSerializer;
import org.eclipse.esmf.aspectmodel.shacl.violation.Violation;
import org.eclipse.esmf.aspectmodel.urn.AspectModelUrn;
import org.eclipse.esmf.aspectmodel.validation.services.AspectModelValidator;
import org.eclipse.esmf.metamodel.AspectModel;

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

   private static final String sammStructureInfo = "Please check whether the SAMM structure has been followed in the workspace: "
         + "Namespace/Version/Aspect model.";

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

         return aspectModel.files().stream()
               .filter( a -> a.elements().stream().anyMatch( e -> e.urn().equals( aspectModelUrn ) ) ).findFirst()
               .map( AspectSerializer.INSTANCE::aspectModelFileToString )
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
         final File newFile = ModelUtils.createFile( aspectModelUrn, fileName, storagePath );

         final Supplier<AspectModel> aspectModelSupplier = ModelUtils.getAspectModelSupplier( turtleData, newFile, aspectModelLoader );
         final List<Violation> violations = aspectModelValidator.validateModel( aspectModelSupplier );

         if ( violations.stream().anyMatch( ValidationUtils.isInvalidSyntaxViolation() ) ) {
            throw new FileReadException( "Aspect Model syntax is not valid" );
         }

         AspectSerializer.INSTANCE.write( aspectModelSupplier.get().files().getFirst() );
      } catch ( final IOException e ) {
         throw new CreateFileException( String.format( "Cannot create file %s on workspace", aspectModelUrn ), e );
      }
   }

   public void deleteModel( final AspectModelUrn aspectModelUrn ) {
      ModelUtils.deleteEmptyFiles( new StructuredModelsRoot( modelPath ).determineAspectModelFile( aspectModelUrn ) );
   }

   public ViolationReport validateModel( final String turtleData ) {
      final ByteArrayInputStream inputStream = ModelUtils.createInputStream( turtleData );
      final Supplier<AspectModel> aspectModelSupplier = () -> aspectModelLoader.load( inputStream );
      final List<Violation> violations = aspectModelValidator.validateModel( aspectModelSupplier );
      final List<ViolationError> violationErrors = ValidationUtils.violationErrors( violations );

      return new ViolationReport( violationErrors );
   }

   public String migrateModel( final String turtleData ) {
      final ByteArrayInputStream inputStream = ModelUtils.createInputStream( turtleData );
      final AspectModel aspectModel = aspectModelLoader.load( inputStream );

      return aspectModel.files().stream().filter( a -> a.sourceLocation().isEmpty() ).findFirst()
            .map( AspectSerializer.INSTANCE::aspectModelFileToString )
            .orElseThrow( () -> new InvalidAspectModelException( "No aspect model found to migrate" ) );
   }

   public String getFormattedModel( final String turtleData ) {
      final ByteArrayInputStream inputStream = ModelUtils.createInputStream( turtleData );
      final AspectModel aspectModel = aspectModelLoader.load( inputStream );

      return aspectModel.files().stream().filter( a -> a.sourceLocation().isEmpty() ).findFirst()
            .map( AspectSerializer.INSTANCE::aspectModelFileToString )
            .orElseThrow( () -> new InvalidAspectModelException( "No aspect model found to formate" ) );
   }

   public Map<String, List<Version>> getAllNamespaces() {
      try {
         return new ModelGroupingUtils( aspectModelLoader ).groupModelsByNamespaceAndVersion( aspectModelLoader.listContents() );
      } catch ( final UnsupportedVersionException e ) {
         LOG.error( "{} There is a loose .ttl file somewhere — remove it along with any other non-standardized files.",
               sammStructureInfo, e );
         throw new FileReadException( sammStructureInfo + " Remove all non-standardized files." );
      }
   }

   public MigrationResult migrateWorkspace( final boolean setNewVersion ) {
      final List<String> errors = new ArrayList<>();

      try {
         getAllNamespaces().forEach(
               ( namespace, versions ) -> versions.forEach( version -> processVersion( namespace, version, setNewVersion, errors ) ) );
         return new MigrationResult( true, errors );
      } catch ( final Exception e ) {
         errors.add( e.getMessage() );
         return new MigrationResult( false, errors );
      }
   }

   private void processVersion( final String namespace, final Version version, final boolean setNewVersion, final List<String> errors ) {
      version.getModels().forEach( model -> {
         try {
            final Path aspectModelPath = ModelUtils.constructModelPath( modelPath, namespace, version.getVersion(), model.getModel() );
            final AspectModel aspectModel = aspectModelLoader.load( aspectModelPath.toFile() );

            if ( setNewVersion ) {
               applyNamespaceVersionChange( aspectModel );
            }

            saveAspectModelFiles( aspectModel, setNewVersion );
         } catch ( final Exception e ) {
            errors.add( String.format( "Error processing model: %s", model.getModel() ) );
         }
      } );
   }

   private void applyNamespaceVersionChange( final AspectModel aspectModel ) {
      final AspectChangeManager aspectChangeManager = new AspectChangeManager( aspectModel );
      final CopyFileWithIncreasedNamespaceVersion changes = new CopyFileWithIncreasedNamespaceVersion( aspectModel.files().getFirst(),
            IncreaseVersion.MAJOR );
      aspectChangeManager.applyChange( changes );
   }

   private void saveAspectModelFiles( final AspectModel aspectModel, final boolean setNewVersion ) {
      aspectModel.files().forEach( aspectModelFile -> aspectModelFile.sourceLocation().ifPresent( sourceLocation -> {
         final File file = new File( sourceLocation );
         try {
            if ( !setNewVersion ) {
               AspectSerializer.INSTANCE.write( aspectModelFile );
               return;
            }

            if ( file.exists() ) {
               return;
            }

            final File parent = file.getParentFile();
            if ( !parent.exists() && !parent.mkdirs() ) {
               throw new IOException( "Failed to create directories for: " + parent );
            }

            AspectSerializer.INSTANCE.write( aspectModelFile );
         } catch ( final IOException e ) {
            throw new RuntimeException( "Error saving aspect model file: " + sourceLocation, e );
         }
      } ) );
   }
}
