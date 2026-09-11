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

package org.eclipse.esmf.ame;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.http.client.multipart.MultipartBody;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@MicronautTest
public class ApplicationTest {

   @Inject
   @Client( "/" )
   HttpClient client;

   @Test
   void contextLoads() {
      // This test will simply check if the application context loads successfully
      assertNotNull( client );
   }

   @Test
   void testValidateWithInvalidSyntax() throws Exception {
      final File file = new File( "bruno/TestingFiles/AspectWitchInvalidSyntaxAspectModel.ttl" );
      final byte[] bytes = Files.readAllBytes( file.toPath() );

      final MultipartBody body = MultipartBody.builder()
            .addPart( "aspectModel", file.getName(), MediaType.TEXT_PLAIN_TYPE, bytes )
            .build();

      final HttpRequest<?> request = HttpRequest.POST( "/ame/api/models/validate", body )
            .contentType( MediaType.MULTIPART_FORM_DATA_TYPE )
            .header( "uri", "blob://test.ttl" );

      final HttpResponse<String> response = client.toBlocking().exchange( request, String.class );
      assertEquals( HttpStatus.OK, response.getStatus() );
      final String responseBody = response.getBody().orElse( "" );
      assertTrue( responseBody.contains( "ERR_WRONG_DATATYPE" ) );
   }

   @Test
   void testValidateWithInvalidAspectModel() throws Exception {
      final File file = new File( "bruno/TestingFiles/InvalidAspectModel.ttl" );
      final byte[] bytes = Files.readAllBytes( file.toPath() );

      final MultipartBody body = MultipartBody.builder()
            .addPart( "aspectModel", file.getName(), MediaType.TEXT_PLAIN_TYPE, bytes )
            .build();

      final HttpRequest<?> request = HttpRequest.POST( "/ame/api/models/validate", body )
            .contentType( MediaType.MULTIPART_FORM_DATA_TYPE )
            .header( "uri", "blob://test.ttl" );

      final HttpClientResponseException exception = assertThrows(
            HttpClientResponseException.class,
            () -> client.toBlocking().exchange( request, String.class )
      );

      assertEquals( HttpStatus.CONFLICT, exception.getStatus() );
      final String errorBody = exception.getResponse().getBody( String.class ).orElse( "" );
      assertTrue( errorBody.contains( "No datatype is defined on the Characteristic instance 'Characteristic1'" ) );
   }
}
