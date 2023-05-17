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
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.esmf.ame.exceptions.FileCannotDeleteException;
import org.eclipse.esmf.ame.exceptions.FileNotFoundException;
import org.eclipse.esmf.ame.model.packaging.AspectModelFiles;
import org.eclipse.esmf.ame.model.packaging.MissingElement;
import org.eclipse.esmf.ame.model.packaging.ProcessPackage;
import org.eclipse.esmf.ame.model.packaging.ValidFile;
import org.eclipse.esmf.ame.model.repository.AspectModelInformation;
import org.eclipse.esmf.ame.model.resolver.FolderStructure;
import org.eclipse.esmf.ame.model.validation.ViolationError;
import org.eclipse.esmf.ame.model.validation.ViolationReport;
import org.eclipse.esmf.ame.repository.ModelResolverRepository;
import org.eclipse.esmf.ame.repository.strategy.LocalFolderResolverStrategy;
import org.eclipse.esmf.ame.repository.strategy.ModelResolverStrategy;
import org.eclipse.esmf.ame.repository.strategy.utils.LocalFolderResolverUtils;
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
   private final String modelPath;
   private final ModelResolverRepository modelResolverRepository;
   private final FileSystem importFileSystem;
   private final Map<String, String> AspectModelToExportCache = new HashMap<>();

   public PackageService( final AspectModelValidator aspectModelValidator, final String modelPath,
         final ModelResolverRepository modelResolverRepository, final FileSystem importFileSystem ) {
      this.aspectModelValidator = aspectModelValidator;
      this.modelPath = modelPath;
      this.modelResolverRepository = modelResolverRepository;
      this.importFileSystem = importFileSystem;

      DataType.setupTypeMapping();
   }

   public ProcessPackage validateAspectModelsForExport( final List<AspectModelFiles> aspectModelFiles ) {
      final ModelResolverStrategy strategy = modelResolverRepository.getStrategy( LocalFolderResolverStrategy.class );
      final ProcessPackage processPackage = new ProcessPackage();

      AspectModelToExportCache.clear();

      Map<String, ValidFile> validFiles = aspectModelFiles.stream()
                                                          .flatMap( data -> data.getFiles().stream()
                                                                                .map( fileName -> {
                                                                                   String model = strategy.getModelAsString(
                                                                                         data.getNamespace(),
                                                                                         fileName );
                                                                                   AspectModelToExportCache.put(
                                                                                         data.getNamespace() + ":"
                                                                                               + fileName, model );
                                                                                   ViolationReport report = ModelUtils.validateModel(
                                                                                         model, aspectModelValidator );
                                                                                   return new ValidFile(
                                                                                         data.getNamespace(), fileName,
                                                                                         report );
                                                                                } ) )
                                                          .collect( Collectors.toMap(
                                                                validFile -> validFile.getNamespace() + ":"
                                                                      + validFile.getFileName(),
                                                                Function.identity() ) );

      validFiles.values().forEach( processPackage::addValidFile );

      validFiles.values().stream()
                .flatMap(
                      validFile -> getMissingAspectModelFiles( validFile.getViolationReport(), validFile.getFileName(),
                            modelPath ).stream() )
                .forEach( processPackage::addMissingElement );

      return processPackage;
   }

   public byte[] exportAspectModelPackage( final String zipFileName ) {
      try {
         return ZipUtils.createPackageFromCache( zipFileName, AspectModelToExportCache );
      } catch ( final IOException e ) {
         LOG.error( "Cannot create exported package file." );
         throw new FileNotFoundException( String.format( "Error while creating the package file: %s", zipFileName ),
               e );
      }
   }

   public ProcessPackage validateImportAspectModelPackage( final MultipartFile zipFile ) {
      try ( final InputStream inputStream = zipFile.getInputStream() ) {
         deleteInMemoryFileSystem();

         UnzipUtils.extractFilesFromPackage( inputStream, importFileSystem );
         return validateValidFiles( modelPath );
      } catch ( final Exception e ) {
         LOG.error( "Cannot unzip package file." );
         throw new IllegalArgumentException(
               String.format( "Package file %s was not unzipped successfully. %s.", zipFile.getOriginalFilename(),
                     e.getMessage() ), e );
      }
   }

   public void deleteInMemoryFileSystem() throws IOException {
      Path root = importFileSystem.getRootDirectories().iterator().next();

      try ( Stream<Path> rootPath = Files.walk( importFileSystem.getPath( "/" ) ) ) {
         rootPath.sorted( Comparator.reverseOrder() )
                 .forEach( path -> {
                    try {
                       if ( !path.equals( root ) ) {
                          Files.delete( path );
                       }
                    } catch ( IOException e ) {
                       throw new FileCannotDeleteException(
                             "Failed to delete files and directories on the in-memory file system.", e );
                    }
                 } );
      }
   }

   private ProcessPackage validateValidFiles( final String modelStoragePath ) {
      final ModelResolverStrategy strategy = modelResolverRepository.getStrategy( LocalFolderResolverStrategy.class );
      final List<AspectModelInformation> aspectModelInformations = strategy.getImportedAspectModelInformation();

      return aspectModelInformations.stream()
                                    .map( fileInfo -> {
                                       final String fileName = fileInfo.getFileName();
                                       final Boolean modelExist = strategy.checkModelExist( fileInfo.getNamespace(),
                                             fileName );

                                       final ViolationReport violationReport = ModelUtils.validateModelInMemoryFiles(
                                             fileInfo.getAspectModel(), aspectModelValidator, importFileSystem );

                                       final ValidFile validFile = new ValidFile( fileInfo.getNamespace(), fileName,
                                             violationReport, modelExist );
                                       final List<MissingElement> missingFiles = getMissingAspectModelFiles(
                                             violationReport, fileName, modelStoragePath );

                                       return new ProcessPackage( validFile, missingFiles );
                                    } )
                                    .reduce( new ProcessPackage(), ProcessPackage::merge );
   }

   public List<String> importAspectModelPackage( final List<AspectModelFiles> aspectModelFiles ) {
      final ModelResolverStrategy strategy = modelResolverRepository.getStrategy( LocalFolderResolverStrategy.class );

      return aspectModelFiles.stream().flatMap( data -> data.getFiles().stream().map( fileName -> {
         try {
            final FolderStructure folderStructure = LocalFolderResolverUtils.extractFilePath( data.getNamespace() );
            folderStructure.setFileName( fileName );
            String aspectModel = Files.readString( importFileSystem.getPath( folderStructure.toString() ) );
            Optional<String> namespaceVersion = Optional.of(
                  folderStructure.getFileRootPath() + File.separator + folderStructure.getVersion() );

            strategy.saveModel( namespaceVersion, Optional.of( fileName ), aspectModel );

            return folderStructure.toString();
         } catch ( final IOException e ) {
            throw new FileNotFoundException(
                  String.format( "Cannot import Aspect Model with name %s to workspace", fileName ) );
         }
      } ) ).toList();
   }

   private List<MissingElement> getMissingAspectModelFiles( final ViolationReport violationReport,
         final String fileName, final String modelStoragePath ) {
      final List<ViolationError> violationErrors = violationReport.getViolationErrors().stream().filter(
                                                                        violation -> violation.getErrorCode() != null && violation.getErrorCode()
                                                                                                                                  .equals( ProcessingViolation.ERROR_CODE ) )
                                                                  .toList();

      if ( violationErrors.isEmpty() ) {
         return List.of();
      }

      return violationErrors.stream().map( validation -> {
         final AspectModelUrn focusNode = validation.getFocusNode() != null ? validation.getFocusNode() : null;

         final String missingAspectModelFile = ModelUtils.getAspectModelFile( modelStoragePath, focusNode );

         final String errorMessage = String.format(
               "Referenced element: '%s' could not be found in Aspect Model file: '%s'.", focusNode, fileName );
         return new MissingElement( fileName, (focusNode != null ? focusNode.toString() : ""),
               missingAspectModelFile, errorMessage );
      } ).toList();
   }

   public void backupWorkspace( final String aspectModelPath ) {
      try {
         final SimpleDateFormat sdf = new SimpleDateFormat( "yyyy.MM.dd-HH.mm.ss" );
         final String timestamp = sdf.format( new Timestamp( System.currentTimeMillis() ) );
         final String fileName = "backup-" + timestamp + ".zip";
         ZipUtils.createPackageFromWorkspace( fileName, aspectModelPath, modelPath );
      } catch ( final IOException e ) {
         LOG.error( "Cannot create backup package." );
         throw new FileNotFoundException( "Error while creating backup package.", e );
      }
   }
}
