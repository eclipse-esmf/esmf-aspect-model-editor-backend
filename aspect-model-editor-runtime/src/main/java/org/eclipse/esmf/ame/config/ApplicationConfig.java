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
import io.micronaut.http.annotation.Controller;
import io.micronaut.runtime.http.scope.RequestScope;
import jakarta.inject.Singleton;

/**
 * Configuration class for setting up various application-level beans and configurations.
 * This class primarily sets up properties, file systems, model paths, and CORS mappings.
 */
@Factory
public class ApplicationConfig {

   /**
    * Constructs an instance of ApplicationConfig with the provided settings and environment.
    */
   public ApplicationConfig() {
   }

   /**
    * Configures CORS mappings for the application, allowing specific HTTP methods.
    */
   @Controller
   @RequestScope
   public static class CorsController {
      @io.micronaut.http.annotation.Options( "/**" )
      public void configureCors() {
         // Allow specific HTTP methods (Micronaut handles this via filters or controller annotations)
      }
   }

   /**
    * Creates a bean of {@link AspectModelValidator} with JavaScript evaluations disabled.
    *
    * @return a new instance of AspectModelValidator.
    */
   @Bean
   @Singleton
   public AspectModelValidator getAspectModelValidator() {
      return new AspectModelValidator();
   }

   /**
    * Determines and returns the path for models based on the environment profile.
    *
    * @return the absolute path to the models.
    */
   @Bean
   @Singleton
   public Path modelPath() {
      return ApplicationSettings.getMetaModelStoragePath();
   }

   /**
    * Creates a bean of {@link AspectModelLoader} using a {@link FileSystemStrategy} based on the model path.
    *
    * @return a new instance of {@link AspectModelLoader}.
    */
   @Bean
   public AspectModelLoader aspectModelLoader() {
      return new AspectModelLoader( new FileSystemStrategy( modelPath() ) );
   }
}
