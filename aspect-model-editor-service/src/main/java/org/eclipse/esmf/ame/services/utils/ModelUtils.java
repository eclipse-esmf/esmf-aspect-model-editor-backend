/*
 * Copyright (c) 2025 Robert Bosch Manufacturing Solutions GmbH
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

package org.eclipse.esmf.ame.services.utils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.StreamSupport;

import org.eclipse.esmf.ame.config.ApplicationSettings;
import org.eclipse.esmf.ame.exceptions.FileHandlingException;
import org.eclipse.esmf.ame.exceptions.FileReadException;
import org.eclipse.esmf.aspectmodel.loader.AspectModelLoader;
import org.eclipse.esmf.aspectmodel.urn.AspectModelUrn;
import org.eclipse.esmf.metamodel.AspectModel;

import jakarta.annotation.Nonnull;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for handling model-related file operations.
 */
public class ModelUtils {
   private static final Logger LOG = LoggerFactory.getLogger( ModelUtils.class );

   /**
    * Creates a ByteArrayInputStream from the given Turtle data string.
    *
    * @param turtleData the Turtle data as a string
    * @return a ByteArrayInputStream containing the Turtle data
    */
   public static ByteArrayInputStream createInputStream( final String turtleData ) {
      return new ByteArrayInputStream( turtleData.getBytes( StandardCharsets.UTF_8 ) );
   }

   /**
    * Finds and deletes all the parent folders that are empty for the given file.
    *
    * @param file - that will be removed.
    */
   public static void deleteEmptyFiles( @Nonnull final File file ) {
      if ( !ApplicationSettings.getEndFilePath().toFile().getName().equals( file.getName() ) ) {
         final File parentFile = file.getParentFile();
         deleteFileSafely( file );

         final List<File> fileList = Arrays.stream( Objects.requireNonNull( parentFile.listFiles() ) )
               .filter( f -> filterOutUnVisibleFiles().test( f ) ).toList();

         if ( fileList.isEmpty() ) {
            deleteEmptyFiles( parentFile );
         }
      }
   }

   /**
    * Safely deletes a file or directory. If the file is locked, it waits for the lock to be released
    * before attempting deletion. If the file is a directory, it deletes the directory and all its contents.
    *
    * @param file the file or directory to be deleted
    * @throws FileHandlingException if an I/O error occurs during deletion
    * @throws FileReadException if the thread is interrupted while waiting for the file lock to be released
    */
   public static void deleteFileSafely( @Nonnull final File file ) {
      try {
         if ( !file.exists() ) {
            return;
         }

         if ( !file.isDirectory() ) {
            FileUtils.deleteQuietly( file );
         } else {
            FileUtils.deleteDirectory( file );
         }
      } catch ( final IOException e ) {
         throw new FileHandlingException( "File could not be deleted: " + file.getAbsolutePath(), e );
      }
   }

   private static Predicate<File> filterOutUnVisibleFiles() {
      return file -> !file.getName().equals( ".DS_Store" );
   }

   /**
    * Creates a file based on the given AspectModelUrn.
    *
    * @param aspectModelUrn - The Aspect Model URN.
    * @param fileName - The name of the Aspect Model file.
    * @return The created file.
    * @throws IOException if an I/O error occurs.
    */
   public static File createFile( final AspectModelUrn aspectModelUrn, final String fileName, final Path storagePath ) throws IOException {
      final Path filePath = createFilePath( aspectModelUrn, fileName, storagePath );

      try {
         if ( Files.notExists( filePath.getParent() ) ) {
            Files.createDirectories( filePath.getParent() );
            LOG.info( "Directories created: {}", filePath.getParent() );
         }

         if ( Files.notExists( filePath ) ) {
            Files.createFile( filePath );
            LOG.info( "File created: {}", filePath );
         } else {
            LOG.info( "File already exists: {}", filePath );
         }
      } catch ( final IOException e ) {
         LOG.error( "Failed to create file: {}", filePath, e );
         throw e;
      }

      return filePath.toFile();
   }

   /**
    * Creates a file path based on the given AspectModelUrn, file name, and storage path.
    *
    * @param aspectModelUrn the Aspect Model URN
    * @param fileName the name of the Aspect Model file
    * @param storagePath the storage path where the file will be created
    * @return the created file path
    */
   public static Path createFilePath( final AspectModelUrn aspectModelUrn, final String fileName, final Path storagePath ) {
      return Paths.get( storagePath.toString(), aspectModelUrn.getNamespaceMainPart(), aspectModelUrn.getVersion(),
            ( fileName.isEmpty() ? aspectModelUrn.getName() + ".ttl" : fileName ) );
   }

   /**
    * Returns a Supplier for loading an AspectModel based on the given AspectModelUrn.
    *
    * @param aspectModelUrn the Aspect Model URN
    * @param aspectModelLoader the loader to load the Aspect Model
    * @return a Supplier for the Aspect Model
    */
   public static Supplier<AspectModel> getAspectModelSupplier( final AspectModelUrn aspectModelUrn,
         final AspectModelLoader aspectModelLoader ) {
      return createLazySupplier( () -> aspectModelLoader.load( aspectModelUrn ) );
   }

   /**
    * Returns a Supplier for loading an AspectModel based on the given Turtle data and file.
    *
    * @param turtleData the Turtle data as a string
    * @param newFile the file to be created
    * @param aspectModelLoader the loader to load the Aspect Model
    * @return a Supplier for the Aspect Model
    */
   public static Supplier<AspectModel> getAspectModelSupplier( final String turtleData, final File newFile,
         final AspectModelLoader aspectModelLoader ) {
      final Optional<URI> sourceLocation = Optional.of( newFile.toURI() );
      final ByteArrayInputStream inputStream = new ByteArrayInputStream( turtleData.getBytes( StandardCharsets.UTF_8 ) );

      return createLazySupplier( () -> aspectModelLoader.load( inputStream, sourceLocation ) );
   }

   private static Supplier<AspectModel> createLazySupplier( final Supplier<AspectModel> loader ) {
      return new Supplier<>() {
         private AspectModel aspectModel;
         private boolean isLoaded = false;

         @Override
         public AspectModel get() {
            if ( !isLoaded ) {
               aspectModel = loader.get();
               isLoaded = true;
            }
            return aspectModel;
         }
      };
   }

   /**
    * Resolves a file path from a given URN and base model path.
    * <p>
    * The URN is expected to follow the format:
    * <pre>urn:namespace:version#fileName</pre>
    * This method extracts the namespace, version, and file name from the URN
    * and constructs a path relative to the provided model path.
    *
    * @param urn the URN to be resolved, in the format `urn:namespace:version#fileName`
    * @param modelPath the base path where the resolved file path will be constructed
    * @return the resolved file path as a {@link Path}
    * @throws IllegalArgumentException if the URN format is invalid
    */
   public static Path resolvePathFromUrn( final String urn, final Path modelPath ) {
      if ( urn == null || !urn.contains( "#" ) || !urn.contains( ":" ) ) {
         throw new IllegalArgumentException( "Invalid URN format. Expected format: urn:namespace:version#fileName" );
      }

      final String[] parts = urn.split( "#", 2 );
      final String fileName = parts[1];
      final String[] nsAndVer = parts[0].split( ":" );

      if ( nsAndVer.length < 3 ) {
         throw new IllegalArgumentException( "Invalid URN format. Namespace and version are missing." );
      }

      final String version = nsAndVer[nsAndVer.length - 1];
      final String namespace = nsAndVer[nsAndVer.length - 2];

      return modelPath.resolve( namespace ).resolve( version ).resolve( fileName + ".ttl" );
   }

   public static AspectModel loadModelFromFile( final Path modelPath, final String filePath, final AspectModelLoader aspectModelLoader ) {
      final Path path = Paths.get( filePath ).normalize();
      final String[] pathParts = StreamSupport.stream( path.spliterator(), false ).map( Path::toString ).toArray( String[]::new );
      final Path aspectModelPath = constructModelPath( modelPath, pathParts[0], pathParts[1], pathParts[2] );
      return aspectModelLoader.load( aspectModelPath.toFile() );
   }

   public static Path constructModelPath( final Path modelPath, final String namespace, final String version, final String modelName ) {
      return Path.of( modelPath.toString(), namespace, version, modelName );
   }
}
