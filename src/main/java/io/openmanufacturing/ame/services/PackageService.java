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
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import io.openmanufacturing.ame.config.ApplicationSettings;
import io.openmanufacturing.ame.exceptions.FileNotFoundException;
import io.openmanufacturing.ame.repository.ModelResolverRepository;
import io.openmanufacturing.ame.repository.model.LocalPackageInfo;
import io.openmanufacturing.ame.repository.strategy.LocalFolderResolverStrategy;
import io.openmanufacturing.ame.repository.strategy.ModelResolverStrategy;
import io.openmanufacturing.ame.resolver.file.FileSystemStrategy;
import io.openmanufacturing.ame.services.model.FileInformation;
import io.openmanufacturing.ame.services.model.MissingFileInfo;
import io.openmanufacturing.ame.services.model.ProcessedExportedPackage;
import io.openmanufacturing.ame.services.model.ProcessedImportedPackage;
import io.openmanufacturing.ame.services.utils.ModelUtils;
import io.openmanufacturing.ame.services.utils.UnzipUtils;
import io.openmanufacturing.ame.services.utils.ZipUtils;
import io.openmanufacturing.sds.aspectmodel.resolver.services.DataType;
import io.openmanufacturing.sds.aspectmodel.urn.AspectModelUrn;
import io.openmanufacturing.sds.aspectmodel.validation.report.ValidationError;
import io.openmanufacturing.sds.aspectmodel.validation.report.ValidationReport;
import io.openmanufacturing.sds.aspectmodel.validation.services.AspectModelValidator;

@Service
public class PackageService {
   private static final Logger LOG = LoggerFactory.getLogger( PackageService.class );

   private final AspectModelValidator aspectModelValidator;
   private final ModelResolverRepository modelResolverRepository;

   public PackageService( final AspectModelValidator aspectModelValidator,
         final ModelResolverRepository modelResolverRepository ) {
      this.aspectModelValidator = aspectModelValidator;
      this.modelResolverRepository = modelResolverRepository;

      DataType.setupTypeMapping();
   }

   public ProcessedImportedPackage validateImportAspectModelPackage( final MultipartFile zipFile,
         final String storagePath ) {
      try {
         // When validating the aspect modes to be exported, the folder is initially deleted.
         FileUtils.deleteDirectory( new File( storagePath ) );

         final Path packagePath = Path.of( storagePath );
         UnzipUtils.unzipPackageFile( zipFile, packagePath );

         final ModelResolverStrategy strategy = modelResolverRepository.getStrategy(
               LocalFolderResolverStrategy.class );

         final LocalPackageInfo localPackageInfo = strategy.getLocalPackageInformation( storagePath );

         final ProcessedImportedPackage processedImportedPackage = new ProcessedImportedPackage(
               localPackageInfo.getInValidFiles() );

         localPackageInfo.getValidFiles().forEach( localPackageInformation -> {
                  final Boolean modelExist = strategy.checkModelExist( localPackageInformation.getAspectModelFile(),
                        ApplicationSettings.getMetaModelStoragePath() );

                  final FileInformation fileInformation = new FileInformation( localPackageInformation.getAspectModelFile(),
                        ModelUtils.validateModel( localPackageInformation.getAspectModel(), storagePath,
                              aspectModelValidator ),
                        modelExist );

                  processedImportedPackage.addFileInformation( fileInformation );
               }
         );

         return processedImportedPackage;
      } catch ( final IOException e ) {
         LOG.error( "Cannot delete imported package folder." );
         throw new FileNotFoundException(
               String.format( "Package folder: %s was not deleted successfully.", storagePath ), e );
      }
   }

   public List<String> importAspectModelPackage( final List<String> aspectModelFiles, final String storagePath ) {
      try {
         final ModelResolverStrategy strategy = modelResolverRepository.getStrategy(
               LocalFolderResolverStrategy.class );

         final List<String> fileLocations = aspectModelFiles.stream()
                                                            .map( aspectModelFile -> strategy.saveModel(
                                                                  Optional.empty(),
                                                                  strategy.getModel( aspectModelFile, storagePath ),
                                                                  ApplicationSettings.getMetaModelStoragePath() )
                                                            ).collect( Collectors.toList() );

         FileUtils.deleteDirectory( new File( storagePath ) );

         return fileLocations;
      } catch ( final IOException e ) {
         LOG.error( "Cannot delete imported package folder." );
         throw new FileNotFoundException(
               String.format( "Package folder: %s was not deleted successfully.", storagePath ), e );
      }
   }

   public ProcessedExportedPackage validateAspectModels( final List<String> aspectModelFiles,
         final String storagePath ) {
      try {
         // When validating the aspect modes to be exported, the folder is initially deleted.
         FileUtils.deleteDirectory( new File( storagePath ) );

         final ModelResolverStrategy strategy = modelResolverRepository.getStrategy(
               LocalFolderResolverStrategy.class );

         final ProcessedExportedPackage processedExportedPackage = new ProcessedExportedPackage();

         // Save all aspect models to export storage path
         aspectModelFiles.forEach( aspectModelFileName -> {
            final String aspectModel = strategy.getModel( aspectModelFileName,
                  ApplicationSettings.getMetaModelStoragePath() );
            strategy.saveModel( Optional.empty(), aspectModel, storagePath );
         } );

         // Validate all aspect models from export storage path and create export package model
         aspectModelFiles.forEach( aspectModelFileName -> {
            final String aspectModel = strategy.getModel( aspectModelFileName, storagePath );
            final ValidationReport validationReport = ModelUtils.validateModel( aspectModel, storagePath,
                  aspectModelValidator );

            getMissingAspectModelFiles( validationReport ).forEach( processedExportedPackage::addMissingFiles );
            final FileInformation fileInformation = new FileInformation( aspectModelFileName, validationReport );

            processedExportedPackage.addFileInformation( fileInformation );
         } );

         return processedExportedPackage;
      } catch ( final IOException e ) {
         LOG.error( "Cannot delete exported package folder." );
         throw new FileNotFoundException( String.format( "Unable to delete folder: %s", storagePath ), e );
      }
   }

   public byte[] exportAspectModelPackage( final String zipFileName, final String storagePath ) {
      try {
         final byte[] zipFile = ZipUtils.createZipFile( zipFileName, storagePath );
         FileUtils.deleteDirectory( new File( storagePath ) );
         return zipFile;
      } catch ( final IOException e ) {
         LOG.error( "Cannot create exported package file." );
         throw new FileNotFoundException( String.format( "Error while creating the package file: %s", zipFileName ),
               e );
      }
   }

   private List<MissingFileInfo> getMissingAspectModelFiles( final ValidationReport validationReport ) {
      final List<ValidationError> validationErrors = validationReport.getValidationErrors().stream().filter(
            validationError -> validationError instanceof ValidationError.Semantic ).collect( Collectors.toList() );

      if ( validationErrors.isEmpty() ) {
         return List.of();
      }

      return validationErrors.stream()
                             .filter( validationError -> ModelUtils.URN_PATTERN.matcher(
                                   ((ValidationError.Semantic) validationError).getValue() ).matches() )
                             .map( validationError -> {
                                final String valueUrn = ((ValidationError.Semantic) validationError).getValue();
                                final String focusNodeUrn = ((ValidationError.Semantic) validationError).getFocusNode();

                                final FileSystemStrategy fileSystemStrategy = new FileSystemStrategy(
                                      Path.of( ApplicationSettings.getMetaModelStoragePath() ) );

                                final String analysedFile = fileSystemStrategy.getAspectModelFile(
                                      AspectModelUrn.fromUrn( focusNodeUrn ) );
                                final String missingFile = fileSystemStrategy.getAspectModelFile(
                                      AspectModelUrn.fromUrn( valueUrn ) );

                                return new MissingFileInfo( analysedFile, missingFile,
                                      String.format(
                                            "Shared model: %s defined in the Aspect Model file: %s could not be found. Please export it as well to prevent validation problems later.",
                                            missingFile, analysedFile )
                                );
                             } )
                             .collect( Collectors.toList() );
   }
}
