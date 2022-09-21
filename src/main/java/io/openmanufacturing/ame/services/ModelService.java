/*
 * Copyright (c) 2022 Robert Bosch Manufacturing Solutions GmbH
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

package io.openmanufacturing.ame.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;

import io.openmanufacturing.ame.config.ApplicationSettings;
import io.openmanufacturing.ame.exceptions.FileNotFoundException;
import io.openmanufacturing.ame.exceptions.InvalidAspectModelException;
import io.openmanufacturing.ame.repository.ModelResolverRepository;
import io.openmanufacturing.ame.repository.strategy.LocalFolderResolverStrategy;
import io.openmanufacturing.ame.repository.strategy.ModelResolverStrategy;
import io.openmanufacturing.ame.services.model.migration.AspectModelFile;
import io.openmanufacturing.ame.services.model.migration.Namespace;
import io.openmanufacturing.ame.services.model.migration.Namespaces;
import io.openmanufacturing.ame.services.utils.ModelUtils;
import io.openmanufacturing.sds.aspectmodel.resolver.services.DataType;
import io.openmanufacturing.sds.aspectmodel.resolver.services.SdsAspectMetaModelResourceResolver;
import io.openmanufacturing.sds.aspectmodel.resolver.services.TurtleLoader;
import io.openmanufacturing.sds.aspectmodel.resolver.services.VersionedModel;
import io.openmanufacturing.sds.aspectmodel.urn.AspectModelUrn;
import io.openmanufacturing.sds.aspectmodel.validation.report.ValidationReport;
import io.openmanufacturing.sds.aspectmodel.validation.services.AspectModelValidator;
import io.openmanufacturing.sds.aspectmodel.versionupdate.MigratorService;
import io.vavr.control.Try;

@Service
public class ModelService {
   private final AspectModelValidator aspectModelValidator;
   private final ModelResolverRepository modelResolverRepository;

   public ModelService( final AspectModelValidator aspectModelValidator,
         final ModelResolverRepository modelResolverRepository ) {
      this.aspectModelValidator = aspectModelValidator;
      this.modelResolverRepository = modelResolverRepository;

      DataType.setupTypeMapping();
   }

   public String getModel( final String namespace, final Optional<String> storagePath ) {
      final ModelResolverStrategy strategy = modelResolverRepository.getStrategy( LocalFolderResolverStrategy.class );

      return strategy.getModelAsString( namespace,
            storagePath.orElse( ApplicationSettings.getMetaModelStoragePath() ) );
   }

   public String saveModel( final Optional<String> urn, final String turtleData, final Optional<String> storagePath ) {
      final ModelResolverStrategy strategy = modelResolverRepository.getStrategy( LocalFolderResolverStrategy.class );

      return strategy.saveModel( urn, turtleData, storagePath.orElse( ApplicationSettings.getMetaModelStoragePath() ) );
   }

   public void deleteModel( final String namespace ) {
      final ModelResolverStrategy strategy = modelResolverRepository.getStrategy( LocalFolderResolverStrategy.class );

      strategy.deleteModel( namespace, ApplicationSettings.getMetaModelStoragePath() );
   }

   public Map<String, List<String>> getAllNamespaces( final boolean shouldRefresh,
         final Optional<String> storagePath ) {
      final ModelResolverStrategy strategy = modelResolverRepository.getStrategy( LocalFolderResolverStrategy.class );
      return strategy.getAllNamespaces( shouldRefresh,
            storagePath.orElse( ApplicationSettings.getMetaModelStoragePath() ) );
   }

   public ValidationReport validateModel( final String aspectModel, final String storagePath ) {
      return ModelUtils.validateModel( aspectModel, storagePath, aspectModelValidator );
   }

   public String migrateModel( final String aspectModel, final String storagePath ) {
      return ModelUtils.migrateModel( aspectModel, storagePath );
   }

   public Namespaces migrateWorkspace( final String storagePath, final String destPath ) {
      try {
         final File storageDirectory = new File( storagePath );
         final File destDirectory = new File( destPath );

         FileUtils.deleteDirectory( destDirectory );
         FileUtils.copyDirectory( storageDirectory, destDirectory );

         final String[] extensions = { "ttl" };

         final Namespaces namespaces = new Namespaces();

         FileUtils.listFiles( destDirectory, extensions, true )
                  .stream()
                  .forEach( file -> {
                     final File inputFile = file.getAbsoluteFile();
                     final AspectModelUrn aspectModelUrn = fileToUrn( inputFile );

                     final Namespace namespace = new Namespace(
                           aspectModelUrn.getNamespace() + ":" + aspectModelUrn.getVersion() );

                     final Try<VersionedModel> migratedFile = loadButNotResolveModel( inputFile )
                           .flatMap( new MigratorService()::updateMetaModelVersion );

                     final AspectModelFile aspectModelFile = new AspectModelFile(
                           aspectModelUrn.getName() + LocalFolderResolverStrategy.TTL );

                     if ( migratedFile.isSuccess() ) {
                        aspectModelFile.setSuccess( migratedFile.isSuccess() );
                        final String prettyPrintedVersionedModel = ModelUtils.getPrettyPrintedVersionedModel(
                              migratedFile.get(), aspectModelUrn.getUrn() );

                        saveModel( Optional.of( aspectModelUrn.getUrn().toString() ), prettyPrintedVersionedModel,
                              Optional.of( destPath ) );
                     } else {
                        aspectModelFile.setSuccess( !migratedFile.isFailure() );
                     }

                     namespace.addAspectModelFile( aspectModelFile );
                     namespaces.addNamespace( namespace );
                  } );

         return namespaces;
      } catch ( final IOException e ) {
         throw new FileNotFoundException( String.format( "Cannot copy directory %s to %s", storagePath, destPath ) );
      }
   }

   private Try<VersionedModel> loadButNotResolveModel( final File inputFile ) {
      try ( final InputStream inputStream = new FileInputStream( inputFile ) ) {
         final SdsAspectMetaModelResourceResolver metaModelResourceResolver = new SdsAspectMetaModelResourceResolver();
         return TurtleLoader.loadTurtle( inputStream ).flatMap( model ->
               metaModelResourceResolver.getBammVersion( model ).flatMap( metaModelVersion ->
                     metaModelResourceResolver.mergeMetaModelIntoRawModel( model, metaModelVersion ) ) );
      } catch ( final IOException exception ) {
         return Try.failure( exception );
      }
   }

   private AspectModelUrn fileToUrn( final File inputFile ) {
      final File versionDirectory = inputFile.getParentFile();
      final String version = versionDirectory.getName();
      final File namespaceDirectory = versionDirectory.getParentFile();
      final String namespace = namespaceDirectory.getName();
      final String aspectName = FilenameUtils.removeExtension( inputFile.getName() );
      final String urn = String.format( "urn:bamm:%s:%s#%s", namespace, version, aspectName );
      return new SdsAspectMetaModelResourceResolver().getAspectModelUrn( urn ).getOrElse( () -> {
         throw new InvalidAspectModelException(
               String.format( "The URN constructed from the input file path is invalid: %s", urn ) );
      } );
   }
}
