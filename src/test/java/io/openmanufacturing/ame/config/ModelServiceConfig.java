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

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import io.openmanufacturing.ame.repository.ModelResolverRepository;
import io.openmanufacturing.ame.repository.strategy.ModelResolverStrategy;
import io.openmanufacturing.ame.services.ModelService;

@Profile("test")
@Configuration
public class ModelServiceConfig {

    @Bean
    @Primary
    public ModelService modelService() {
        return Mockito.mock(ModelService.class);
    }

    @Bean
    @Primary
    public ModelResolverRepository modelResolverRepository() {
        return Mockito.mock(ModelResolverRepository.class);
    }

    @Bean
    @Primary
    public ModelResolverStrategy modelResolverStrategy() {
        return Mockito.mock(ModelResolverStrategy.class);
    }
}
