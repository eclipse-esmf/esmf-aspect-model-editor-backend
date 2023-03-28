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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.esmf.ame.exceptions.CreateFileException;
import org.eclipse.esmf.ame.exceptions.FileWriteException;

public class ZipUtils {
   private static final Logger LOG = LoggerFactory.getLogger( ZipUtils.class );

   private ZipUtils() {
   }

   static final int BUFFER = 1024;

   public static byte[] createZipFile( final String zipFileName, final String sourceStoragePath ) throws IOException {
      final String zipFile = sourceStoragePath + File.separator + zipFileName;

      try ( final FileOutputStream fos = new FileOutputStream( zipFile );
            final ZipOutputStream zos = new ZipOutputStream( fos ) ) {

         final List<File> fileList = getFileList( new File( sourceStoragePath ), new ArrayList<>(), sourceStoragePath );

         for ( final File file : fileList ) {
            final String fileName = file.isDirectory() ?
                  getFileName( file.toString(), sourceStoragePath ) + File.separator :
                  getFileName( file.toString(), sourceStoragePath );
            final BasicFileAttributes attr = Files.readAttributes( file.toPath(), BasicFileAttributes.class );

            final ZipEntry zipEntry = new ZipEntry( fileName );
            zipEntry.setLastModifiedTime( attr.lastModifiedTime() );
            zipEntry.setCreationTime( attr.creationTime() );
            zipEntry.setLastAccessTime( attr.lastAccessTime() );
            zipEntry.setTime( attr.lastModifiedTime().toMillis() );

            zos.putNextEntry( zipEntry );

            if ( !file.isDirectory() ) {
               createNewAspectModelFileWithContent( file, zos );
            }

            zos.closeEntry();
         }
      } catch ( final IOException e ) {
         LOG.error( "Cannot create zip file." );
         throw new CreateFileException( "Error creating the zip file.", e );
      }

      return Files.readAllBytes( Paths.get( zipFile ) );
   }

   private static List<File> getFileList( File source, final List<File> fileList, final String sourceStoragePath ) {
      if ( source.isDirectory() ) {
         final String[] subList = source.list();
         if ( Objects.requireNonNull( subList ).length == 0 ) {
            source = new File( source.getAbsolutePath() );
         }
         for ( final String child : subList ) {
            if ( !child.endsWith( ".zip" ) ) {
               getFileList( new File( source, child ), fileList, sourceStoragePath );
            }
         }
      } else {
         fileList.add( source );
      }

      return fileList;
   }

   private static String getFileName( final String filePath, final String sourceStoragePath ) {
      return filePath.substring( sourceStoragePath.length() + 1 );
   }

   private static void createNewAspectModelFileWithContent( final File file, final ZipOutputStream zos )
         throws IOException {
      try ( final FileInputStream fileInputStream = new FileInputStream( file );
            final BufferedInputStream bufferedInputStream = new BufferedInputStream( fileInputStream,
                  BUFFER ) ) {

         final byte[] data = new byte[BUFFER];
         int count;
         while ( (count = bufferedInputStream.read( data, 0, BUFFER )) != -1 ) {
            zos.write( data, 0, count );
         }
         IOUtils.closeQuietly( fileInputStream );
         IOUtils.closeQuietly( bufferedInputStream );
      } catch ( final IOException e ) {
         LOG.error( "Cannot find file to write in." );
         throw new FileWriteException( "File for writing not found", e );
      }
   }
}
