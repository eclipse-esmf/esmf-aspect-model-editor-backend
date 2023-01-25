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
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import io.openmanufacturing.ame.config.ApplicationSettings;
import io.openmanufacturing.ame.exceptions.FileNotFoundException;
import io.openmanufacturing.ame.model.packaging.MissingFile;
import io.openmanufacturing.ame.model.packaging.ProcessPackage;
import io.openmanufacturing.ame.model.packaging.ValidFile;
import io.openmanufacturing.ame.model.repository.LocalPackageInfo;
import io.openmanufacturing.ame.model.validation.ViolationError;
import io.openmanufacturing.ame.model.validation.ViolationReport;
import io.openmanufacturing.ame.repository.ModelResolverRepository;
import io.openmanufacturing.ame.repository.strategy.LocalFolderResolverStrategy;
import io.openmanufacturing.ame.repository.strategy.ModelResolverStrategy;
import io.openmanufacturing.ame.repository.strategy.utils.LocalFolderResolverUtils;
import io.openmanufacturing.ame.resolver.file.FileSystemStrategy;
import io.openmanufacturing.ame.services.utils.ModelUtils;
import io.openmanufacturing.ame.services.utils.UnzipUtils;
import io.openmanufacturing.ame.services.utils.ZipUtils;
import io.openmanufacturing.sds.aspectmodel.resolver.services.DataType;
import io.openmanufacturing.sds.aspectmodel.shacl.violation.ProcessingViolation;
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

   public ProcessPackage validateAspectModels( final List<String> aspectModelFiles,
         final String storagePath ) {
      try {
         // When validating the aspect modes to be exported, the folder is initially deleted.
         FileUtils.deleteDirectory( new File( storagePath ) );

         final ModelResolverStrategy strategy = modelResolverRepository.getStrategy(
               LocalFolderResolverStrategy.class );

         final ProcessPackage processPackage = new ProcessPackage();

         // Save all aspect models to export storage path
         aspectModelFiles.forEach( aspectModelFileName -> copyFileToDirectory( aspectModelFileName,
               ApplicationSettings.getMetaModelStoragePath(), storagePath ) );

         // Validate all aspect models from export storage path and create export package model
         aspectModelFiles.forEach( aspectModelFileName -> {
            final String aspectModel = strategy.getModelAsString( aspectModelFileName, storagePath );

            final ViolationReport violationReport = new ViolationReport();

            ModelUtils.validateModel( aspectModel, storagePath, aspectModelValidator, violationReport );

            getMissingAspectModelFiles( violationReport ).forEach( processPackage::addMissingFiles );
            final ValidFile validFile = new ValidFile( aspectModelFileName, violationReport );

            processPackage.addValidFiles( validFile );
         } );

         return processPackage;
      } catch ( final IOException e ) {
         LOG.error( "Cannot delete exported package folder." );
         throw new FileNotFoundException( String.format( "Unable to delete folder: %s", storagePath ), e );
      }
   }

   public ProcessPackage validateImportAspectModelPackage( final MultipartFile zipFile,
         final String storagePath ) {
      try {
         // Delete directory for importing new Aspect Models.
         FileUtils.deleteDirectory( new File( storagePath ) );

         final Path packagePath = Path.of( storagePath );
         UnzipUtils.unzipPackageFile( zipFile, packagePath );

         final ModelResolverStrategy strategy = modelResolverRepository.getStrategy(
               LocalFolderResolverStrategy.class );

         final LocalPackageInfo localPackageInfo = strategy.getLocalPackageInformation( storagePath );

         final ProcessPackage processPackage = new ProcessPackage(
               localPackageInfo.getInValidFiles() );

         localPackageInfo.getValidFiles().forEach( localPackageInformation -> {
                  final Boolean modelExist = strategy.checkModelExist( localPackageInformation.getAspectModelFile(),
                        ApplicationSettings.getMetaModelStoragePath() );

                  final ViolationReport violationReport = new ViolationReport();

                  ModelUtils.validateModel( localPackageInformation.getAspectModel(), storagePath, aspectModelValidator,
                        violationReport );

                  final ValidFile validFile = new ValidFile( localPackageInformation.getAspectModelFile(),
                        violationReport, modelExist );

                  getMissingAspectModelFiles( violationReport ).forEach( processPackage::addMissingFiles );

                  processPackage.addValidFiles( validFile );
               }
         );

         return processPackage;
      } catch ( final IOException e ) {
         LOG.error( "Cannot delete imported package folder." );
         throw new FileNotFoundException(
               String.format( "Package folder: %s was not deleted successfully.", storagePath ), e );
      }
   }

   public List<String> importAspectModelPackage( final List<String> aspectModelFiles, final String storagePath ) {
      try {

         final List<String> fileLocations =
               aspectModelFiles.stream().map( aspectModelFileName -> copyFileToDirectory(
                                     aspectModelFileName, storagePath, ApplicationSettings.getMetaModelStoragePath() ) )
                               .collect( Collectors.toList() );

         FileUtils.deleteDirectory( new File( storagePath ) );

         return fileLocations;
      } catch ( final IOException e ) {
         LOG.error( "Cannot delete imported package folder." );
         throw new FileNotFoundException(
               String.format( "Package folder: %s was not deleted successfully.", storagePath ), e );
      }
   }

   private String copyFileToDirectory( final String aspectModelFileName, final String sourceStorage,
         final String destStorage ) {
      final LocalFolderResolverUtils.FolderStructure folderStructure = LocalFolderResolverUtils.extractFilePath(
            aspectModelFileName );

      final String absoluteAspectModelPath = sourceStorage + File.separator + folderStructure.toString();

      final File aspectModelStoragePath = new File( destStorage + File.separator + folderStructure.getFileRootPath()
            + File.separator + folderStructure.getVersion() );

      if ( !aspectModelStoragePath.exists() ) {
         aspectModelStoragePath.mkdir();
      }

      try {
         FileUtils.copyFileToDirectory( new File( absoluteAspectModelPath ), aspectModelStoragePath );
         return folderStructure.toString();
      } catch ( final IOException e ) {
         throw new FileNotFoundException(
               String.format( "Cannot copy file %s to %s", folderStructure.getFileName(),
                     aspectModelStoragePath ) );
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

   public void backupWorkspace( final String sourceStoragePath, final String destStoragePath ) {
      try {
         final SimpleDateFormat sdf = new SimpleDateFormat( "yyyy.MM.dd-HH.mm.ss" );
         final String timestamp = sdf.format( new Timestamp( System.currentTimeMillis() ) );
         final String fileName = "backup-" + timestamp + ".zip";
         final byte[] zipFile = ZipUtils.createZipFile( fileName, sourceStoragePath );

         final File file = new File( sourceStoragePath + File.separator + fileName );
         final File destStorageFile = new File( destStoragePath + File.separator + fileName );

         FileUtils.writeByteArrayToFile( destStorageFile, zipFile );
         Files.deleteIfExists( file.toPath() );
      } catch ( final IOException e ) {
         LOG.error( "Cannot create backup package." );
         throw new FileNotFoundException( "Error while creating backup package.", e );
      }
   }

   private List<MissingFile> getMissingAspectModelFiles( final ViolationReport violationReport ) {
      final List<ViolationError> validationErrors = violationReport.getViolationErrors().stream().filter(
            violation -> {
               return violation.getErrorCode().equals(
                     ProcessingViolation.ERROR_CODE );
            } ).toList();

      if ( validationErrors.isEmpty() ) {
         return List.of();
      }

      return validationErrors.stream()
                             .filter(
                                   validation -> ModelUtils.URN_PATTERN.matcher( validation.getFocusNode().toString() )
                                                                       .matches() )
                             .map( validation -> {
                                final String valueUrn = null;
                                // ((ValidationError.Semantic) validationError).getValue();

                                final FileSystemStrategy fileSystemStrategy = new FileSystemStrategy(
                                      Path.of( ApplicationSettings.getMetaModelStoragePath() ) );

                                final String analysedFile = fileSystemStrategy.getAspectModelFile(
                                      validation.getFocusNode() );

                                final String missingFile = "";

                                //                                      fileSystemStrategy.getAspectModelFile( AspectModelUrn.fromUrn( valueUrn ) );

                                return new MissingFile( new File( analysedFile ).getName(),
                                      new File( missingFile ).getName(),
                                      String.format(
                                            "Referenced Aspect Model %s could not be found in Aspect Model %s.",
                                            missingFile.replace( "/", "." ), analysedFile.replace( "/", "." ) )
                                );
                             } )
                             .toList();
   }
}
