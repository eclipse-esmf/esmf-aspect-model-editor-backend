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

package org.eclipse.esmf.ame.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.NoSuchElementException;

import org.eclipse.esmf.ame.api.model.response.ErrorResponse;
import org.eclipse.esmf.aspectmodel.AspectLoadingException;
import org.eclipse.esmf.aspectmodel.UnsupportedVersionException;
import org.eclipse.esmf.aspectmodel.resolver.exceptions.ParserException;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GlobalExceptionHandlerTest {

   private GlobalExceptionHandler handler;
   private HttpRequest<?> request;

   @BeforeEach
   void setUp() {
      handler = new GlobalExceptionHandler();
      request = HttpRequest.GET( URI.create( "/ame/api/models" ) );
   }

   @Test
   void testHandleFileNotFoundException() {
      final FileNotFoundException ex = new FileNotFoundException( "Aspect Model not found" );
      final HttpResponse<?> response = handler.handle( request, ex );

      assertEquals( HttpStatus.NOT_FOUND, response.getStatus() );
      final ErrorResponse body = (ErrorResponse) response.body();
      assertNotNull( body );
      assertEquals( "Aspect Model not found", body.error().message() );
      assertEquals( 404, body.error().code() );
      assertEquals( "/ame/api/models", body.error().path() );
   }

   @Test
   void testHandleInvalidAspectModelException() {
      final InvalidAspectModelException ex = new InvalidAspectModelException( "Aspect Model has invalid syntax" );
      final HttpResponse<?> response = handler.handle( request, ex );

      assertEquals( HttpStatus.CONFLICT, response.getStatus() );
      final ErrorResponse body = (ErrorResponse) response.body();
      assertNotNull( body );
      assertEquals( "Aspect Model has invalid syntax", body.error().message() );
      assertEquals( 409, body.error().code() );
   }

   @Test
   void testHandleGenerationException() {
      final GenerationException ex = new GenerationException( "Failed to generate OpenAPI spec" );
      final HttpResponse<?> response = handler.handle( request, ex );

      assertEquals( HttpStatus.BAD_REQUEST, response.getStatus() );
      final ErrorResponse body = (ErrorResponse) response.body();
      assertNotNull( body );
      assertEquals( "Failed to generate OpenAPI spec", body.error().message() );
      assertEquals( 400, body.error().code() );
   }

   @Test
   void testHandleUriNotDefinedException() {
      final UriNotDefinedException ex = new UriNotDefinedException( "Invalid Aspect Model File URI Format" );
      final HttpResponse<?> response = handler.handle( request, ex );

      assertEquals( HttpStatus.UNPROCESSABLE_ENTITY, response.getStatus() );
      final ErrorResponse body = (ErrorResponse) response.body();
      assertNotNull( body );
      assertEquals( 422, body.error().code() );
   }

   @Test
   void testHandleAspectLoadingExceptionWithNestedCause() {
      final RuntimeException cause = new RuntimeException( "Syntax error at line 5" );
      final AspectLoadingException ex = new AspectLoadingException( "Loading failed", cause );
      final HttpResponse<?> response = handler.handle( request, ex );

      assertEquals( HttpStatus.CONFLICT, response.getStatus() );
      final ErrorResponse body = (ErrorResponse) response.body();
      assertNotNull( body );
      assertTrue( body.error().message().contains( "Syntax error at line 5" ) );
   }

   @Test
   void testHandleIllegalArgumentException() {
      final IllegalArgumentException ex = new IllegalArgumentException( "Invalid aspect model URN: 'urn:invalid'" );
      final HttpResponse<?> response = handler.handle( request, ex );

      assertEquals( HttpStatus.BAD_REQUEST, response.getStatus() );
      final ErrorResponse body = (ErrorResponse) response.body();
      assertNotNull( body );
      assertEquals( "Invalid aspect model URN: 'urn:invalid'", body.error().message() );
      assertEquals( 400, body.error().code() );
   }

   @Test
   void testHandleNoSuchElementException() {
      final NoSuchElementException ex = new NoSuchElementException();
      final HttpResponse<?> response = handler.handle( request, ex );

      assertEquals( HttpStatus.NOT_FOUND, response.getStatus() );
      final ErrorResponse body = (ErrorResponse) response.body();
      assertNotNull( body );
      assertEquals( "The requested Aspect Model element was not found.", body.error().message() );
   }

   @Test
   void testHandleUnsupportedVersionException() {
      final UnsupportedVersionException ex = new UnsupportedVersionException( "1.0.0" );
      final HttpResponse<?> response = handler.handle( request, ex );

      assertEquals( HttpStatus.CONFLICT, response.getStatus() );
      final ErrorResponse body = (ErrorResponse) response.body();
      assertNotNull( body );
      assertTrue( body.error().message().contains( "1.0.0" ) );
   }

   @Test
   void testHandleParserException() {
      final org.apache.jena.riot.RiotException riotEx = new org.apache.jena.riot.RiotException( "Unexpected token at line 3" );
      final ParserException ex = new ParserException( riotEx, "Unexpected token at line 3", URI.create( "file:///model.ttl" ) );
      final HttpResponse<?> response = handler.handle( request, ex );

      assertEquals( HttpStatus.BAD_REQUEST, response.getStatus() );
      final ErrorResponse body = (ErrorResponse) response.body();
      assertNotNull( body );
      assertTrue( body.error().message().contains( "Unexpected token at line 3" ) );
   }

   @Test
   void testHandleIllegalStateException() {
      final IllegalStateException ex = new IllegalStateException( "Outdated BAMM definition" );
      final HttpResponse<?> response = handler.handle( request, ex );

      assertEquals( HttpStatus.BAD_REQUEST, response.getStatus() );
      final ErrorResponse body = (ErrorResponse) response.body();
      assertNotNull( body );
      assertEquals( "Outdated BAMM definition", body.error().message() );
   }

   @Test
   void testHandleFileHandlingException() {
      final FileHandlingException ex = new FileHandlingException( "Failed to process file on disk" );
      final HttpResponse<?> response = handler.handle( request, ex );

      assertEquals( HttpStatus.INTERNAL_SERVER_ERROR, response.getStatus() );
      final ErrorResponse body = (ErrorResponse) response.body();
      assertNotNull( body );
      assertEquals( "Failed to process file on disk", body.error().message() );
      assertEquals( 500, body.error().code() );
   }

   @Test
   void testHandleFileReadException() {
      final FileReadException ex = new FileReadException( "Failed to read file from upload" );
      final HttpResponse<?> response = handler.handle( request, ex );

      assertEquals( HttpStatus.CONFLICT, response.getStatus() );
      final ErrorResponse body = (ErrorResponse) response.body();
      assertNotNull( body );
      assertEquals( "Failed to read file from upload", body.error().message() );
      assertEquals( 409, body.error().code() );
   }

   @Test
   void testHandleCreateFileException() {
      final CreateFileException ex = new CreateFileException( "Cannot create duplicate model" );
      final HttpResponse<?> response = handler.handle( request, ex );

      assertEquals( HttpStatus.CONFLICT, response.getStatus() );
      final ErrorResponse body = (ErrorResponse) response.body();
      assertNotNull( body );
      assertEquals( "Cannot create duplicate model", body.error().message() );
      assertEquals( 409, body.error().code() );
   }

   @Test
   void testHandleUrnNotFoundException() {
      final UrnNotFoundException ex = new UrnNotFoundException( "URN not found", null );
      final HttpResponse<?> response = handler.handle( request, ex );

      assertEquals( HttpStatus.NOT_FOUND, response.getStatus() );
      final ErrorResponse body = (ErrorResponse) response.body();
      assertNotNull( body );
      assertEquals( "URN not found", body.error().message() );
      assertEquals( 404, body.error().code() );
   }

   @Test
   void testHandleURISyntaxException() {
      final URISyntaxException ex = new URISyntaxException( "ht tp://bad uri", "Illegal character" );
      final HttpResponse<?> response = handler.handle( request, ex );

      assertEquals( HttpStatus.UNPROCESSABLE_ENTITY, response.getStatus() );
      final ErrorResponse body = (ErrorResponse) response.body();
      assertNotNull( body );
      assertEquals( 422, body.error().code() );
      assertTrue( body.error().message().contains( "Invalid URI format" ) );
   }

   @Test
   void testHandleGenericException() {
      final NullPointerException ex = new NullPointerException();
      final HttpResponse<?> response = handler.handle( request, ex );

      assertEquals( HttpStatus.INTERNAL_SERVER_ERROR, response.getStatus() );
      final ErrorResponse body = (ErrorResponse) response.body();
      assertNotNull( body );
      assertEquals( 500, body.error().code() );
      assertTrue( body.error().message().contains( "NullPointerException" ) );
   }
}


