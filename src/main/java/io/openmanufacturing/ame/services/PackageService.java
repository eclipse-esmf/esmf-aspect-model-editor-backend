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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import io.openmanufacturing.ame.config.ApplicationSettings;
import io.openmanufacturing.ame.exceptions.FileNotFoundException;
import io.openmanufacturing.ame.model.packaging.MissingElement;
import io.openmanufacturing.ame.model.packaging.ProcessPackage;
import io.openmanufacturing.ame.model.packaging.ValidFile;
import io.openmanufacturing.ame.model.repository.LocalPackageInfo;
import io.openmanufacturing.ame.model.validation.ViolationError;
import io.openmanufacturing.ame.model.validation.ViolationReport;
import io.openmanufacturing.ame.repository.ModelResolverRepository;
import io.openmanufacturing.ame.repository.strategy.LocalFolderResolverStrategy;
import io.openmanufacturing.ame.repository.strategy.ModelResolverStrategy;
import io.openmanufacturing.ame.repository.strategy.utils.LocalFolderResolverUtils;
import io.openmanufacturing.ame.services.utils.ModelUtils;
import io.openmanufacturing.ame.services.utils.UnzipUtils;
import io.openmanufacturing.ame.services.utils.ZipUtils;
import io.openmanufacturing.sds.aspectmodel.resolver.services.DataType;
import io.openmanufacturing.sds.aspectmodel.urn.AspectModelUrn;
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

   public ProcessPackage validateAspectModelsForExport( final List<String> aspectModelFiles,
         final String storagePath ) {
      try {
         // Folder will be deleted when the validation starts.
         LocalFolderResolverUtils.deleteDirectory( storagePath );

         final ModelResolverStrategy strategy = modelResolverRepository.getStrategy(
               LocalFolderResolverStrategy.class );

         // Save all Aspect Models to export storage path
         ModelUtils.copyAspectModelToDirectory( aspectModelFiles, ApplicationSettings.getMetaModelStoragePath(),
               storagePath );

         // Validate all Aspect Models from export storage path and create export package model
         return validateAspectModelsFromDirectory( aspectModelFiles, strategy, storagePath );
      } catch ( final IOException e ) {
         LOG.error( "Cannot delete exported package folder." );
         throw new FileNotFoundException( String.format( "Unable to delete folder: %s", storagePath ), e );
      }
   }

   private ProcessPackage validateAspectModelsFromDirectory( final List<String> aspectModelFiles,
         final ModelResolverStrategy strategy, final String storagePath ) {
      final ProcessPackage processPackage = new ProcessPackage();

      aspectModelFiles.forEach( fileName -> {
         final String aspectModel = strategy.getModelAsString( fileName, storagePath );
         final ViolationReport violationReport = new ViolationReport();
         ModelUtils.validateModel( aspectModel, storagePath, aspectModelValidator, violationReport );
         processPackage.addValidFiles( new ValidFile( fileName, violationReport ) );
         getMissingAspectModelFiles( violationReport, fileName ).forEach( processPackage::addMissingElement );
      } );

      return processPackage;
   }

   public byte[] exportAspectModelPackage( final String zipFileName, final String storagePath ) {
      try {
         final byte[] zipFile = ZipUtils.createZipFile( zipFileName, storagePath );
         LocalFolderResolverUtils.deleteDirectory( storagePath );
         return zipFile;
      } catch ( final IOException e ) {
         LOG.error( "Cannot create exported package file." );
         throw new FileNotFoundException( String.format( "Error while creating the package file: %s", zipFileName ),
               e );
      }
   }

   public ProcessPackage validateImportAspectModelPackage( final MultipartFile zipFile, final String storagePath ) {
      try {
         // Delete directory for importing new Aspect Models.
         LocalFolderResolverUtils.deleteDirectory( storagePath );

         final Path packagePath = Path.of( storagePath );
         unzipPackageFile( zipFile, packagePath );

         final ModelResolverStrategy strategy = modelResolverRepository.getStrategy(
               LocalFolderResolverStrategy.class );
         final LocalPackageInfo localPackageInfo = strategy.getLocalPackageInformation( storagePath );
         final ProcessPackage processPackage = new ProcessPackage( localPackageInfo.getInValidFiles() );

         validateValidFiles( localPackageInfo, strategy, storagePath, processPackage );

         return processPackage;
      } catch ( final IOException e ) {
         LOG.error( "Cannot delete imported package folder." );
         throw new FileNotFoundException(
               String.format( "Package folder: %s was not deleted successfully.", storagePath ), e );
      }
   }

   private void unzipPackageFile( final MultipartFile zipFile, final Path packagePath ) throws IOException {
      Files.createDirectories( packagePath );
      try ( final InputStream inputStream = zipFile.getInputStream() ) {
         UnzipUtils.unzipPackageFile( inputStream, packagePath );
      }
   }

   private void validateValidFiles( final LocalPackageInfo localPackageInfo, final ModelResolverStrategy strategy,
         final String storagePath, final ProcessPackage processPackage ) {
      localPackageInfo.getValidFiles().forEach( fileInfo -> {
         final String aspectModelFile = fileInfo.getAspectModelFile();
         final Boolean modelExist = strategy.checkModelExist( aspectModelFile,
               ApplicationSettings.getMetaModelStoragePath() );

         final ViolationReport violationReport = new ViolationReport();
         ModelUtils.validateModel( fileInfo.getAspectModel(), storagePath, aspectModelValidator, violationReport );

         processPackage.addValidFiles( new ValidFile( aspectModelFile, violationReport, modelExist ) );
         getMissingAspectModelFiles( violationReport, aspectModelFile ).forEach( processPackage::addMissingElement );
      } );
   }

   public List<String> importAspectModelPackage( final List<String> aspectModelFiles, final String storagePath ) {
      try {
         final List<String> fileLocations = ModelUtils.copyAspectModelToDirectory( aspectModelFiles, storagePath,
               ApplicationSettings.getMetaModelStoragePath() );

         LocalFolderResolverUtils.deleteDirectory( storagePath );

         return fileLocations;
      } catch ( final IOException e ) {
         LOG.error( "Cannot delete imported package folder." );
         throw new FileNotFoundException(
               String.format( "Package folder: %s was not deleted successfully.", storagePath ), e );
      }
   }

   private List<MissingElement> getMissingAspectModelFiles( final ViolationReport violationReport,
         final String fileName ) {
      final List<ViolationError> violationErrors = violationReport.getViolationErrors().stream()
                                                                  .filter( ModelUtils.isProcessingViolation() ).filter(
                  validation -> ModelUtils.URN_PATTERN.matcher( validation.getFocusNode().toString() ).matches() )
                                                                  .toList();

      if ( violationErrors.isEmpty() ) {
         return List.of();
      }

      return violationErrors.stream().map( validation -> {
         final AspectModelUrn focusNode = validation.getFocusNode();

         final String missingAspectModelFile = ModelUtils.getAspectModelFile(
               ApplicationSettings.getMetaModelStoragePath(),
               focusNode );

         final String errorMessage = String.format(
               "Referenced element: '%s' could not be found in Aspect Model file: '%s'.", focusNode,
               fileName );
         return new MissingElement( fileName.split( ":" )[2], focusNode.toString(), missingAspectModelFile,
               errorMessage );
      } ).toList();
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
}
