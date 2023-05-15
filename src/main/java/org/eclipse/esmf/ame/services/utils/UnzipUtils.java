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

package org.eclipse.esmf.ame.services.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.esmf.ame.exceptions.FileReadException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnzipUtils {
   private static final Logger LOG = LoggerFactory.getLogger( UnzipUtils.class );

   private UnzipUtils() {}

   /**
    * Extracts files from a ZIP input stream, skipping Mac-specific entries and performs the necessary file operations.
    *
    * @param zipFile          - The ZIP input stream.
    * @param importFileSystem - The in-memory import file system
    * @throws FileReadException If there's an error reading the ZIP file.
    */
   public static void extractFilesFromPackage( final InputStream zipFile, final FileSystem importFileSystem ) {
      try ( ZipInputStream zipInputStream = new ZipInputStream( zipFile ) ) {
         ZipEntry zipEntry;

         while ( (zipEntry = zipInputStream.getNextEntry()) != null ) {
            String zipEntryName = zipEntry.getName();

            if ( !isMacEntry( zipEntryName ) ) {
               String normalizedPath = FilenameUtils.separatorsToSystem( zipEntryName );
               String[] pathParts = splitPath( normalizedPath );
               Path filePath = importFileSystem.getPath( normalizedPath );

               if ( isNestedFile( pathParts ) ) {
                  createAndCopyFile( zipInputStream, filePath );
               } else {
                  Files.createDirectory( filePath );
               }
            }

            zipInputStream.closeEntry();
         }
      } catch ( IOException e ) {
         LOG.error( "Package cannot be imported." );
         throw new FileReadException( "Package cannot be imported.", e );
      }
   }

   /**
    * Checks if the given ZIP entry name corresponds to a Mac-specific entry.
    *
    * @param zipEntryName - The name of the ZIP entry.
    * @return {@code true} if the ZIP entry is a Mac-specific entry, {@code false} otherwise.
    */
   private static boolean isMacEntry( String zipEntryName ) {
      return zipEntryName.contains( ".DS_Store" ) || zipEntryName.contains( "__MACOSX" );
   }

   /**
    * Splits the path into individual parts based on the file separator of the current system.
    *
    * @param path - The path to split.
    * @return An array of path parts.
    */
   private static String[] splitPath( String path ) {
      String fileSeparator = System.getProperty( "os.name" ).toLowerCase().contains( "win" ) ? "\\\\" : File.separator;
      return path.split( fileSeparator );
   }

   /**
    * Checks if the given path corresponds to a nested file based on the number of path parts.
    *
    * @param pathParts - The individual parts of the path.
    * @return {@code true} if the path corresponds to a nested file, {@code false} otherwise.
    */
   private static boolean isNestedFile( String[] pathParts ) {
      return pathParts.length > 2 && isTTLFile( pathParts[pathParts.length - 1] );
   }

   /**
    * Checks if the given file name represents a TTL file.
    *
    * @param fileName - The file name to check.
    * @return {@code true} if the file name represents a TTL file, {@code false} otherwise.
    */
   private static boolean isTTLFile( String fileName ) {
      return fileName.endsWith( ".ttl" );
   }

   /**
    * Creates the file and copies its contents from the ZIP input stream.
    *
    * @param zipInputStream - The ZIP input stream.
    * @param filePath       - The path of the file to create.
    * @throws IOException - If there's an I/O error during file creation or copying.
    */
   private static void createAndCopyFile( ZipInputStream zipInputStream, Path filePath ) throws IOException {
      if ( isTTLFile( filePath.getFileName().toString() ) ) {
         Files.createFile( filePath );
         Files.copy( zipInputStream, filePath, StandardCopyOption.REPLACE_EXISTING );
      }
   }
}
