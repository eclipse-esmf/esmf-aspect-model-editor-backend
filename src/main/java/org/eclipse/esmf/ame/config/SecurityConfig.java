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
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Application Security configuration file to configure HTTPSecurity
 */
@Configuration
public class SecurityConfig {
   /**
    * overridden Function to configure HTTPSecurity
    *
    * @throws Exception On input error.
    */
   @Bean
   public SecurityFilterChain filterChain( final HttpSecurity http ) throws Exception {
      return http.csrf().disable().authorizeHttpRequests(
                       requests -> requests.requestMatchers( "/**" ).permitAll().anyRequest().authenticated() )
                 .build();
   }
}
