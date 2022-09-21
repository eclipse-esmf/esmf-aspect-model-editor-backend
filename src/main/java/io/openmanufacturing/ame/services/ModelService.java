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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;

import io.openmanufacturing.ame.config.ApplicationSettings;
import io.openmanufacturing.ame.exceptions.FileNotFoundException;
import io.openmanufacturing.ame.repository.ModelResolverRepository;
import io.openmanufacturing.ame.repository.strategy.LocalFolderResolverStrategy;
import io.openmanufacturing.ame.repository.strategy.ModelResolverStrategy;
import io.openmanufacturing.ame.services.model.migration.AspectModelFile;
import io.openmanufacturing.ame.services.model.migration.Namespace;
import io.openmanufacturing.ame.services.model.migration.Namespaces;
import io.openmanufacturing.ame.services.utils.ModelUtils;
import io.openmanufacturing.sds.aspectmodel.resolver.services.DataType;
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

      return strategy.getModel( namespace, storagePath.orElse( ApplicationSettings.getMetaModelStoragePath() ) );
   }

   public String saveModel( final Optional<String> urn, final String turtleData, final Optional<String> storagePath ) {
      final ModelResolverStrategy strategy = modelResolverRepository.getStrategy( LocalFolderResolverStrategy.class );

      return strategy.saveModel( urn, turtleData, storagePath.orElse( ApplicationSettings.getMetaModelStoragePath() ) );
   }

   private void saveVersionedModel( final VersionedModel versionedModel, final AspectModelUrn aspectModelUrn,
         final String path ) {
      final String prettyPrintedVersionedModel = ModelUtils.getPrettyPrintedVersionedModel( versionedModel,
            aspectModelUrn.getUrn() );
      saveModel( Optional.of( aspectModelUrn.getUrn().toString() ), prettyPrintedVersionedModel, Optional.of( path ) );
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
         final ModelResolverStrategy strategy = modelResolverRepository.getStrategy(
               LocalFolderResolverStrategy.class );
         final File storageDirectory = new File( storagePath );
         final File destDirectory = new File( destPath );

         FileUtils.deleteDirectory( destDirectory );
         FileUtils.copyDirectory( storageDirectory, destDirectory );

         final String[] extensions = { "ttl" };

         final List<Namespace> namespaces = new ArrayList<>();

         FileUtils.listFiles( destDirectory, extensions, true )
                  .stream()
                  .map( File::getAbsoluteFile )
                  .map( inputFile -> {
                     final Try<VersionedModel> versionedModels = updateModelVersion(
                           inputFile );
                     final AspectModelUrn aspectModelUrn = strategy.convertFileToUrn(
                           inputFile );
                     final Namespace namespace = resolveNamespace( namespaces,
                           aspectModelUrn );
                     return namespaceFileInfo( namespace, inputFile, versionedModels,
                           destPath );
                  } )
                  .collect( Collectors.toList() );

         return new Namespaces( namespaces );
      } catch ( final IOException e ) {
         throw new FileNotFoundException( String.format( "Cannot copy directory %s to %s", storagePath, destPath ) );
      }
   }

   private Try<VersionedModel> updateModelVersion( final File inputFile ) {
      return ModelUtils.loadButNotResolveModel( inputFile ).flatMap( new MigratorService()::updateMetaModelVersion );
   }

   private Namespace resolveNamespace( final List<Namespace> namespaces, final AspectModelUrn aspectModelUrn ) {
      final String versionedNamespace = aspectModelUrn.getNamespace() + ":" + aspectModelUrn.getVersion();
      final Optional<Namespace> first = namespaces.stream()
                                                  .filter( namespace -> namespace.versionedNamespace.equals(
                                                        versionedNamespace ) )
                                                  .findFirst();

      return first.orElseGet( () -> {
         final Namespace namespace = new Namespace( versionedNamespace );
         namespaces.add( namespace );
         return namespace;
      } );
   }

   private Namespace namespaceFileInfo( final Namespace namespace, final File inputFile,
         final Try<VersionedModel> model, final String destPath ) {
      final ModelResolverStrategy strategy = modelResolverRepository.getStrategy( LocalFolderResolverStrategy.class );

      final AspectModelUrn aspectModelUrn = strategy.convertFileToUrn( inputFile );

      if ( model.isSuccess() ) {
         saveVersionedModel( model.get(), aspectModelUrn, destPath );
      }

      final AspectModelFile aspectModelFile = new AspectModelFile( aspectModelUrn.getName() + ModelUtils.TTL,
            model.isSuccess() );

      namespace.addAspectModelFile( aspectModelFile );
      return namespace;
   }
}
