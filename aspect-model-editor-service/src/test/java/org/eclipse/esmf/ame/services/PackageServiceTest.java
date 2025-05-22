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

package org.eclipse.esmf.ame.services;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.MediaType;
import io.micronaut.http.multipart.CompletedFileUpload;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@MicronautTest
class PackageServiceTest {
   @Inject
   private PackageService packageService;

   private static final String FILE_EXTENSION = ".ttl";

   private static final Path RESOURCE_PATH = Path.of( "src", "test", "resources", "services" );

   private static final String SAMM_URN = "urn:samm";
   private static final String VERSION = "1.0.0";
   private static final String EXPORT_NAMESPACE = "org.eclipse.esmf.export";

   private static final String NAMESPACE_VERSION = SAMM_URN + ":" + EXPORT_NAMESPACE + ":" + VERSION;

   private static final String FILE_ONE = "TestFileOne";

   @Test
   void testExportAspectModelPackage() {
      final byte[] exportPackage = packageService.exportPackage( NAMESPACE_VERSION + "#" + FILE_ONE );

      assertDoesNotThrow( () -> {
         try ( final ByteArrayInputStream bais = new ByteArrayInputStream( exportPackage );
               final ZipInputStream zis = new ZipInputStream( bais ) ) {
            ZipEntry entry;
            while ( ( entry = zis.getNextEntry() ) != null ) {
               assertNotNull( entry.getName() );
               assertFalse( entry.getName().isEmpty() );
               assertTrue( entry.getName().contains( "TestFileOne" + FILE_EXTENSION ) || entry.getName()
                     .contains( "TestFileTwo" + FILE_EXTENSION ) || entry.getName().contains( "TestFileThree" + FILE_EXTENSION ) );
            }
         }
      } );
   }

   @Test
   void testImportAspectModelPackage() throws IOException {
      final Path zipFilePath = Paths.get( RESOURCE_PATH.toString(), "TestArchive.zip" );
      final byte[] testPackage = Files.readAllBytes( zipFilePath );

      final CompletedFileUpload mockedZipFile = new MockFileUpload( "TestArchive.zip", testPackage, MediaType.APPLICATION_PDF_TYPE );

      packageService.importPackage( mockedZipFile );

      try ( final ZipInputStream zis = new ZipInputStream( new ByteArrayInputStream( testPackage ) ) ) {
         ZipEntry entry;
         while ( ( entry = zis.getNextEntry() ) != null ) {
            if ( !entry.isDirectory() && entry.getName().endsWith( FILE_EXTENSION ) ) {
               final Path extractedFilePath = RESOURCE_PATH.resolve( entry.getName() );
               System.out.println( extractedFilePath );
               assertTrue( Files.exists( extractedFilePath ), "File " + entry.getName() + " should exist" );
            }
         }
      }
   }

   @Test
   void testBackupWorkspace() {
      packageService.backupWorkspace();

      assertTrue( Arrays.stream( Objects.requireNonNull( RESOURCE_PATH.toFile().list() ) )
            .anyMatch( file -> file.contains( "backup-" ) ) );
   }

   private class MockFileUpload implements CompletedFileUpload {
      private final String filename;
      private final MediaType mediaType;
      private final byte[] content;

      public MockFileUpload( final String filename, final byte[] content, final MediaType mediaType ) {
         this( filename, mediaType, content );
      }

      public MockFileUpload( final String filename, final MediaType mediaType, @Nullable final byte[] content ) {
         this.filename = filename;
         this.mediaType = mediaType;
         this.content = ( content != null ? content : new byte[0] );
      }

      @Override
      public InputStream getInputStream() {
         return new ByteArrayInputStream( content );
      }

      @Override
      public byte[] getBytes() {
         return content;
      }

      @Override
      public ByteBuffer getByteBuffer() {
         return ByteBuffer.wrap( content );
      }

      @Override
      public Optional<MediaType> getContentType() {
         return Optional.of( mediaType );
      }

      @Override
      public String getName() {
         return filename;
      }

      @Override
      public String getFilename() {
         return filename;
      }

      @Override
      public long getSize() {
         return content.length;
      }

      @Override
      public long getDefinedSize() {
         return content.length;
      }

      @Override
      public boolean isComplete() {
         return true;
      }
   }
}
