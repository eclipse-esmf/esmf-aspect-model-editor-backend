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
import java.nio.charset.StandardCharsets;
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
import org.eclipse.esmf.ame.repository.ModelResolverRepository;
import org.eclipse.esmf.ame.repository.strategy.LocalFolderResolverStrategy;
import org.eclipse.esmf.ame.repository.strategy.ModelResolverStrategy;
import org.eclipse.esmf.ame.repository.strategy.utils.LocalFolderResolverUtils;
import org.eclipse.esmf.ame.resolver.strategy.FileSystemStrategy;
import org.eclipse.esmf.ame.resolver.strategy.InMemoryStrategy;
import org.eclipse.esmf.ame.resolver.strategy.model.FolderStructure;
import org.eclipse.esmf.ame.resolver.strategy.model.NamespaceFileContent;
import org.eclipse.esmf.ame.resolver.strategy.utils.ResolverUtils;
import org.eclipse.esmf.ame.services.model.ElementMissingReport;
import org.eclipse.esmf.ame.services.model.FileValidationReport;
import org.eclipse.esmf.ame.services.model.NamespaceFileCollection;
import org.eclipse.esmf.ame.services.model.NamespaceFileReport;
import org.eclipse.esmf.ame.services.utils.UnzipUtils;
import org.eclipse.esmf.ame.services.utils.ZipUtils;
import org.eclipse.esmf.ame.utils.ModelUtils;
import org.eclipse.esmf.ame.validation.model.ViolationError;
import org.eclipse.esmf.ame.validation.model.ViolationReport;
import org.eclipse.esmf.ame.validation.utils.ValidationUtils;
import org.eclipse.esmf.aspectmodel.resolver.services.DataType;
import org.eclipse.esmf.aspectmodel.resolver.services.VersionedModel;
import org.eclipse.esmf.aspectmodel.shacl.violation.ProcessingViolation;
import org.eclipse.esmf.aspectmodel.urn.AspectModelUrn;
import org.eclipse.esmf.aspectmodel.validation.services.AspectModelValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import io.vavr.control.Try;

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

   public FileValidationReport validateAspectModelsForExport( final List<NamespaceFileCollection> aspectModelFiles ) {
      final ModelResolverStrategy strategy = modelResolverRepository.getStrategy( LocalFolderResolverStrategy.class );
      final FileValidationReport fileValidationReport = new FileValidationReport();

      AspectModelToExportCache.clear();

      Map<String, NamespaceFileReport> validFiles =
            aspectModelFiles.stream()
                            .flatMap( data -> data.getFiles().stream().map( fileName -> {
                               String aspectModel = strategy.getModelAsString( data.getNamespace(), fileName );
                               AspectModelToExportCache.put( data.getNamespace() + ":" + fileName, aspectModel );
                               final FileSystemStrategy fileSystemStrategy = new FileSystemStrategy( aspectModel );
                               final Try<VersionedModel> versionedModel = ResolverUtils.fetchVersionModel(
                                     fileSystemStrategy );
                               ViolationReport report = ValidationUtils.validateModel( versionedModel,
                                     aspectModelValidator );
                               return new NamespaceFileReport( data.getNamespace(), fileName, report );
                            } ) )
                            .collect( Collectors.toMap( namespaceFileReport -> namespaceFileReport.getNamespace() + ":"
                                  + namespaceFileReport.getFileName(), Function.identity() ) );

      validFiles.values().forEach( fileValidationReport::addValidFile );

      validFiles.values().stream().flatMap(
            namespaceFileReport -> getMissingAspectModelFiles( namespaceFileReport.getViolationReport(),
                  namespaceFileReport.getFileName(),
                  modelPath ).stream() ).forEach( fileValidationReport::addMissingElement );

      return fileValidationReport;
   }

   public byte[] exportAspectModelPackage( final String zipFileName ) {
      try {
         return ZipUtils.createPackageFromCache( AspectModelToExportCache );
      } catch ( final IOException e ) {
         LOG.error( "Cannot create exported package file." );
         throw new FileNotFoundException( String.format( "Error while creating the package file: %s", zipFileName ),
               e );
      }
   }

   public FileValidationReport validateImportAspectModelPackage( final MultipartFile zipFile ) {
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
         rootPath.sorted( Comparator.reverseOrder() ).forEach( path -> {
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

   private FileValidationReport validateValidFiles( final String modelStoragePath ) {
      final ModelResolverStrategy strategy = modelResolverRepository.getStrategy( LocalFolderResolverStrategy.class );
      final List<NamespaceFileContent> namespaceFileContent = strategy.getImportedNamespaceFileContent();

      return namespaceFileContent.stream().map( content -> {
         final Boolean modelExist = strategy.checkModelExist( content.getNamespace(), content.getFileName() );

         final Path root = importFileSystem.getRootDirectories().iterator().next();

         final InMemoryStrategy inMemoryStrategy = new InMemoryStrategy( content, root, importFileSystem );
         final Try<VersionedModel> versionedModel = ResolverUtils.fetchVersionModel( inMemoryStrategy );

         final ViolationReport violationReport = ValidationUtils.validateModel( versionedModel,
               aspectModelValidator );

         final NamespaceFileReport namespaceFileReport = new NamespaceFileReport( content.getNamespace(),
               content.getFileName(), violationReport, modelExist );

         final List<ElementMissingReport> missingFiles = getMissingAspectModelFiles( violationReport,
               content.getFileName(), modelStoragePath );

         return new FileValidationReport( namespaceFileReport, missingFiles );
      } ).reduce( new FileValidationReport(), FileValidationReport::merge );
   }

   public List<String> importAspectModelPackage( final List<NamespaceFileCollection> aspectModelFiles ) {
      final ModelResolverStrategy strategy = modelResolverRepository.getStrategy( LocalFolderResolverStrategy.class );

      return aspectModelFiles.stream().flatMap( data -> data.getFiles().stream().map( fileName -> {
         try {
            final FolderStructure folderStructure = LocalFolderResolverUtils.extractFilePath( data.getNamespace() );
            folderStructure.setFileName( fileName );
            String aspectModel = ResolverUtils.readString(
                  importFileSystem.getPath( folderStructure.toString() ), StandardCharsets.UTF_8 );
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

   private List<ElementMissingReport> getMissingAspectModelFiles( final ViolationReport violationReport,
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
         return new ElementMissingReport( fileName, (focusNode != null ? focusNode.toString() : ""),
               missingAspectModelFile,
               errorMessage );
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
