/*
 * Copyright (c) 2022 Robert Bosch Manufacturing Solutions GmbH
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

package io.openmanufacturing.ame.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configurers.ExpressionUrlAuthorizationConfigurer;

/**
 * Application Security configuration file to configure HTTPSecurity
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

   private final ApplicationSettings applicationSettings;

   public SecurityConfig( final ApplicationSettings applicationSettings ) {
      this.applicationSettings = applicationSettings;
   }

   /**
    * overridden Function to configure HTTPSecurity
    * @throws Exception On input error.
    */
   @Override
   @SuppressWarnings("java:S4502")
   protected void configure( final HttpSecurity http ) throws Exception {
       final ExpressionUrlAuthorizationConfigurer<HttpSecurity>.AuthorizedUrl authorizedUrl = http
               .csrf()
               .disable()
               .authorizeRequests()
               .antMatchers("/**");

       if (Boolean.TRUE.equals(applicationSettings.getLocal())) {
           authorizedUrl.hasIpAddress("127.0.0.1");
       } else {
           authorizedUrl.permitAll();
       }
   }
}
