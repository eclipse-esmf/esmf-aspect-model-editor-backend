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
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.StreamSupport;

import org.eclipse.esmf.ame.exceptions.CreateFileException;
import org.eclipse.esmf.ame.exceptions.FileHandlingException;
import org.eclipse.esmf.ame.exceptions.FileReadException;
import org.eclipse.esmf.aspectmodel.AspectLoadingException;
import org.eclipse.esmf.aspectmodel.AspectModelFile;
import org.eclipse.esmf.aspectmodel.loader.AspectModelLoader;
import org.eclipse.esmf.aspectmodel.resolver.exceptions.ModelResolutionException;
import org.eclipse.esmf.aspectmodel.shacl.violation.Violation;
import org.eclipse.esmf.aspectmodel.urn.AspectModelUrn;
import org.eclipse.esmf.metamodel.AspectModel;
import org.eclipse.esmf.metamodel.ModelElement;

import io.micronaut.http.multipart.CompletedFileUpload;
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
    * Finds and deletes all the parent folders that are empty for the given file.
    *
    * @param aspectModelFile - that will be removed.
    */
   public static void deleteEmptyFiles( @Nonnull final AspectModelFile aspectModelFile ) {
      aspectModelFile.sourceLocation().ifPresent( uri -> deleteFileSafely( new File( uri ) ) );
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

   /**
    * Creates a new file for the given AspectModelUrn and file name in the specified storage path.
    * If necessary, parent directories are created.
    *
    * @param aspectModelUrn the Aspect Model URN
    * @param fileName the name of the file to create
    * @param storagePath the base storage path
    * @throws IOException if an I/O error occurs during creation
    */
   public static void createFile( final AspectModelUrn aspectModelUrn, final String fileName, final Path storagePath ) throws IOException {
      final Path filePath = createFilePath( aspectModelUrn, fileName, storagePath );
      createFile( filePath );
   }

   /**
    * Creates a file at the specified path, including any necessary parent directories.
    * Logs the creation process.
    *
    * @param filePath the path of the file to create
    * @throws IOException if an I/O error occurs during creation
    */
   public static void createFile( final Path filePath ) throws IOException {
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
    * Opens an InputStream from the given CompletedFileUpload.
    * <p>
    * Note: The InputStream is closed immediately due to the try-with-resources block,
    * so the returned InputStream will not be usable. Consider refactoring this method.
    *
    * @param aspectModel the uploaded file
    * @return the InputStream of the uploaded file
    * @throws FileReadException if an I/O error occurs while reading the file
    */
   public static InputStream openInputStreamFromUpload( final CompletedFileUpload aspectModel ) {
      try {
         return aspectModel.getInputStream();
      } catch ( final IOException e ) {
         throw new FileReadException( "Failed to read uploaded file" );
      }
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
      return createLazySupplier( () -> {
         try ( final ByteArrayInputStream inputStream = new ByteArrayInputStream( turtleData.getBytes( StandardCharsets.UTF_8 ) ) ) {
            final AspectModel aspectModel = aspectModelLoader.load( inputStream, newFile.toURI() );
            checkForDuplicateFiles( aspectModel, newFile.getName() );
            return aspectModel;
         } catch ( final IOException e ) {
            throw new CreateFileException( "Failed to process Turtle data", e );
         }
      } );
   }

   private static void checkForDuplicateFiles( final AspectModel aspectModel,
         final String sourceFilename ) {

      final boolean hasDifferentFile = aspectModel.elements().stream()
            .filter( modelElement -> modelElement.getSourceFile().filename().orElse( "" ).equals( sourceFilename ) )
            .anyMatch( modelElement -> hasDifferentFileForElement( modelElement, sourceFilename ) );

      if ( hasDifferentFile ) {
         LOG.warn( "Some elements are already defined in the same namespace" );
         throw new CreateFileException( "Some elements are already defined in the same namespace" );
      }
   }

   private static boolean hasDifferentFileForElement( final ModelElement modelElement, final String sourceFilename ) {
      try {
         final String modelSourceFileName = modelElement.getSourceFile().filename().orElse( "" );
         return !modelSourceFileName.equals( sourceFilename );
      } catch ( final ModelResolutionException | AspectLoadingException ex ) {
         return false;
      }
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
    * Loads an {@link AspectModel} from a file path by constructing the full model path.
    *
    * @param modelPath the base path to the model storage directory
    * @param filePath the relative file path (namespace/version/modelName)
    * @param aspectModelLoader the loader to load the {@link AspectModel}
    * @return the loaded {@link AspectModel}
    */
   public static AspectModel loadModelFromFile( final Path modelPath, final String filePath, final AspectModelLoader aspectModelLoader ) {
      final Path path = Paths.get( filePath ).normalize();
      final String[] pathParts = StreamSupport.stream( path.spliterator(), false ).map( Path::toString ).toArray( String[]::new );
      final Path aspectModelPath = constructModelPath( modelPath, pathParts[0], pathParts[1], pathParts[2] );
      return aspectModelLoader.load( aspectModelPath.toFile() );
   }

   /**
    * Constructs a model file path from the given components.
    *
    * @param modelPath the base path to the model storage directory
    * @param namespace the namespace part of the model
    * @param version the version part of the model
    * @param modelName the name of the model file
    * @return the constructed {@link Path}
    */
   public static Path constructModelPath( final Path modelPath, final String namespace, final String version, final String modelName ) {
      return Path.of( modelPath.toString(), namespace, version, modelName );
   }

   /**
    * Throws the given exception if any violation in the list matches the predicate.
    *
    * @param violations the list of {@link Violation} objects to check
    * @param predicate the predicate to filter violations
    * @param exception the exception to throw if a matching violation is found
    */
   public static void throwIfViolationPresent( final List<Violation> violations, final Predicate<Violation> predicate,
         final RuntimeException exception ) {
      violations.stream().filter( predicate ).findFirst().ifPresent( v -> {throw exception;} );
   }
}
