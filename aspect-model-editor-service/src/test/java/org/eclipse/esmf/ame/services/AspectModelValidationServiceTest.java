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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.eclipse.esmf.ame.exceptions.InvalidAspectModelException;
import org.eclipse.esmf.ame.model.MockFileUpload;
import org.eclipse.esmf.ame.repository.AspectModelRepository;
import org.eclipse.esmf.ame.validation.model.ViolationReport;
import org.eclipse.esmf.aspectmodel.resolver.ModelResolutionViolation;
import org.eclipse.esmf.aspectmodel.resolver.exceptions.ModelResolutionException;
import org.eclipse.esmf.aspectmodel.urn.AspectModelUrn;
import org.eclipse.esmf.aspectmodel.validation.services.AspectModelValidator;

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

   @Test
   void testValidateThrowsInvalidAspectModelExceptionWithAllCheckedLocationsInfo() {
      final AspectModelRepository mockRepository = mock( AspectModelRepository.class );
      final AspectModelValidator mockValidator = mock( AspectModelValidator.class );
      final AspectModelValidationService service = new AspectModelValidationService( mockRepository, mockValidator );

      final AspectModelUrn urn1 = AspectModelUrn.fromUrn( "urn:samm:org.eclipse.esmf.example:1.0.0#ElementOne" );
      final AspectModelUrn urn2 = AspectModelUrn.fromUrn( "urn:samm:org.eclipse.esmf.example:1.0.0#ElementTwo" );

      final ModelResolutionViolation violation1 = new ModelResolutionViolation(
            Optional.of( urn1 ),
            URI.create( "file:///path/to/one.ttl" ),
            "First element error",
            Optional.of( new RuntimeException( "Root cause 1" ) )
      );
      final ModelResolutionViolation violation2 = new ModelResolutionViolation(
            Optional.of( urn2 ),
            URI.create( "file:///path/to/two.ttl" ),
            "Second element error",
            Optional.of( new RuntimeException( "Root cause 2" ) )
      );

      final ModelResolutionException mre = new ModelResolutionException( List.of( violation1, violation2 ) );
      when( mockRepository.loadFromUpload( any(), any() ) ).thenThrow( mre );

      final CompletedFileUpload mockUpload = MockFileUpload.create( "test.ttl", new byte[0],
            MediaType.of( MediaType.MULTIPART_FORM_DATA ) );
      final InvalidAspectModelException ex = assertThrows( InvalidAspectModelException.class,
            () -> service.validate( URI.create( "blob://test.ttl" ), mockUpload ) );

      final String message = ex.getMessage();
      assertTrue( message.contains( "Element '" + urn1 + "' does not exist in a file." ) );
      assertTrue( message.contains( "Element '" + urn2 + "' does not exist in a file." ) );
   }

   @Test
   void testValidateThrowsInvalidAspectModelExceptionFallbackWhenNoElement() {
      final AspectModelRepository mockRepository = mock( AspectModelRepository.class );
      final AspectModelValidator mockValidator = mock( AspectModelValidator.class );
      final AspectModelValidationService service = new AspectModelValidationService( mockRepository, mockValidator );

      final ModelResolutionViolation violation = new ModelResolutionViolation(
            Optional.empty(),
            URI.create( "file:///path/to/file.ttl" ),
            "General resolution error",
            Optional.empty()
      );

      final ModelResolutionException mre = new ModelResolutionException( List.of( violation ) );
      when( mockRepository.loadFromUpload( any(), any() ) ).thenThrow( mre );

      final CompletedFileUpload mockUpload = MockFileUpload.create( "test.ttl", new byte[0],
            MediaType.of( MediaType.MULTIPART_FORM_DATA ) );
      final InvalidAspectModelException ex = assertThrows( InvalidAspectModelException.class,
            () -> service.validate( URI.create( "blob://test.ttl" ), mockUpload ) );

      assertTrue( ex.getMessage().contains( "General resolution error" ) );
   }

   private String toUriPath( final Path path ) {
      String uriPath = path.toString();
      if ( System.getProperty( "os.name" ).toLowerCase().contains( "win" ) ) {
         uriPath = uriPath.replace( "\\", "/" );
      }
      return uriPath;
   }
}

