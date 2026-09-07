/*
 * Copyright (c) 2026 Robert Bosch Manufacturing Solutions GmbH
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

package org.eclipse.esmf.ame.services.file;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.eclipse.esmf.ame.exceptions.FileHandlingException;
import org.eclipse.esmf.aspectmodel.AspectModelFile;

import jakarta.annotation.Nonnull;
import jakarta.inject.Singleton;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service responsible for file system operations related to aspect models.
 * Handles file creation, deletion, and directory management.
 */
@Singleton
public class FileOperations {
   private static final Logger LOG = LoggerFactory.getLogger( FileOperations.class );

   /**
    * Deletes the file associated with the given AspectModelFile if it exists.
    *
    * @param aspectModelFile the aspect model file whose source location should be deleted
    */
   public void deleteAspectModelFile( @Nonnull final AspectModelFile aspectModelFile ) {
      final URI uri = aspectModelFile.sourceLocation()
            .or( () -> Optional.ofNullable( aspectModelFile.sourceUri() ) )
            .orElse( null );
      if ( uri != null ) {
         final Path path = uriToPath( uri );
         deleteFileSafely( path );
      }
   }

   private Path uriToPath( final URI uri ) {
      try {
         if ( uri.getScheme() != null && uri.getScheme().equalsIgnoreCase( "file" ) ) {
            return Path.of( uri );
         }
      } catch ( final Exception ignored ) {
      }
      final String pathStr = uri.getPath() != null ? uri.getPath() : uri.getSchemeSpecificPart();
      return Path.of( pathStr );
   }

   /**
    * Safely deletes a file or directory.
    * If the file is a directory, it deletes the directory and all its contents.
    *
    * @param filePath the file or directory path to be deleted
    * @throws FileHandlingException if an I/O error occurs during deletion
    */
   public void deleteFileSafely( @Nonnull final Path filePath ) {
      try {
         if ( !Files.exists( filePath ) ) {
            LOG.debug( "File does not exist, skipping deletion: {}", filePath );
            return;
         }

         if ( Files.isDirectory( filePath ) ) {
            FileUtils.deleteDirectory( filePath.toFile() );
            LOG.info( "Directory deleted: {}", filePath );
         } else {
            boolean deleted = false;
            try {
               deleted = Files.deleteIfExists( filePath );
            } catch ( final IOException e ) {
               // On Windows, open handles held by loader/JVM can block deletion until GC runs
               System.gc();
               try {
                  Thread.sleep( 30 );
               } catch ( final InterruptedException ie ) {
                  Thread.currentThread().interrupt();
               }
               deleted = Files.deleteIfExists( filePath );
            }

            if ( !deleted && Files.exists( filePath ) ) {
               System.gc();
               try {
                  Thread.sleep( 30 );
               } catch ( final InterruptedException ie ) {
                  Thread.currentThread().interrupt();
               }
               if ( !filePath.toFile().delete() && Files.exists( filePath ) ) {
                  throw new IOException( "Failed to delete file: " + filePath );
               }
            }
            LOG.info( "File deleted: {}", filePath );
         }
      } catch ( final IOException e ) {
         throw new FileHandlingException( "File could not be deleted: " + filePath, e );
      }
   }

   /**
    * Creates a file at the specified path, including any necessary parent directories.
    *
    * @param filePath the path of the file to create
    * @throws IOException if an I/O error occurs during creation
    */
   public void createFile( final Path filePath ) throws IOException {
      try {
         if ( Files.notExists( filePath.getParent() ) ) {
            Files.createDirectories( filePath.getParent() );
            LOG.info( "Directories created: {}", filePath.getParent() );
         }
         if ( Files.notExists( filePath ) ) {
            Files.createFile( filePath );
            LOG.info( "File created: {}", filePath );
         } else {
            LOG.debug( "File already exists: {}", filePath );
         }
      } catch ( final IOException e ) {
         LOG.error( "Failed to create file: {}", filePath, e );
         throw e;
      }
   }

   /**
    * Checks if a file exists at the given path.
    *
    * @param filePath the file path to check
    * @return true if the file exists, false otherwise
    */
   public boolean exists( final Path filePath ) {
      return Files.exists( filePath );
   }
}

