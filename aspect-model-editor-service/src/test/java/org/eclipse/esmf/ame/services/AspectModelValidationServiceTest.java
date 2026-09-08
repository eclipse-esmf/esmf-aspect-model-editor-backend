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

package org.eclipse.esmf.ame.services;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.esmf.ame.model.MockFileUpload;
import org.eclipse.esmf.ame.validation.model.ViolationReport;

import io.micronaut.http.MediaType;
import io.micronaut.http.multipart.CompletedFileUpload;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@MicronautTest
class AspectModelValidationServiceTest {
   @Inject
   private AspectModelValidationService validationService;

   private static final String FILE_EXTENSION = ".ttl";

   private static final Path RESOURCE_PATH = Path.of( "src", "test", "resources", "services" );

   private static final String EXAMPLE_NAMESPACE = "org.eclipse.esmf.example";
   private static final String VERSION = "1.0.0";

   private static final Path TEST_NAMESPACE_PATH = Path.of( RESOURCE_PATH.toString(), EXAMPLE_NAMESPACE, VERSION );

   private static final String TEST_MODEL_FOR_SERVICE = "Movement";

   @Test
   void testValidateModel() throws IOException {
      final Path storagePath = Path.of( TEST_NAMESPACE_PATH.toString(), TEST_MODEL_FOR_SERVICE + FILE_EXTENSION );
      final byte[] testModelForService = Files.readAllBytes( storagePath );
      final CompletedFileUpload mockedZipFile = MockFileUpload.create( "TestArchive.ttl", testModelForService,
            MediaType.of( MediaType.MULTIPART_FORM_DATA ) );

      final ViolationReport validateReport = validationService.validate( URI.create( "blob:///" + toUriPath( storagePath ) ),
            mockedZipFile );

      assertTrue( validateReport.getViolationErrors().isEmpty() );
   }

   private String toUriPath( final Path path ) {
      String uriPath = path.toString();
      if ( System.getProperty( "os.name" ).toLowerCase().contains( "win" ) ) {
         uriPath = uriPath.replace( "\\", "/" );
      }
      return uriPath;
   }
}

