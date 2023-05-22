/*
 * Copyright (c) 2023 Robert Bosch Manufacturing Solutions GmbH
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.apache.jena.rdf.model.StmtIterator;
import org.eclipse.esmf.ame.model.ProcessPath;
import org.eclipse.esmf.ame.model.migration.FileInformation;
import org.eclipse.esmf.ame.model.migration.Namespace;
import org.eclipse.esmf.ame.model.migration.Namespaces;
import org.eclipse.esmf.ame.model.validation.ViolationReport;
import org.eclipse.esmf.ame.repository.ModelResolverRepository;
import org.eclipse.esmf.ame.repository.strategy.LocalFolderResolverStrategy;
import org.eclipse.esmf.ame.repository.strategy.ModelResolverStrategy;
import org.eclipse.esmf.ame.resolver.strategy.FileSystemStrategy;
import org.eclipse.esmf.ame.services.utils.ModelUtils;
import org.eclipse.esmf.aspectmodel.resolver.services.DataType;
import org.eclipse.esmf.aspectmodel.resolver.services.VersionedModel;
import org.eclipse.esmf.aspectmodel.urn.AspectModelUrn;
import org.eclipse.esmf.aspectmodel.validation.services.AspectModelValidator;
import org.eclipse.esmf.aspectmodel.versionupdate.MigratorService;
import org.springframework.stereotype.Service;

import io.vavr.NotImplementedError;
import io.vavr.Tuple2;
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

   public String getModel( final String namespace, final String filename ) {
      final ModelResolverStrategy strategy = modelResolverRepository.getStrategy( LocalFolderResolverStrategy.class );

      return strategy.getModelAsString( namespace, filename );
   }

   public String saveModel( final Optional<String> namespace, final Optional<String> fileName,
         final String aspectModel ) {
      final ModelResolverStrategy strategy = modelResolverRepository.getStrategy( LocalFolderResolverStrategy.class );
      final String prettyPrintedModel = ModelUtils.getPrettyPrintedModel( aspectModel );

      return strategy.saveModel( namespace, fileName, prettyPrintedModel );
   }

   private void saveVersionedModel( final VersionedModel versionedModel, final String namespace,
         final String fileName ) {

      final Optional<StmtIterator> esmfStatements = FileSystemStrategy.getEsmfStatements( versionedModel.getModel() );

      final String uri = esmfStatements.stream().findFirst()
                                       .orElseThrow(
                                             () -> new NotImplementedError( "AspectModelUrn cannot be found." ) )
                                       .next()
                                       .getSubject().getURI();

      final String prettyPrintedVersionedModel = ModelUtils.getPrettyPrintedVersionedModel( versionedModel,
            AspectModelUrn.fromUrn( uri ).getUrn() );

      saveModel( Optional.of( namespace ), Optional.of( fileName ), prettyPrintedVersionedModel );
   }

   public void deleteModel( final String namespace, final String fileName ) {
      final ModelResolverStrategy strategy = modelResolverRepository.getStrategy( LocalFolderResolverStrategy.class );

      strategy.deleteModel( namespace, fileName );
   }

   public Map<String, List<String>> getAllNamespaces( final boolean shouldRefresh ) {
      final ModelResolverStrategy strategy = modelResolverRepository.getStrategy( LocalFolderResolverStrategy.class );
      return strategy.getAllNamespaces( shouldRefresh );
   }

   public ViolationReport validateModel( final String aspectModel ) {
      return ModelUtils.validateModel( aspectModel, aspectModelValidator );
   }

   public String migrateModel( final String aspectModel ) {
      return ModelUtils.migrateModel( aspectModel );
   }

   public Namespaces migrateWorkspace() {
      final ModelResolverStrategy strategy = modelResolverRepository.getStrategy( LocalFolderResolverStrategy.class );
      final File storageDirectory = ProcessPath.MODELS.getPath().toFile();

      final String[] extensions = { "ttl" };

      final List<Namespace> namespaces = new ArrayList<>();

      FileUtils.listFiles( storageDirectory, extensions, true ).stream().map( File::getAbsoluteFile )
               .forEach( inputFile -> {
                  if ( !inputFile.getName().equals( "latest.ttl" ) ) {
                     final Try<VersionedModel> versionedModels = updateModelVersion( inputFile );
                     final Tuple2<String, String> fileInfo = strategy.convertFileToTuple( inputFile );
                     final Namespace namespace = resolveNamespace( namespaces, fileInfo._2 );
                     namespaceFileInfo( namespace, versionedModels, fileInfo._1, fileInfo._2 );
                  }
               } );

      return new Namespaces( namespaces );
   }

   private Try<VersionedModel> updateModelVersion( final File inputFile ) {
      return ModelUtils.loadModelFromFile( inputFile ).flatMap( new MigratorService()::updateMetaModelVersion );
   }

   private Namespace resolveNamespace( final List<Namespace> namespaces, final String versionedNamespace ) {
      final Optional<Namespace> first = namespaces.stream().filter(
            namespace -> namespace.versionedNamespace.equals( versionedNamespace ) ).findFirst();

      return first.orElseGet( () -> {
         final Namespace namespace = new Namespace( versionedNamespace );
         namespaces.add( namespace );
         return namespace;
      } );
   }

   private void namespaceFileInfo( final Namespace namespace, final Try<VersionedModel> model, final String fileName,
         final String versionedNamespace ) {

      boolean modelIsSuccess = false;

      if ( model.isSuccess() ) {
         saveVersionedModel( model.get(), versionedNamespace, fileName + ModelUtils.TTL_EXTENSION );
         modelIsSuccess = !getModel( namespace.versionedNamespace, fileName + ModelUtils.TTL_EXTENSION ).contains(
               "undefined:" );
      }

      final FileInformation aspectModelFile = new FileInformation( fileName + ModelUtils.TTL_EXTENSION,
            modelIsSuccess );

      namespace.addAspectModelFile( aspectModelFile );
   }

   public String getFormattedModel( final String aspectModel ) {
      return ModelUtils.getPrettyPrintedModel( aspectModel );
   }
}
