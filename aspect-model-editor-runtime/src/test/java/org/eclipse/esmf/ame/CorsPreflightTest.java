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

import io.micronaut.http.HttpMethod;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

/**
 * The browser sends a CORS preflight before every DELETE. Requests without a custom header (namespace deletion,
 * clearing the workspace) send it without Access-Control-Request-Headers and must not be rejected.
 */
@MicronautTest
class CorsPreflightTest {
   private static final String UI_ORIGIN = "http://localhost:4200";

   @Inject
   @Client( "/" )
   HttpClient client;

   private MutableHttpRequest<?> preflight( final String uri, final String origin, final String method ) {
      return HttpRequest.OPTIONS( uri )
            .header( "Origin", origin )
            .header( "Access-Control-Request-Method", method );
   }

   private HttpResponse<?> exchange( final MutableHttpRequest<?> request ) {
      return client.toBlocking().exchange( request );
   }

   @Test
   void deleteNamespacePreflightWithoutRequestHeadersIsAllowed() {
      final HttpResponse<?> response = exchange(
            preflight( "/ame/api/models/namespace?namespace=org.example&version=1.0.0", UI_ORIGIN, "DELETE" ) );

      assertEquals( HttpStatus.OK, response.getStatus() );
      assertEquals( UI_ORIGIN, response.getHeaders().get( "Access-Control-Allow-Origin" ) );
      assertEquals( true, response.getHeaders().get( "Access-Control-Allow-Methods" ).contains( HttpMethod.DELETE.name() ) );
   }

   @Test
   void clearWorkspacePreflightWithoutRequestHeadersIsAllowed() {
      assertEquals( HttpStatus.OK, exchange( preflight( "/ame/api/models/workspace?backup=true", UI_ORIGIN, "DELETE" ) ).getStatus() );
   }

   @Test
   void preflightFromTheDesktopAppIsAllowed() {
      assertEquals( HttpStatus.OK,
            exchange( preflight( "/ame/api/models/namespace?namespace=org.example&version=1.0.0", "tauri://localhost", "DELETE" ) )
                  .getStatus() );
   }

   @Test
   void preflightWithCustomHeadersIsStillAllowed() {
      final HttpResponse<?> response = exchange(
            preflight( "/ame/api/models", UI_ORIGIN, "DELETE" ).header( "Access-Control-Request-Headers", "aspect-model-urn" ) );

      assertEquals( HttpStatus.OK, response.getStatus() );
   }

   @Test
   void preflightFromAnUnknownOriginIsRejected() {
      final HttpClientResponseException exception = assertThrows( HttpClientResponseException.class,
            () -> exchange( preflight( "/ame/api/models/workspace?backup=true", "http://evil.example", "DELETE" ) ) );

      assertEquals( HttpStatus.FORBIDDEN, exception.getStatus() );
   }
}
