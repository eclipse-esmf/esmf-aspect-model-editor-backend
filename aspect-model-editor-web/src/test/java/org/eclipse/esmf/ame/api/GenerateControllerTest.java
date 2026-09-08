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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.eclipse.esmf.ame.exceptions.GenerationException;
import org.eclipse.esmf.ame.exceptions.UriNotDefinedException;
import org.eclipse.esmf.ame.model.MockFileUpload;
import org.eclipse.esmf.ame.services.GenerateService;
import org.eclipse.esmf.aspectmodel.generator.openapi.PagingOption;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.multipart.CompletedFileUpload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GenerateControllerTest {

   private GenerateService generateService;
   private GenerateController controller;

   private static final String VALID_URI = "blob://Movement.ttl";

   @BeforeEach
   void setUp() {
      generateService = mock( GenerateService.class );
      controller = new GenerateController( generateService );
   }

   @Test
   void testGenerateHtmlSuccess() {
      final CompletedFileUpload upload = MockFileUpload.create( "model.ttl" );
      final byte[] expectedBytes = "<html>Documentation</html>".getBytes( StandardCharsets.UTF_8 );
      when( generateService.generateHtmlDocument( eq( upload ), any( URI.class ), eq( "en" ) ) ).thenReturn( expectedBytes );

      final HttpResponse<byte[]> response = controller.generateHtml( Optional.of( VALID_URI ), upload, "en" );

      assertEquals( HttpStatus.OK, response.getStatus() );
      assertArrayEquals( expectedBytes, response.body() );
   }

   @Test
   void testGenerateHtmlMissingUriThrowsUriNotDefinedException() {
      final CompletedFileUpload upload = MockFileUpload.create( "model.ttl" );

      final UriNotDefinedException ex = assertThrows( UriNotDefinedException.class,
            () -> controller.generateHtml( Optional.empty(), upload, "en" ) );
      assertEquals( 422, ex.getHttpStatusCode() );
   }

   @Test
   void testGenerateHtmlInvalidUriThrowsUriNotDefinedException() {
      final CompletedFileUpload upload = MockFileUpload.create( "model.ttl" );

      final UriNotDefinedException ex = assertThrows( UriNotDefinedException.class,
            () -> controller.generateHtml( Optional.of( "ht tp://bad uri" ), upload, "en" ) );
      assertEquals( 422, ex.getHttpStatusCode() );
      assertTrue( ex.getMessage().contains( "Invalid URI format" ) );
   }

   @Test
   void testJsonSchemaSuccess() {
      final CompletedFileUpload upload = MockFileUpload.create( "model.ttl" );
      when( generateService.jsonSchema( eq( upload ), any( URI.class ), eq( "en" ) ) ).thenReturn( "{\"type\":\"object\"}" );

      final HttpResponse<String> response = controller.jsonSchema( Optional.of( VALID_URI ), upload, "en" );

      assertEquals( HttpStatus.OK, response.getStatus() );
      assertEquals( "{\"type\":\"object\"}", response.body() );
   }

   @Test
   void testJsonSampleSuccess() {
      final CompletedFileUpload upload = MockFileUpload.create( "model.ttl" );
      when( generateService.sampleJSONPayload( eq( upload ), any( URI.class ) ) ).thenReturn( "{\"name\":\"Sample\"}" );

      final HttpResponse<Object> response = controller.jsonSample( Optional.of( VALID_URI ), upload );

      assertEquals( HttpStatus.OK, response.getStatus() );
      assertEquals( "{\"name\":\"Sample\"}", response.body() );
   }

   @Test
   void testGenerateAasxSuccess() {
      final CompletedFileUpload upload = MockFileUpload.create( "model.ttl" );
      when( generateService.generateAASXFile( eq( upload ), any( URI.class ) ) ).thenReturn( "aasx-content" );

      final HttpResponse<String> response = controller.generateAasx( Optional.of( VALID_URI ), upload );

      assertEquals( HttpStatus.OK, response.getStatus() );
      assertEquals( "aasx-content", response.body() );
   }

   @Test
   void testGenerateAasXmlSuccess() {
      final CompletedFileUpload upload = MockFileUpload.create( "model.ttl" );
      when( generateService.generateAasXmlFile( eq( upload ), any( URI.class ) ) ).thenReturn( "<xml>aas</xml>" );

      final HttpResponse<String> response = controller.generateAasXml( Optional.of( VALID_URI ), upload );

      assertEquals( HttpStatus.OK, response.getStatus() );
      assertEquals( "<xml>aas</xml>", response.body() );
   }

   @Test
   void testGenerateAasJsonSuccess() {
      final CompletedFileUpload upload = MockFileUpload.create( "model.ttl" );
      when( generateService.generateAasJsonFile( eq( upload ), any( URI.class ) ) ).thenReturn( "{\"aas\":\"data\"}" );

      final HttpResponse<String> response = controller.generateAasJson( Optional.of( VALID_URI ), upload );

      assertEquals( HttpStatus.OK, response.getStatus() );
      assertEquals( "{\"aas\":\"data\"}", response.body() );
   }

   @Test
   void testOpenApiSpecYamlSuccess() {
      final CompletedFileUpload upload = MockFileUpload.create( "model.ttl" );
      when( generateService.generateYamlOpenApiSpec( eq( upload ), any( URI.class ), any() ) ).thenReturn( "openapi: 3.0.0" );

      final HttpResponse<String> response = controller.openApiSpec(
            Optional.of( VALID_URI ), upload, "en", "yaml", "https://example.com",
            false, false, PagingOption.TIME_BASED_PAGING, false, false, false, false,
            "", "", ""
      );

      assertEquals( HttpStatus.OK, response.getStatus() );
      assertEquals( MediaType.APPLICATION_YAML, response.getContentType().map( MediaType::getName ).orElse( null ) );
      assertEquals( "openapi: 3.0.0", response.body() );
   }

   @Test
   void testOpenApiSpecJsonSuccess() {
      final CompletedFileUpload upload = MockFileUpload.create( "model.ttl" );
      when( generateService.generateJsonOpenApiSpec( eq( upload ), any( URI.class ), any() ) ).thenReturn( "{\"openapi\":\"3.0.0\"}" );

      final HttpResponse<String> response = controller.openApiSpec(
            Optional.of( VALID_URI ), upload, "en", "json", "https://example.com",
            false, false, PagingOption.TIME_BASED_PAGING, false, false, false, false,
            "", "", ""
      );

      assertEquals( HttpStatus.OK, response.getStatus() );
      assertEquals( MediaType.APPLICATION_JSON, response.getContentType().map( MediaType::getName ).orElse( null ) );
      assertEquals( "{\"openapi\":\"3.0.0\"}", response.body() );
   }

   @Test
   void testOpenApiSpecInvalidPropertiesThrowsGenerationException() {
      final CompletedFileUpload upload = MockFileUpload.create( "model.ttl" );

      final GenerationException ex = assertThrows( GenerationException.class, () -> controller.openApiSpec(
            Optional.of( VALID_URI ), upload, "en", "json", "https://example.com",
            false, false, PagingOption.TIME_BASED_PAGING, false, false, false, false,
            "/resource", "", "INVALID_JSON_NOT_AN_OBJECT"
      ) );

      assertEquals( 400, ex.getHttpStatusCode() );
      assertTrue( ex.getMessage().contains( "Invalid OpenAPI properties format" ) );
   }

   @Test
   void testAsyncApiSpecSingleFileSuccess() {
      final CompletedFileUpload upload = MockFileUpload.create( "model.ttl" );
      final byte[] yamlBytes = "asyncapi: 2.6.0".getBytes( StandardCharsets.UTF_8 );
      when( generateService.generateAsyncApiSpec( eq( upload ), any( URI.class ), eq( "en" ), eq( "yaml" ), any(), any(), eq( false ), eq( false ) ) )
            .thenReturn( yamlBytes );

      final HttpResponse<byte[]> response = controller.asyncApiSpec(
            Optional.of( VALID_URI ), upload, "en", "yaml", "app-id", "topic", false, false
      );

      assertEquals( HttpStatus.OK, response.getStatus() );
      assertEquals( MediaType.APPLICATION_YAML, response.getContentType().map( MediaType::getName ).orElse( null ) );
      assertArrayEquals( yamlBytes, response.body() );
   }

   @Test
   void testAsyncApiSpecZipPackageSuccess() {
      final CompletedFileUpload upload = MockFileUpload.create( "model.ttl" );
      final byte[] zipBytes = new byte[]{ 0x50, 0x4B, 0x03, 0x04 };
      when( generateService.generateAsyncApiSpec( eq( upload ), any( URI.class ), eq( "en" ), eq( "yaml" ), any(), any(), eq( false ), eq( true ) ) )
            .thenReturn( zipBytes );

      final HttpResponse<byte[]> response = controller.asyncApiSpec(
            Optional.of( VALID_URI ), upload, "en", "yaml", "app-id", "topic", false, true
      );

      assertEquals( HttpStatus.OK, response.getStatus() );
      assertEquals( MediaType.APPLICATION_ZIP, response.getContentType().map( MediaType::getName ).orElse( null ) );
      assertNotNull( response.getHeaders().get( "Content-Disposition" ) );
   }
}

