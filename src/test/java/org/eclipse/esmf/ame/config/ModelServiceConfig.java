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

import org.eclipse.esmf.ame.repository.ModelResolverRepository;
import org.eclipse.esmf.ame.repository.strategy.ModelResolverStrategy;
import org.eclipse.esmf.ame.services.ModelService;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Profile( "test" )
@Configuration
public class ModelServiceConfig {

   @Bean
   @Primary
   public ModelService modelService() {
      return Mockito.mock( ModelService.class );
   }

   @Bean
   @Primary
   public ModelResolverRepository modelResolverRepository() {
      return Mockito.mock( ModelResolverRepository.class );
   }

   @Bean
   @Primary
   public ModelResolverStrategy modelResolverStrategy() {
      return Mockito.mock( ModelResolverStrategy.class );
   }
}
