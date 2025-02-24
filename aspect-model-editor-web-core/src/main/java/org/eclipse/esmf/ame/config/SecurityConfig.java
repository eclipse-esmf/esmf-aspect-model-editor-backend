/*
 * Copyright (c) 2023 Robert Bosch Manufacturing Solutions GmbH
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

package org.eclipse.esmf.ame.config;

import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;

/**
 * Represents the application's security configuration.
 * This configuration is responsible for setting up the HTTP security rules,
 * such as request matchers and CSRF settings.
 *
 * <p>Typical usage includes disabling CSRF and permitting all requests to certain paths.</p>
 */

@Singleton
@Requires( property = "micronaut.security.enabled", value = "true" )
@Filter( "/**" )
public class SecurityConfig implements HttpServerFilter {
   @Override
   public Publisher<MutableHttpResponse<?>> doFilter( final HttpRequest<?> request, final ServerFilterChain chain ) {
      return chain.proceed( request );
   }
}