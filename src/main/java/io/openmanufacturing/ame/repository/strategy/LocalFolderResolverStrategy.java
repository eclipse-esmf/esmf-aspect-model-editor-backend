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

package io.openmanufacturing.ame.repository.strategy;

import static io.openmanufacturing.ame.services.utils.ModelUtils.inMemoryStrategy;
import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import io.openmanufacturing.ame.config.ApplicationSettings;
import io.openmanufacturing.ame.exceptions.FileNotFoundException;
import io.openmanufacturing.ame.exceptions.FileReadException;
import io.openmanufacturing.ame.exceptions.FileWriteException;
import io.openmanufacturing.ame.exceptions.InvalidAspectModelException;
import io.openmanufacturing.ame.model.repository.LocalPackageInfo;
import io.openmanufacturing.ame.model.repository.ValidFile;
import io.openmanufacturing.ame.repository.strategy.utils.LocalFolderResolverUtils;
import io.openmanufacturing.ame.services.utils.ModelUtils;
import io.openmanufacturing.sds.aspectmodel.resolver.services.ExtendedXsdDataType;
import io.openmanufacturing.sds.aspectmodel.urn.AspectModelUrn;

@Service
public class LocalFolderResolverStrategy implements ModelResolverStrategy {

   private static final Logger LOG = LoggerFactory.getLogger( LocalFolderResolverStrategy.class );
   private static final String TTL_FILE_DOES_NOT_EXISTS = "File %s does not exists.";
   private final ApplicationSettings applicationSettings;
   private Optional<Map<String, List<String>>> namespaces = Optional.empty();

   public LocalFolderResolverStrategy( final ApplicationSettings applicationSettings ) {
      this.applicationSettings = applicationSettings;
   }

   @Override
   public Boolean checkModelExist( final @Nonnull String namespace, final String storagePath ) {
      final String filePath = LocalFolderResolverUtils.extractFilePath( namespace ).toString();
      final String qualifiedFilePath = getQualifiedFilePath( filePath, storagePath );

      return new File( qualifiedFilePath ).exists();
   }

   @Override
   public String getModelAsString( final @Nonnull String namespace, final String storagePath ) {
      return getFileContent( getModelAsFile( namespace, storagePath ) );
   }

   @Override
   public File getModelAsFile( final @Nonnull String namespace, final String storagePath ) {
      final String filePath = LocalFolderResolverUtils.extractFilePath( namespace ).toString();
      final String qualifiedFilePath = getQualifiedFilePath( filePath, storagePath );
      final File storeFile = getFileInstance( qualifiedFilePath );

      if ( storeFile.exists() ) {
         return storeFile;
      } else {
         throw new FileNotFoundException( String.format( TTL_FILE_DOES_NOT_EXISTS, namespace ) );
      }
   }

   @Override
   public String saveModel( final Optional<String> urn, final @Nonnull String turtleData, final String storagePath ) {
      try {
         ExtendedXsdDataType.setChecking( false );

         final String filePath = getFilePath( urn.orElse( StringUtils.EMPTY ), turtleData, storagePath );
         final File storeFile = getFileInstance( getQualifiedFilePath( filePath, storagePath ) );

         writeToFile( turtleData, storeFile );

         ExtendedXsdDataType.setChecking( true );
         return filePath;
      } catch ( final IOException e ) {
         throw new FileWriteException( "File cannot be written", e );
      }
   }

   private String getFilePath( final String urn, final String turtleData, final String storagePath ) {

      if ( urn.isEmpty() ) {
         return getFilePathBasedOnTurtleData( turtleData, storagePath ) + applicationSettings.getFileType();
      }

      if ( ":latest.ttl".equalsIgnoreCase( urn ) || urn.contains( applicationSettings.getFileType() ) ) {
         return LocalFolderResolverUtils.extractFilePath( urn ).toString();
      }

      final AspectModelUrn aspectModelUrn = AspectModelUrn.fromUrn( urn );
      return aspectModelUrn.getNamespace() + File.separator + aspectModelUrn.getVersion() + File.separator +
            aspectModelUrn.getName() + applicationSettings.getFileType();
   }

   @Override
   public void deleteModel( final @Nonnull String namespace, final String storagePath ) {
      final String filePath = LocalFolderResolverUtils.extractFilePath( namespace ).toString();
      final String qualifiedFilePath = getQualifiedFilePath( filePath, storagePath );
      final File file = getFileInstance( qualifiedFilePath );

      if ( !file.exists() ) {
         throw new FileNotFoundException( String.format( TTL_FILE_DOES_NOT_EXISTS, namespace ) );
      }

      deleteEmptyFiles( file );
   }

   @Override
   public Map<String, List<String>> getAllNamespaces( final boolean shouldRefresh, final String storagePath ) {
      if ( getNamespaces().isEmpty() || shouldRefresh ) {
         final Map<String, List<String>> newNamespaces = readAllNamespacesFromFolder( storagePath );
         setNamespaces( newNamespaces );
      }

      return getNamespaces().orElseThrow();
   }

   @Override
   public LocalPackageInfo getLocalPackageInformation( final String storagePath ) {
      final File file = getFileInstance( storagePath );

      if ( !file.exists() ) {
         throw new FileNotFoundException( String.format( TTL_FILE_DOES_NOT_EXISTS, storagePath ) );
      }

      return new LocalPackageInfo(
            getListOfLocalPackageInformation( getEndFilePaths( storagePath, file ), storagePath ),
            getNonTurtleFiles( storagePath, file ) );
   }

   @Override
   public AspectModelUrn convertFileToUrn( final File inputFile ) {
      final File versionDirectory = inputFile.getParentFile();
      final String version = versionDirectory.getName();
      final File namespaceDirectory = versionDirectory.getParentFile();
      final String namespace = namespaceDirectory.getName();
      final String aspectName = FilenameUtils.removeExtension( inputFile.getName() );
      final String urn = String.format( "urn:bamm:%s:%s#%s", namespace, version, aspectName );
      return AspectModelUrn.from( urn ).getOrElse( () -> {
         throw new InvalidAspectModelException(
               String.format( "The URN constructed from the input file path is invalid: %s", urn ) );
      } );
   }

   private synchronized Optional<Map<String, List<String>>> getNamespaces() {
      return namespaces;
   }

   private synchronized void setNamespaces( final Map<String, List<String>> namespaces ) {
      this.namespaces = Optional.of( namespaces );
   }

   /**
    * This method will read the folder structure from shared folder.
    *
    * @return a map of key = namespace + version and value = list of turtle files that are present in that namespace.
    */
   private Map<String, List<String>> readAllNamespacesFromFolder( final String storagePath ) {
      final String rootSharedFolder = getQualifiedFilePath( StringUtils.EMPTY, storagePath );
      final File file = getFileInstance( rootSharedFolder );

      if ( !file.exists() ) {
         throw new FileNotFoundException( String.format( TTL_FILE_DOES_NOT_EXISTS, rootSharedFolder ) );
      }

      final List<String> endFilePaths = getEndFilePaths( rootSharedFolder, file );

      final Map<String, List<String>> namespacePathMapping = endFilePaths
            .stream()
            .map( LocalFolderResolverStrategy::transformToValidModelDirectory )
            .collect( Collectors.groupingBy( this::extractNamespaceAndVersion, toList() ) );

      retainOnlyTurtleFileName( namespacePathMapping );

      return namespacePathMapping;
   }

   /**
    * From a list of all file paths, this method will return only those paths that are end folders or ttl folders.
    */
   private List<String> getEndFilePaths( @Nonnull final String rootSharedFolder, @Nonnull final File file ) {
      try ( final Stream<Path> paths = getAllSubFilePaths( file.toPath() ) ) {

         return paths.filter( this::isPathRelevant )
                     .map( Path::toString )
                     .map( path -> path.replace( rootSharedFolder, StringUtils.EMPTY ) )
                     .filter( this::isPathExcluded )
                     .toList();
      } catch ( final IOException e ) {
         throw new FileReadException( "Can not read shared folder file structure", e );
      }
   }

   /**
    * Returns a list of all non turtle files in the package.
    */
   private List<String> getNonTurtleFiles( @Nonnull final String rootSharedFolder, @Nonnull final File file ) {
      try ( final Stream<Path> paths = getAllSubFilePaths( file.toPath() ) ) {

         return paths.filter( path -> {
                        final File parentDir = getFileInstance( path.toString() );
                        return !parentDir.isDirectory() && !path.toString().endsWith( ModelUtils.TTL_EXTENSION );
                     } )
                     .map( Path::toString )
                     .map( path -> path.replace( rootSharedFolder, StringUtils.EMPTY ) )
                     .filter( this::isPathExcluded )
                     .collect( toList() );
      } catch ( final IOException e ) {
         throw new FileReadException( "Can not read shared folder file structure", e );
      }
   }

   /**
    * Method for excluding turtle files from user.
    * For example latest.ttl is used internally AME, and it should nt be modified or used by the user.
    *
    * @param path - folder location that will be analyzed.
    */
   private boolean isPathExcluded( @Nonnull final String path ) {
      return !path.endsWith( "latest.ttl" );
   }

   private List<ValidFile> getListOfLocalPackageInformation( final List<String> filePath, final String storagePath ) {
      return filePath
            .stream()
            .map( path -> {
               final String aspectModelFile = transformToValidModelDirectory( path );
               final String aspectModel = getModelAsString( aspectModelFile, storagePath );
               return new ValidFile( aspectModelFile, aspectModel );
            } )
            .toList();
   }

   /**
    * This method will transform the path in namespace:version:turtleFileName.
    * ex: io.openmanufacturing\1.0.0\AspectDefault.ttl - io.openmanufacturing:1.0.0:AspectDefault.ttl
    *
    * @param path - folder location that will be analyzed.
    */
   public static String transformToValidModelDirectory( @Nonnull final String path ) {
      String result = replaceLastFileSeparator( path );

      if ( path.endsWith( ModelUtils.TTL_EXTENSION ) ) {
         result = replaceLastFileSeparator( result );
      }

      return result.replace( File.separator, "" );
   }

   /**
    * Recreate list of values only with turtle file names if any.
    * ex: list value io.openmanufacturing:1.0.0:AspectDefault.ttl - AspectDefault.ttl
    */
   private void retainOnlyTurtleFileName( @Nonnull final Map<String, List<String>> pathTurtleFilesMap ) {
      for ( final Map.Entry<String, List<String>> entry : pathTurtleFilesMap.entrySet() ) {
         final List<String> collect = entry.getValue().stream()
                                           .filter( value -> !value.equals( entry.getKey() ) )
                                           .map( value -> value.replaceAll( entry.getKey(), StringUtils.EMPTY ) )
                                           .map( value -> value.replace(
                                                 LocalFolderResolverUtils.NAMESPACE_VERSION_NAME_SEPARATOR,
                                                 StringUtils.EMPTY ) )
                                           .toList();

         entry.setValue( collect );
      }
   }

   /**
    * This method will separate turtle file name and the rest of the path with ':'.
    * io\openmanufacturing\1.0.0\AspectDefaultTeast.ttl - io\openmanufacturing\1.0.0:AspectDefault.ttl
    *
    * @param path - folder location that will be analyzed.
    */
   private static String replaceLastFileSeparator( @Nonnull final String path ) {
      final int lastFileSeparatorIndex = path.lastIndexOf( File.separator );

      if ( lastFileSeparatorIndex != -1 ) {
         return path.substring( 0, lastFileSeparatorIndex ) + LocalFolderResolverUtils.NAMESPACE_VERSION_NAME_SEPARATOR
               + path.substring( lastFileSeparatorIndex + 1 );
      }

      return path;
   }

   /**
    * This method will extract namespace and version from the modelIdentifier.
    * ex: io.openmanufacturing:1.0.0:AspectDefault.ttl - io.openmanufacturing:1.0.0
    *
    * @param modelIdentifier - folder location that will be analyzed.
    */
   private String extractNamespaceAndVersion( @Nonnull final String modelIdentifier ) {
      if ( modelIdentifier.lastIndexOf( LocalFolderResolverUtils.NAMESPACE_VERSION_NAME_SEPARATOR ) == -1 ) {
         return StringUtils.EMPTY;
      }

      if ( modelIdentifier.endsWith( ModelUtils.TTL_EXTENSION ) ) {
         return modelIdentifier.substring( 0,
               modelIdentifier.lastIndexOf( LocalFolderResolverUtils.NAMESPACE_VERSION_NAME_SEPARATOR ) );
      }

      return modelIdentifier;
   }

   /**
    * This method will be used to identify the end folders (with no content in it) or the path which ends with a turtle
    * file.
    *
    * @param path - folder location that will be analyzed.
    */
   protected boolean isPathRelevant( @Nonnull final Path path ) {
      if ( path.toString().endsWith( ModelUtils.TTL_EXTENSION ) ) {
         return true;
      }
      final File parentDir = getFileInstance( path.toString() );
      return parentDir.isDirectory() && Objects.requireNonNull( parentDir.list() ).length == 0;
   }

   /**
    * Get file based on the file path.
    *
    * @param filePath - of the file.
    */
   protected File getFileInstance( @Nonnull final String filePath ) {
      final File storeFile = new File( filePath );
      final boolean isMkdir = storeFile.getParentFile().mkdirs();

      if ( isMkdir ) {
         LOG.info( "Folder successful created in {}", storeFile.getParentFile() );
      }

      return storeFile;
   }

   /**
    * Extract Aspect Model urn from turtle data.
    *
    * @param turtleData - file used to extract filePath.
    * @param storagePath - path to storage files.
    * @return Aspect Model urn
    */
   protected AspectModelUrn getAspectModelUrn( @Nonnull final String turtleData,
         final @Nonnull String storagePath ) {
      return inMemoryStrategy( turtleData, storagePath ).getAspectModelUrn();
   }

   /**
    * Extract file path from turtle file - namespace.
    * ex: @prefix : urn:bamm:io.openmanufacturing:1.0.0#.
    *
    * @param turtleData - file used to extract filePath.
    * @param storagePath - path to storage files.
    */
   protected String getFilePathBasedOnTurtleData( @Nonnull final String turtleData,
         final @Nonnull String storagePath ) {
      final AspectModelUrn aspectModelUrn = getAspectModelUrn( turtleData, storagePath );

      return aspectModelUrn.getNamespace() + File.separator + aspectModelUrn.getVersion() + File.separator +
            aspectModelUrn.getName();
   }

   /**
    * Get full file path based on the local file path + namespace.
    * ex: C:\Users\{myUser}\ame\models + \org\openmanufacturing
    *
    * @param namespace - namespace of the current ttl.
    * @param storagePath - path of the workspace storage.
    */
   protected String getQualifiedFilePath( final String namespace, final String storagePath ) {
      return storagePath + File.separator + namespace;
   }

   /**
    * Write the given content to the file.
    *
    * @param content - information that will be stored into the file.
    * @param storeFile - file path were the content will be saved.
    */
   protected void writeToFile( @Nonnull final String content, @Nonnull final File storeFile ) throws IOException {
      Files.write( storeFile.toPath(), content.getBytes() );
   }

   /**
    * Returns a stream of sub files based on a parent file path.
    *
    * @param parentPath - parent file path
    */
   protected Stream<Path> getAllSubFilePaths( @Nonnull final Path parentPath ) throws IOException {
      return Files.walk( parentPath );
   }

   /**
    * Reads the content of the given file and returns it as a string.
    *
    * @param storeFile - file content that will be read.
    */
   protected String getFileContent( @Nonnull final File storeFile ) {
      try {
         return new String( Files.readAllBytes( storeFile.toPath() ) );
      } catch ( final IOException e ) {
         throw new FileNotFoundException( String.format( "Cannot read file at the following path: %s",
               storeFile.toPath() ), e );
      }
   }

   /**
    * Force remove the given file
    *
    * @param file - that will be removed.
    * @throws FileNotFoundException will be thrown if file can not be found.
    */
   private void deleteFile( @Nonnull final File file ) {
      try {
         FileUtils.forceDeleteOnExit( file );
      } catch ( final IOException e ) {
         throw new FileNotFoundException( String.format( "File %s was not deleted successfully.", file.toPath() ),
               e );
      }
   }

   /**
    * Finds and deletes all the parent folders that are empty for the given file.
    *
    * @param file - that will be removed.
    */
   private void deleteEmptyFiles( @Nonnull final File file ) {
      if ( !applicationSettings.getEndFilePath().equals( file.getName() ) ) {
         final File parentFile = file.getParentFile();
         deleteFile( file );
         if ( Objects.requireNonNull( parentFile.listFiles() ).length == 0 ) {
            deleteEmptyFiles( parentFile );
         }
      }
   }
}
