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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.esmf.ame.security.AuthTokenFilter;

import io.micronaut.context.annotation.Property;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@MicronautTest
@Property( name = AuthTokenFilter.TOKEN_PROPERTY, value = AuthTokenFilterTest.TOKEN )
class AuthTokenFilterTest {
   static final String TOKEN = "test-session-token";
   private static final String STORAGE_PATH = "/ame/api/models/storage-path";
   private static final String UI_ORIGIN = "tauri://localhost";

   @Inject
   @Client( "/" )
   HttpClient client;

   private HttpStatus status( final MutableHttpRequest<?> request ) {
      try {
         return client.toBlocking().exchange( request ).getStatus();
      } catch ( final HttpClientResponseException exception ) {
         return exception.getStatus();
      }
   }

   @Test
   void requestWithTheTokenIsAccepted() {
      assertEquals( HttpStatus.OK, status( HttpRequest.GET( STORAGE_PATH ).header( AuthTokenFilter.TOKEN_HEADER, TOKEN ) ) );
   }

   @Test
   void requestWithoutTheTokenIsRejected() {
      assertEquals( HttpStatus.UNAUTHORIZED, status( HttpRequest.GET( STORAGE_PATH ) ) );
   }

   @Test
   void requestWithAWrongTokenIsRejected() {
      assertEquals( HttpStatus.UNAUTHORIZED, status( HttpRequest.GET( STORAGE_PATH ).header( AuthTokenFilter.TOKEN_HEADER, "wrong" ) ) );
   }

   @Test
   void simpleCrossOriginPostWithoutTheTokenIsRejected() {
      // Rejected either by the CORS filter (403, unknown origin) or by the token filter (401)
      final HttpStatus status = status( HttpRequest.POST( "/ame/api/package/backup-workspace", "" ).header( "Origin", "http://evil.example" ) );
      assertTrue( status == HttpStatus.FORBIDDEN || status == HttpStatus.UNAUTHORIZED, "unexpected status " + status );
   }

   @Test
   void rejectionCarriesCorsHeadersForTheUi() {
      final HttpClientResponseException exception = assertThrows( HttpClientResponseException.class,
            () -> client.toBlocking().exchange( HttpRequest.GET( STORAGE_PATH ).header( "Origin", UI_ORIGIN ) ) );
      assertEquals( HttpStatus.UNAUTHORIZED, exception.getStatus() );
      assertEquals( UI_ORIGIN, exception.getResponse().getHeaders().get( "Access-Control-Allow-Origin" ) );
   }

   @Test
   void preflightForTheTokenHeaderIsAllowed() {
      assertEquals( HttpStatus.OK, status( HttpRequest.OPTIONS( STORAGE_PATH )
            .header( "Origin", UI_ORIGIN )
            .header( "Access-Control-Request-Method", "GET" )
            .header( "Access-Control-Request-Headers", AuthTokenFilter.TOKEN_HEADER ) ) );
   }
}
