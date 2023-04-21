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

package org.eclipse.esmf.ame.repository.strategy;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.esmf.ame.config.ApplicationSettings;
import org.eclipse.esmf.ame.exceptions.FileNotFoundException;
import org.eclipse.esmf.ame.exceptions.FileReadException;
import org.eclipse.esmf.ame.exceptions.FileWriteException;
import org.eclipse.esmf.ame.model.ValidationProcess;
import org.eclipse.esmf.ame.model.repository.LocalPackageInfo;
import org.eclipse.esmf.ame.model.repository.ValidFile;
import org.eclipse.esmf.ame.repository.strategy.utils.LocalFolderResolverUtils;
import org.eclipse.esmf.ame.services.utils.ModelUtils;
import org.eclipse.esmf.aspectmodel.resolver.exceptions.InvalidNamespaceException;
import org.eclipse.esmf.aspectmodel.resolver.services.ExtendedXsdDataType;
import org.eclipse.esmf.aspectmodel.urn.AspectModelUrn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import lombok.NonNull;

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
   public String getModelAsString( final @Nonnull String namespace, final @Nonnull String filename,
         final String storagePath ) {
      return getFileContent( getModelAsFile( namespace, filename, storagePath ) );
   }

   @Override
   public File getModelAsFile( final @Nonnull String namespace, final @Nonnull String fileName,
         final String storagePath ) {

      final String filePath = isLatest( fileName ) ?
            fileName :
            LocalFolderResolverUtils.buildFilePath( namespace, fileName );
      final String qualifiedFilePath = getQualifiedFilePath( filePath, storagePath );
      final File storeFile = getFileInstance( qualifiedFilePath );

      if ( !storeFile.exists() ) {
         throw new FileNotFoundException( String.format( TTL_FILE_DOES_NOT_EXISTS, namespace ) );
      }

      return storeFile;
   }

   private boolean isLatest( final String fileName ) {
      return "latest.ttl".equals( fileName );
   }

   @Override
   public String saveModel( final Optional<String> namespace, final Optional<String> fileName,
         final @Nonnull String turtleData, final String storagePath ) {
      try {
         ExtendedXsdDataType.setChecking( false );

         final String name = fileName.orElse( StringUtils.EMPTY );
         final String space = namespace.orElse( StringUtils.EMPTY );

         final String filePath = isLatest( name ) ? name : getFilePath( space, name, turtleData, storagePath );
         final File storeFile = getFileInstance( getQualifiedFilePath( filePath, storagePath ) );

         writeToFile( turtleData, storeFile );

         ExtendedXsdDataType.setChecking( true );
         return filePath;
      } catch ( final IOException e ) {
         throw new FileWriteException( "File cannot be written", e );
      }
   }

   /**
    * Deletes a directory and all its contents.
    *
    * @param storagePath - path to storage.
    */
   @Override
   public void deleteDirectory( @NonNull final File storagePath ) {
      LocalFolderResolverUtils.deleteDirectory( storagePath );
   }

   private String getFilePath( final String namespace, final String fileName, final String turtleData,
         final String storagePath ) {

      if ( namespace.contains( applicationSettings.getFileType() ) ) {
         throw new InvalidNamespaceException( "Namespace does not contain filename" );
      }

      if ( namespace.isEmpty() ) {
         return getFilePathBasedOnTurtleData( turtleData, storagePath ) + applicationSettings.getFileType();
      }

      if ( isLatest( fileName ) ) {
         return fileName;
      }

      final String[] splitedUrn = namespace.replace( "urn:samm:", "" ).replace( "#", "" ).split( ":" );
      return splitedUrn[0] + File.separator + splitedUrn[1] + File.separator + fileName;
   }

   @Override
   public void deleteModel( final @Nonnull String namespace, final @Nonnull String fileName,
         final String storagePath ) {
      final String filePath = LocalFolderResolverUtils.buildFilePath( namespace, fileName );
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
      final String urn = String.format( "urn:samm:%s:%s#%s", namespace, version, aspectName );
      return LocalFolderResolverUtils.convertToAspectModelUrn( urn );
   }

   @Override
   public AspectModelUrn convertAspectModelFileNameToUrn( final String aspectFileName ) {
      final String urn = replaceLastColon( aspectFileName ).replace( ".ttl", "" );
      return LocalFolderResolverUtils.convertToAspectModelUrn( String.format( "urn:samm:%s", urn ) );
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

      final Map<String, List<String>> namespacePathMapping = endFilePaths.stream()
                                                                         .map( LocalFolderResolverStrategy::transformToValidModelDirectory )
                                                                         .map( s -> s.split( ":" ) ).collect(
                  Collectors.groupingBy( arr -> arr[0] + ":" + arr[1],
                        Collectors.mapping( arr -> arr[2], Collectors.toList() ) ) );

      retainOnlyTurtleFileName( namespacePathMapping );

      return namespacePathMapping;
   }

   /**
    * From a list of all file paths, this method will return only those paths that are end folders or ttl folders.
    */
   private List<String> getEndFilePaths( @Nonnull final String rootSharedFolder, @Nonnull final File file ) {
      try ( final Stream<Path> paths = getAllSubFilePaths( file.toPath() ) ) {

         return paths.filter( this::isPathRelevant ).map( Path::toString )
                     .map( path -> excludeStandaloneFiles( rootSharedFolder, path ) ).filter( StringUtils::isNotBlank )
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
                     } ).map( Path::toString ).map( path -> excludeStandaloneFiles( rootSharedFolder, path ) )
                     .filter( StringUtils::isNotBlank ).toList();
      } catch ( final IOException e ) {
         throw new FileReadException( "Can not read shared folder file structure", e );
      }
   }

   /**
    * Method for excluding standalone files without folder structure from user.
    * For example latest.ttl is used internally AME, and it should not be modified or used by the user.
    * In addition, aspect models should be available in their folder structure.
    *
    * @param rootSharedFolder - absolute path of the file.
    * @param pathAsString - folder location that will be analyzed.
    */
   private String excludeStandaloneFiles( final String rootSharedFolder, final String pathAsString ) {
      final String relativePath = pathAsString.replace( rootSharedFolder, StringUtils.EMPTY );
      final Path path = Path.of( relativePath );

      if ( path.getParent() == null || path.getParent().getParent() == null || path.endsWith( "latest.ttl" ) ) {
         return StringUtils.EMPTY;
      }

      return relativePath;
   }

   private List<ValidFile> getListOfLocalPackageInformation( final List<String> filePath, final String storagePath ) {
      return filePath.stream().map( path -> {
         final String[] arg = transformToValidModelDirectory( path ).split( ":" );
         final String namespace = arg[0] + ":" + arg[1];
         final String fileName = arg[2];
         return new ValidFile( namespace, fileName, getModelAsString( namespace, fileName, storagePath ) );
      } ).toList();
   }

   /**
    * This method will transform the path in namespace:version:turtleFileName.
    * ex: org.eclipse.esmf\1.0.0\AspectDefault.ttl - org.eclipse.esmf.samm:1.0.0:AspectDefault.ttl
    *
    * @param path - folder location that will be analyzed.
    * @return transformed path.
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
    * ex: list value org.eclipse.esmf.samm:1.0.0:AspectDefault.ttl - AspectDefault.ttl
    */
   private void retainOnlyTurtleFileName( @Nonnull final Map<String, List<String>> pathTurtleFilesMap ) {
      for ( final Map.Entry<String, List<String>> entry : pathTurtleFilesMap.entrySet() ) {
         final List<String> collect = entry.getValue().stream().filter( value -> !value.equals( entry.getKey() ) )
                                           .map( value -> value.replaceAll( entry.getKey(), StringUtils.EMPTY ) )
                                           .map( value -> value.replace(
                                                 LocalFolderResolverUtils.NAMESPACE_VERSION_NAME_SEPARATOR,
                                                 StringUtils.EMPTY ) ).toList();

         entry.setValue( collect );
      }
   }

   /**
    * This method will separate turtle file name and the rest of the path with ':'.
    * org\eclipse\esmf\1.0.0\AspectDefaultTeast.ttl - org\eclipse\esmf\1.0.0:AspectDefault.ttl
    *
    * @param path - folder location that will be analyzed.
    * @return folder location with replaced file separator with ':'.
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
    * This method will replace the last ':' with '#'. This is needed because ':' is not allowed in the namespace.
    * org.eclipse.esmf.samm:1.0.0:AspectDefaultTeast.ttl - org.eclipse.esmf.1.0.0#AspectDefault.ttl
    *
    * @param aspectFileName - filename of the Aspect Model.
    * @return filename of the Aspect Model with replaced ':' with '#'.
    */
   private static String replaceLastColon( @Nonnull final String aspectFileName ) {
      final int lastStringIndex = aspectFileName.lastIndexOf( ":" );

      if ( lastStringIndex != -1 ) {
         return aspectFileName.substring( 0, lastStringIndex ) + "#" + aspectFileName.substring( lastStringIndex + 1 );
      }

      return aspectFileName;
   }

   /**
    * This method will extract namespace and version from the modelIdentifier.
    * ex: org.eclipse.esmf.samm:1.0.0:AspectDefault.ttl - org.eclipse.esmf.samm:1.0.0
    *
    * @param modelIdentifier - folder location that will be analyzed.
    * @return namespace and version.
    */
   //   private String extractNamespaceAndVersion( @Nonnull final String modelIdentifier ) {
   //      if ( modelIdentifier.lastIndexOf( LocalFolderResolverUtils.NAMESPACE_VERSION_NAME_SEPARATOR ) == -1 ) {
   //         return StringUtils.EMPTY;
   //      }
   //
   //      if ( modelIdentifier.endsWith( ModelUtils.TTL_EXTENSION ) ) {
   //         return modelIdentifier.substring( 0,
   //               modelIdentifier.lastIndexOf( LocalFolderResolverUtils.NAMESPACE_VERSION_NAME_SEPARATOR ) );
   //      }
   //
   //      return modelIdentifier;
   //   }

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
   protected AspectModelUrn getAspectModelUrn( @Nonnull final String turtleData, final @Nonnull String storagePath ) {
      return ModelUtils.inMemoryStrategy( turtleData, ValidationProcess.getEnum( storagePath ) ).getAspectModelUrn();
   }

   /**
    * Extract file path from turtle file - namespace.
    * ex: @prefix : urn:samm:org.eclipse.esmf.samm:1.0.0#.
    *
    * @param turtleData - file used to extract filePath.
    * @param storagePath - path to storage files.
    */
   protected String getFilePathBasedOnTurtleData( @Nonnull final String turtleData,
         final @Nonnull String storagePath ) {
      final AspectModelUrn aspectModelUrn = getAspectModelUrn( turtleData, storagePath );

      return aspectModelUrn.getNamespace() + File.separator + aspectModelUrn.getVersion() + File.separator
            + aspectModelUrn.getName();
   }

   /**
    * Get full file path based on the local file path + namespace.
    * ex: C:\Users\{myUser}\ame\models + \org\eclipse\esmf
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
         throw new FileNotFoundException(
               String.format( "Cannot read file at the following path: %s", storeFile.toPath() ), e );
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
         if ( !file.isDirectory() ) {
            file.createNewFile();
            final FileChannel channel = FileChannel.open( file.toPath(), StandardOpenOption.WRITE );
            channel.lock().release();
            channel.close();
         }
         FileUtils.forceDelete( file );
      } catch ( final IOException e ) {
         throw new FileNotFoundException( String.format( "File %s was not deleted successfully.", file.toPath() ), e );
      }
   }

   /**
    * Finds and deletes all the parent folders that are empty for the given file.
    *
    * @param file - that will be removed.
    */
   private void deleteEmptyFiles( @Nonnull final File file ) {
      if ( !applicationSettings.getEndFilePath().toFile().getName().equals( file.getName() ) ) {
         final File parentFile = file.getParentFile();
         deleteFile( file );

         final List<File> fileList = Arrays.stream( Objects.requireNonNull( parentFile.listFiles() ) )
                                           .filter( f -> filterOutUnVisibleFiles().test( f ) ).toList();

         if ( fileList.isEmpty() ) {
            deleteEmptyFiles( parentFile );
         }
      }
   }

   private Predicate<File> filterOutUnVisibleFiles() {
      return file -> !file.getName().equals( ".DS_Store" );
   }
}
