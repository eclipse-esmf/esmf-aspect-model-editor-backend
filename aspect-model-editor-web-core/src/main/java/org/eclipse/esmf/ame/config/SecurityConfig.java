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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Represents the application's security configuration.
 * This configuration is responsible for setting up the HTTP security rules,
 * such as request matchers and CSRF settings.
 *
 * <p>Typical usage includes disabling CSRF and permitting all requests to certain paths.</p>
 */
@Configuration
@PropertySource( { "classpath:/multipart-default.properties" } )
public class SecurityConfig {
   /**
    * Configures the HTTP security filter chain. This configuration disables CSRF,
    * permits all requests to paths matching "/**", and requires authentication for
    * all other requests.
    *
    * @param http an {@link HttpSecurity} instance to configure.
    * @return the configured {@link SecurityFilterChain}.
    * @throws Exception if there's an error configuring the {@link HttpSecurity}.
    */
   @Bean
   public SecurityFilterChain filterChain( final HttpSecurity http ) throws Exception {
      return http.csrf().disable().authorizeHttpRequests(
                  requests -> requests.requestMatchers( "/**" ).permitAll().anyRequest().authenticated() )
            .build();
   }
}
