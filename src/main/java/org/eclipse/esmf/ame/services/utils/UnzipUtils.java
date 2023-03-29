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

package org.eclipse.esmf.ame.services.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.esmf.ame.exceptions.CreateFileException;
import org.eclipse.esmf.ame.exceptions.FileNotFoundException;
import org.eclipse.esmf.ame.exceptions.FileReadException;
import org.eclipse.esmf.ame.exceptions.FileWriteException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnzipUtils {
   private static final Logger LOG = LoggerFactory.getLogger( UnzipUtils.class );

   private UnzipUtils() {

   }

   /**
    * This Method is used to unzip a zip package with aspect models.
    *
    * @param zipFile - The zip file as InputStream
    * @param packagePath - The default package storage folder path
    */
   @SuppressWarnings( { "squid:S135", "squid:S5042" } )
   public static void unzipPackageFile( final InputStream zipFile, final Path packagePath ) {
      try ( final ZipInputStream zipInputStream = new ZipInputStream( zipFile ) ) {

         ZipEntry zipEntry = zipInputStream.getNextEntry();

         while ( zipEntry != null ) {

            // Skip Mac entries
            if ( !zipEntry.getName().contains( ".DS_Store" ) && !zipEntry.getName().contains( "__MACOSX" ) ) {

               final File createdFile = UnzipUtils.createNewFile( packagePath.toFile(), zipEntry );

               if ( zipEntry.isDirectory() ) {
                  UnzipUtils.createNewDirectory( createdFile );
               } else {

                  if ( createdFile.getName().endsWith( ".ttl" ) && containsSpecialChar( createdFile.getName() ) ) {
                     throw new CreateFileException(
                           "The file name contains special characters. Please ensure that the file name does not contain any special characters and try again. (Will be supported soon)";
                  }

                  // To create directory for Windows
                  UnzipUtils.createNewDirectory( createdFile.getParentFile() );
                  // create aspect model file content and close output stream
                  UnzipUtils.createNewAspectModelFileWithContent( createdFile, zipInputStream ).close();
               }
            }

            zipEntry = zipInputStream.getNextEntry();
         }
         zipInputStream.closeEntry();

         IOUtils.closeQuietly( zipInputStream );
      } catch ( final IOException e ) {
         LOG.error( "Cannot read file." );
         throw new FileReadException( "Error reading the zip file.", e );
      }
   }

   /**
    * This Method creates a new file.
    *
    * @param destinationDir - destination directory of the file.
    * @param zipEntry - representation of the zip file.
    * @return the new created file.
    */
   private static File createNewFile( final File destinationDir, final ZipEntry zipEntry ) throws IOException {
      final String zipEntryPathName = FilenameUtils.separatorsToSystem( zipEntry.getName() );
      final File destFile = new File( destinationDir, zipEntryPathName );

      final String destDirPath = destinationDir.getCanonicalPath();
      final String destFilePath = destFile.getCanonicalPath();

      if ( !destFilePath.startsWith( destDirPath + File.separator ) ) {
         LOG.error( "Entry is outside of the target directory." );
         throw new FileNotFoundException( "Entry is outside of the target dir: " + zipEntryPathName );
      }

      return destFile;
   }

   /**
    * This Method checks if the file name contains special characters.
    *
    * @param str - file name.
    * @return true if the file name contains special characters.
    */
   private static boolean containsSpecialChar( final String str ) {
      final String regex = "[^a-zA-Z0-9.]";
      final Pattern pattern = Pattern.compile( regex );
      final Matcher matcher = pattern.matcher( str );
      return matcher.find();
   }

   /**
    * This Method creates a directory.
    *
    * @param file - to create.
    */
   private static void createNewDirectory( final File file ) {
      if ( !file.isDirectory() && !file.mkdirs() ) {
         LOG.error( "Cannot create directory." );
         throw new CreateFileException( "Failed to create directory " + file );
      }
   }

   /**
    * This Method creates a new aspect model file with there specific content.
    *
    * @param file - new created file to fill with there content.
    * @param zis - read files from zip file.
    * @return the output stream of the file.
    */
   private static FileOutputStream createNewAspectModelFileWithContent( final File file, final ZipInputStream zis )
         throws IOException {
      final byte[] buffer = new byte[1024];
      try ( final FileOutputStream fileOutputStream = new FileOutputStream( file ) ) {
         int length;
         while ( (length = zis.read( buffer )) > 0 ) {
            fileOutputStream.write( buffer, 0, length );
         }
         return fileOutputStream;
      } catch ( final IOException e ) {
         LOG.error( "File to write was not found." );
         throw new FileWriteException( "File for writing not found", e );
      }
   }
}
