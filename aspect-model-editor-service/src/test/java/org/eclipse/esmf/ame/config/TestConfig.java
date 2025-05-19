/*
 * Copyright (c) 2025 Robert Bosch Manufacturing Solutions GmbH
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

import java.nio.file.Path;

import org.eclipse.esmf.aspectmodel.loader.AspectModelLoader;
import org.eclipse.esmf.aspectmodel.resolver.FileSystemStrategy;
import org.eclipse.esmf.aspectmodel.validation.services.AspectModelValidator;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import jakarta.inject.Singleton;

@Factory
public class TestConfig {

   @Bean
   @Singleton
   public AspectModelValidator getAspectModelValidator() {
      return new AspectModelValidator();
   }

   @Bean
   @Singleton
   public AspectModelLoader aspectModelLoader() {
      return new AspectModelLoader( new FileSystemStrategy( modelPath() ) );
   }

   @Bean
   @Singleton
   public Path modelPath() {
      return Path.of( "src", "test", "resources", "services" ).toAbsolutePath();
   }
}