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

package org.eclipse.esmf.ame.api;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.esmf.ame.exceptions.FileNotFoundException;
import org.eclipse.esmf.ame.exceptions.FileReadException;
import org.eclipse.esmf.ame.model.MockFileUpload;
import org.eclipse.esmf.ame.security.FileNameSanitizer;
import org.eclipse.esmf.ame.services.PackageService;
import org.eclipse.esmf.ame.services.models.Version;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.multipart.CompletedFileUpload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PackageControllerTest {

   private PackageService packageService;
   private FileNameSanitizer fileNameSanitizer;
   private PackageController controller;

   private static final String VALID_URN = "urn:samm:org.eclipse.esmf.example:1.0.0#Movement";

   @BeforeEach
   void setUp() {
      packageService = mock( PackageService.class );
      fileNameSanitizer = new FileNameSanitizer();
      controller = new PackageController( packageService, fileNameSanitizer );
   }

   @Test
   void testExportPackageSuccess() {
      final byte[] expectedZip = new byte[]{ 0x50, 0x4B, 0x03, 0x04 };
      when( packageService.exportPackage( VALID_URN ) ).thenReturn( expectedZip );

      final HttpResponse<byte[]> response = controller.exportPackage( Optional.of( VALID_URN ) );

      assertEquals( HttpStatus.OK, response.getStatus() );
      assertEquals( MediaType.APPLICATION_ZIP, response.getContentType().map( MediaType::getName ).orElse( null ) );
      assertNotNull( response.getHeaders().get( "Content-Disposition" ) );
      assertArrayEquals( expectedZip, response.body() );
   }

   @Test
   void testExportPackageMissingUrnThrowsFileNotFoundException() {
      final FileNotFoundException ex = assertThrows( FileNotFoundException.class,
            () -> controller.exportPackage( Optional.empty() ) );
      assertEquals( 404, ex.getHttpStatusCode() );
   }

   @Test
   void testImportPackageSuccess() {
      final CompletedFileUpload upload = MockFileUpload.create( "package.zip" );
      final Map<String, List<Version>> result = Map.of( "org.eclipse.esmf.example", Collections.emptyList() );
      when( packageService.importPackage( upload ) ).thenReturn( result );

      final HttpResponse<Map<String, List<Version>>> response = controller.importPackage( upload );

      assertEquals( HttpStatus.OK, response.getStatus() );
      assertNotNull( response.body() );
      assertTrue( response.body().containsKey( "org.eclipse.esmf.example" ) );
   }

   @Test
   void testImportPackageInvalidExtensionThrowsFileReadException() {
      final CompletedFileUpload upload = MockFileUpload.create( "package.txt" );

      final FileReadException ex = assertThrows( FileReadException.class,
            () -> controller.importPackage( upload ) );
      assertEquals( 409, ex.getHttpStatusCode() );
      assertTrue( ex.getMessage().contains( "is not in ZIP format" ) );
   }

   @Test
   void testBackupWorkspaceSuccess() {
      final HttpResponse<String> response = controller.backupWorkspace();

      assertEquals( HttpStatus.CREATED, response.getStatus() );
      verify( packageService ).backupWorkspace();
   }
}

