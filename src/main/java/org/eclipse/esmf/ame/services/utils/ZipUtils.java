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

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.eclipse.esmf.ame.exceptions.CreateFileException;
import org.eclipse.esmf.ame.exceptions.FileWriteException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZipUtils {
   private static final Logger LOG = LoggerFactory.getLogger( ZipUtils.class );

   private ZipUtils() {
   }

   static final int BUFFER = 1024;

   public static byte[] createPackageFromCache( final Map<String, String> exportCache ) throws IOException {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

      try ( ZipOutputStream zos = new ZipOutputStream( outputStream ) ) {
         Set<String> zipFolderSet = new HashSet<>();
         Set<String> zipVersionedNamespaceSet = new HashSet<>();

         for ( Map.Entry<String, String> entry : exportCache.entrySet() ) {
            final String[] fileStructure = entry.getKey().split( ":" );
            final String aspectModel = entry.getValue();

            final String folder = fileStructure[0] + "/";
            final String versionedNamespace = folder + fileStructure[1] + "/";
            final String file = versionedNamespace + fileStructure[2];

            if ( !zipFolderSet.contains( folder ) ) {
               zos.putNextEntry( new ZipEntry( folder ) );
               zipFolderSet.add( folder );
            }

            if ( !zipVersionedNamespaceSet.contains( versionedNamespace ) ) {
               zos.putNextEntry( new ZipEntry( versionedNamespace ) );
               zipVersionedNamespaceSet.add( versionedNamespace );
            }

            zos.putNextEntry( new ZipEntry( file ) );

            zos.write( aspectModel.getBytes() );
            zos.closeEntry();
         }
      } catch ( final IOException e ) {
         LOG.error( "Cannot create zip file." );
         throw new CreateFileException( "Error creating the zip file.", e );
      }

      return outputStream.toByteArray();
   }

   public static void createPackageFromWorkspace( final String zipFileName, final String aspectModelPath,
         final String storagePath ) throws IOException {
      final String zipFile = aspectModelPath + File.separator + zipFileName;

      try ( FileOutputStream fos = new FileOutputStream( zipFile ); ZipOutputStream zos = new ZipOutputStream( fos ) ) {

         final List<File> fileList = getFileList( new File( storagePath ), new ArrayList<>(), storagePath );

         for ( final File file : fileList ) {
            final String fileName = file.isDirectory() ?
                  getFileName( file.toString(), storagePath ) + File.separator :
                  getFileName( file.toString(), storagePath );
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
      } catch ( final IOException e ) {
         LOG.error( "Cannot find file to write in." );
         throw new FileWriteException( "File for writing not found", e );
      }
   }
}
