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

package org.eclipse.esmf.ame.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.annotation.Value;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.order.Ordered;
import io.micronaut.http.HttpMethod;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.RequestFilter;
import io.micronaut.http.annotation.ServerFilter;
import io.micronaut.http.filter.ServerFilterPhase;

/**
 * Rejects requests that do not carry the session token handed to the backend by the desktop app.
 * <p>
 * The backend listens on a local port that every process and every web page in a browser can reach. CORS only prevents
 * foreign origins from reading responses, it does not prevent "simple" requests (e.g. form posts) from being executed.
 * Requiring a custom header closes this gap, because browsers never send custom headers cross-origin without a successful
 * preflight. The filter is only active if a token is configured ({@value #TOKEN_PROPERTY}, environment variable
 * {@code AME_AUTH_TOKEN}), so the standalone development setup keeps working without a token.
 */
@ServerFilter( ServerFilter.MATCH_ALL_PATTERN )
@Requires( property = AuthTokenFilter.TOKEN_PROPERTY, pattern = ".+" )
public class AuthTokenFilter implements Ordered {
   public static final String TOKEN_PROPERTY = "ame.auth.token";
   public static final String TOKEN_HEADER = "X-AME-Token";

   private final byte[] expectedToken;

   public AuthTokenFilter( @Value( "${" + TOKEN_PROPERTY + "}" ) final String token ) {
      expectedToken = token.getBytes( StandardCharsets.UTF_8 );
   }

   /**
    * Runs after the CORS filter, so rejected requests still carry the CORS headers and the UI can read the status.
    */
   @Override
   public int getOrder() {
      return ServerFilterPhase.SECURITY.order();
   }

   @RequestFilter
   @Nullable
   public HttpResponse<?> verifyToken( final HttpRequest<?> request ) {
      // CORS preflights never carry custom headers
      if ( request.getMethod() == HttpMethod.OPTIONS ) {
         return null;
      }

      final String providedToken = request.getHeaders().get( TOKEN_HEADER );
      if ( providedToken != null && MessageDigest.isEqual( expectedToken, providedToken.getBytes( StandardCharsets.UTF_8 ) ) ) {
         return null;
      }
      return HttpResponse.status( HttpStatus.UNAUTHORIZED );
   }
}
