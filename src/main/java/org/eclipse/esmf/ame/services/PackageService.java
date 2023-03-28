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
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.eclipse.esmf.ame.exceptions.FileNotFoundException;
import org.eclipse.esmf.ame.exceptions.InvalidAspectModelException;
import org.eclipse.esmf.ame.model.ValidationProcess;
import org.eclipse.esmf.ame.model.packaging.MissingElement;
import org.eclipse.esmf.ame.model.packaging.ProcessPackage;
import org.eclipse.esmf.ame.model.packaging.ValidFile;
import org.eclipse.esmf.ame.model.repository.LocalPackageInfo;
import org.eclipse.esmf.ame.model.validation.ViolationError;
import org.eclipse.esmf.ame.model.validation.ViolationReport;
import org.eclipse.esmf.ame.repository.ModelResolverRepository;
import org.eclipse.esmf.ame.repository.strategy.LocalFolderResolverStrategy;
import org.eclipse.esmf.ame.repository.strategy.ModelResolverStrategy;
import org.eclipse.esmf.ame.services.utils.ModelUtils;
import org.eclipse.esmf.ame.services.utils.UnzipUtils;
import org.eclipse.esmf.ame.services.utils.ZipUtils;
import org.eclipse.esmf.aspectmodel.resolver.services.DataType;
import org.eclipse.esmf.aspectmodel.shacl.violation.ProcessingViolation;
import org.eclipse.esmf.aspectmodel.urn.AspectModelUrn;
import org.eclipse.esmf.aspectmodel.validation.services.AspectModelValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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
         final ValidationProcess validationProcess, final Path modelStoragePath ) {
      try {
         final ModelResolverStrategy strategy = modelResolverRepository.getStrategy(
               LocalFolderResolverStrategy.class );

         strategy.deleteDirectory( validationProcess.getPath().toFile() );

         ModelUtils.copyAspectModelToDirectory( aspectModelFiles, modelStoragePath.toString(),
               validationProcess.getPath().toString() );

         return validateAspectModelsFromDirectory( aspectModelFiles, strategy, validationProcess, modelStoragePath );
      } catch ( final Exception error ) {
         FileUtils.deleteQuietly( new File( validationProcess.getPath().toString() ) );
         throw new InvalidAspectModelException( error.getMessage() );
      }
   }

   private ProcessPackage validateAspectModelsFromDirectory( final List<String> aspectModelFiles,
         final ModelResolverStrategy strategy, final ValidationProcess validationProcess,
         final Path modelStoragePath ) {
      final ProcessPackage processPackage = new ProcessPackage();

      aspectModelFiles.forEach( fileName -> {
         final String modelAsString = strategy.getModelAsString( fileName, validationProcess.getPath().toString() );
         final AspectModelUrn aspectModelUrn = strategy.convertAspectModelFileNameToUrn( fileName );
         strategy.saveModel( Optional.of( aspectModelUrn.toString() ),
               ModelUtils.getPrettyPrintedModel( modelAsString, validationProcess ),
               validationProcess.getPath().toString() );
         final ViolationReport violationReport = ModelUtils.validateModel( modelAsString, aspectModelValidator,
               validationProcess );
         processPackage.addValidFiles( new ValidFile( fileName, violationReport ) );
         getMissingAspectModelFiles( violationReport, fileName, modelStoragePath.toString() ).forEach(
               processPackage::addMissingElement );
      } );

      return processPackage;
   }

   public byte[] exportAspectModelPackage( final String zipFileName, final ValidationProcess validationProcess ) {
      try {
         final byte[] zipFile = ZipUtils.createZipFile( zipFileName, validationProcess.getPath().toString() );
         final ModelResolverStrategy strategy = modelResolverRepository.getStrategy(
               LocalFolderResolverStrategy.class );
         strategy.deleteDirectory( new File( validationProcess.getPath().toString() ) );
         return zipFile;
      } catch ( final IOException e ) {
         LOG.error( "Cannot create exported package file." );
         throw new FileNotFoundException( String.format( "Error while creating the package file: %s", zipFileName ),
               e );
      }
   }

   public ProcessPackage validateImportAspectModelPackage( final MultipartFile zipFile,
         final ValidationProcess validationProcess, final Path modelStoragePath ) {
      try {
         final ModelResolverStrategy strategy = modelResolverRepository.getStrategy(
               LocalFolderResolverStrategy.class );
         strategy.deleteDirectory( validationProcess.getPath().toFile() );

         unzipPackageFile( zipFile, validationProcess.getPath() );

         final LocalPackageInfo localPackageInfo = strategy.getLocalPackageInformation(
               validationProcess.getPath().toString() );
         final ProcessPackage processPackage = new ProcessPackage( localPackageInfo.getInValidFiles() );

         validateValidFiles( localPackageInfo, strategy, processPackage, validationProcess,
               modelStoragePath.toString() );

         return processPackage;
      } catch ( final Exception e ) {
         LOG.error( "Cannot unzip package file." );
         throw new IllegalArgumentException(
               String.format( "Package file: %s was not unzipped successfully.", zipFile.getOriginalFilename() ), e );
      }
   }

   private void unzipPackageFile( final MultipartFile zipFile, final Path packagePath ) throws IOException {
      Files.createDirectories( packagePath );
      try ( final InputStream inputStream = zipFile.getInputStream() ) {
         UnzipUtils.unzipPackageFile( inputStream, packagePath );
      }
   }

   private void validateValidFiles( final LocalPackageInfo localPackageInfo, final ModelResolverStrategy strategy,
         final ProcessPackage processPackage, final ValidationProcess validationProcess,
         final String modelStoragePath ) {

      localPackageInfo.getValidFiles().forEach( fileInfo -> {
         final String aspectModelFile = fileInfo.getAspectModelFile();
         final Boolean modelExist = strategy.checkModelExist( aspectModelFile,
               ValidationProcess.MODELS.getPath().toString() );

         final ViolationReport violationReport = ModelUtils.validateModel( fileInfo.getAspectModel(),
               aspectModelValidator, validationProcess );

         processPackage.addValidFiles( new ValidFile( aspectModelFile, violationReport, modelExist ) );
         getMissingAspectModelFiles( violationReport, aspectModelFile, modelStoragePath ).forEach(
               processPackage::addMissingElement );
      } );
   }

   public List<String> importAspectModelPackage( final List<String> aspectModelFiles,
         final ValidationProcess validationProcess ) {
      final ValidationProcess modelsProcess = ValidationProcess.MODELS;

      final List<String> fileLocations = ModelUtils.copyAspectModelToDirectory( aspectModelFiles,
            validationProcess.getPath().toString(), modelsProcess.getPath().toString() );

      final ModelResolverStrategy strategy = modelResolverRepository.getStrategy( LocalFolderResolverStrategy.class );

      aspectModelFiles.forEach( fileName -> {
         final String modelAsString = strategy.getModelAsString( fileName, modelsProcess.getPath().toString() );
         final AspectModelUrn aspectModelUrn = strategy.convertAspectModelFileNameToUrn( fileName );

         strategy.saveModel( Optional.of( aspectModelUrn.toString() ),
               ModelUtils.getPrettyPrintedModel( modelAsString, validationProcess ),
               modelsProcess.getPath().toString() );
      } );

      strategy.deleteDirectory( validationProcess.getPath().toFile() );

      return fileLocations;
   }

   private List<MissingElement> getMissingAspectModelFiles( final ViolationReport violationReport,
         final String fileName, final String modelStoragePath ) {
      final List<ViolationError> violationErrors = violationReport.getViolationErrors().stream()
                                                                  .filter( violation -> violation.getErrorCode() != null
                                                                        && violation.getErrorCode().equals(
                                                                        ProcessingViolation.ERROR_CODE ) )
                                                                  .toList();

      if ( violationErrors.isEmpty() ) {
         return List.of();
      }

      return violationErrors.stream().map( validation -> {
         final AspectModelUrn focusNode = validation.getFocusNode() != null ? validation.getFocusNode() : null;

         final String missingAspectModelFile = ModelUtils.getAspectModelFile( modelStoragePath,
               focusNode );

         final String errorMessage = String.format(
               "Referenced element: '%s' could not be found in Aspect Model file: '%s'.", focusNode, fileName );
         return new MissingElement( fileName.split( ":" )[2], (focusNode != null ? focusNode.toString() : ""),
               missingAspectModelFile, errorMessage );
      } ).toList();
   }

   public void backupWorkspace( final Path sourceStoragePath, final Path destStoragePath ) {
      try {
         final SimpleDateFormat sdf = new SimpleDateFormat( "yyyy.MM.dd-HH.mm.ss" );
         final String timestamp = sdf.format( new Timestamp( System.currentTimeMillis() ) );
         final String fileName = "backup-" + timestamp + ".zip";
         final byte[] zipFile = ZipUtils.createZipFile( fileName, sourceStoragePath.toString() );

         final File file = sourceStoragePath.resolve( fileName ).toFile();
         final File destStorageFile = destStoragePath.resolve( fileName ).toFile();

         FileUtils.writeByteArrayToFile( destStorageFile, zipFile );
         Files.deleteIfExists( file.toPath() );
      } catch ( final IOException e ) {
         LOG.error( "Cannot create backup package." );
         throw new FileNotFoundException( "Error while creating backup package.", e );
      }
   }
}
