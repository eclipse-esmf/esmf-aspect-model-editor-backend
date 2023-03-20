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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;

import io.openmanufacturing.ame.config.ApplicationSettings;
import io.openmanufacturing.ame.model.ValidationProcess;
import io.openmanufacturing.ame.model.migration.AspectModelFile;
import io.openmanufacturing.ame.model.migration.Namespace;
import io.openmanufacturing.ame.model.migration.Namespaces;
import io.openmanufacturing.ame.model.validation.ViolationReport;
import io.openmanufacturing.ame.repository.ModelResolverRepository;
import io.openmanufacturing.ame.repository.strategy.LocalFolderResolverStrategy;
import io.openmanufacturing.ame.repository.strategy.ModelResolverStrategy;
import io.openmanufacturing.ame.services.utils.ModelUtils;
import io.openmanufacturing.sds.aspectmodel.resolver.services.DataType;
import io.openmanufacturing.sds.aspectmodel.resolver.services.VersionedModel;
import io.openmanufacturing.sds.aspectmodel.urn.AspectModelUrn;
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
            storagePath.orElse( ApplicationSettings.getMetaModelStoragePath().toString() ) );
   }

   public String saveModel( final Optional<String> urn, final String aspectModel, final Optional<String> storagePath ) {
      final ModelResolverStrategy strategy = modelResolverRepository.getStrategy( LocalFolderResolverStrategy.class );

      return strategy.saveModel( urn, ModelUtils.getPrettyPrintedModel( aspectModel, ValidationProcess.MODELS ),
            storagePath.orElse( ApplicationSettings.getMetaModelStoragePath().toString() ) );
   }

   private void saveVersionedModel( final VersionedModel versionedModel, final AspectModelUrn aspectModelUrn,
         final String path ) {
      final String prettyPrintedVersionedModel = ModelUtils.getPrettyPrintedVersionedModel( versionedModel,
            aspectModelUrn.getUrn() );
      saveModel( Optional.of( aspectModelUrn.getUrn().toString() ), prettyPrintedVersionedModel, Optional.of( path ) );
   }

   public void deleteModel( final String namespace ) {
      final ModelResolverStrategy strategy = modelResolverRepository.getStrategy( LocalFolderResolverStrategy.class );

      strategy.deleteModel( namespace, ApplicationSettings.getMetaModelStoragePath().toString() );
   }

   public Map<String, List<String>> getAllNamespaces( final boolean shouldRefresh,
         final ValidationProcess validationProcess ) {
      final ModelResolverStrategy strategy = modelResolverRepository.getStrategy( LocalFolderResolverStrategy.class );
      return strategy.getAllNamespaces( shouldRefresh, validationProcess.getPath().toString() );
   }

   public ViolationReport validateModel( final String aspectModel, final ValidationProcess validationProcess ) {
      return ModelUtils.validateModel( aspectModel, aspectModelValidator, validationProcess );
   }

   public String migrateModel( final String aspectModel, final ValidationProcess validationProcess ) {
      return ModelUtils.migrateModel( aspectModel, validationProcess );
   }

   public Namespaces migrateWorkspace( final Path storagePath ) {
      final ModelResolverStrategy strategy = modelResolverRepository.getStrategy( LocalFolderResolverStrategy.class );
      final File storageDirectory = storagePath.toFile();

      final String[] extensions = { "ttl" };

      final List<Namespace> namespaces = new ArrayList<>();

      FileUtils.listFiles( storageDirectory, extensions, true ).stream().map( File::getAbsoluteFile )
               .forEach( inputFile -> {
                  if ( !inputFile.getName().equals( "latest.ttl" ) ) {
                     final Try<VersionedModel> versionedModels = updateModelVersion( inputFile );
                     final AspectModelUrn aspectModelUrn = strategy.convertFileToUrn( inputFile );
                     final Namespace namespace = resolveNamespace( namespaces, aspectModelUrn );
                     namespaceFileInfo( namespace, versionedModels, aspectModelUrn, storagePath.toString() );
                  }
               } );

      return new Namespaces( namespaces );
   }

   private Try<VersionedModel> updateModelVersion( final File inputFile ) {
      return ModelUtils.loadModelFromFile( inputFile ).flatMap( new MigratorService()::updateMetaModelVersion );
   }

   private Namespace resolveNamespace( final List<Namespace> namespaces, final AspectModelUrn aspectModelUrn ) {
      final String versionedNamespace = aspectModelUrn.getNamespace() + ":" + aspectModelUrn.getVersion();
      final Optional<Namespace> first = namespaces.stream().filter(
            namespace -> namespace.versionedNamespace.equals( versionedNamespace ) ).findFirst();

      return first.orElseGet( () -> {
         final Namespace namespace = new Namespace( versionedNamespace );
         namespaces.add( namespace );
         return namespace;
      } );
   }

   private void namespaceFileInfo( final Namespace namespace, final Try<VersionedModel> model,
         final AspectModelUrn aspectModelUrn, final String storagePath ) {

      boolean modelIsSuccess = false;

      if ( model.isSuccess() ) {
         saveVersionedModel( model.get(), aspectModelUrn, storagePath );
         modelIsSuccess = !getModel(
               namespace.versionedNamespace + ':' + aspectModelUrn.getName() + ModelUtils.TTL_EXTENSION,
               Optional.of( storagePath ) ).contains( "undefined:" );
      }

      final AspectModelFile aspectModelFile = new AspectModelFile( aspectModelUrn.getName() + ModelUtils.TTL_EXTENSION,
            modelIsSuccess );

      namespace.addAspectModelFile( aspectModelFile );
   }

   public String getFormattedModel( final String aspectModel ) {
      return ModelUtils.getPrettyPrintedModel( aspectModel, ValidationProcess.MODELS );
   }
}
