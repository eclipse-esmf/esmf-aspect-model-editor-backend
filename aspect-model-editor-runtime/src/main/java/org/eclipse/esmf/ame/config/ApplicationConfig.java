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

import java.nio.file.FileSystem;
import java.nio.file.Path;

import org.eclipse.esmf.ame.api.ModelController;
import org.eclipse.esmf.ame.exceptions.ResponseExceptionHandler;
import org.eclipse.esmf.aspectmodel.loader.AspectModelLoader;
import org.eclipse.esmf.aspectmodel.resolver.FileSystemStrategy;
import org.eclipse.esmf.aspectmodel.shacl.constraint.JsConstraint;
import org.eclipse.esmf.aspectmodel.validation.services.AspectModelValidator;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration class for setting up various application-level beans and configurations.
 * This class primarily sets up properties, file systems, model paths, and CORS mappings.
 */
@Configuration
@ComponentScan( basePackageClasses = { ResponseExceptionHandler.class, ModelController.class } )
@EnableConfigurationProperties( ApplicationSettings.class )
public class ApplicationConfig implements WebMvcConfigurer {

   private final Environment environment;
   private FileSystem importFileSystem;

   /**
    * Constructs an instance of ApplicationConfig with the provided settings and environment.
    *
    * @param environment The environment the application is running in.
    */
   public ApplicationConfig( final Environment environment ) {
      this.environment = environment;
   }

   /**
    * Configures CORS mappings for the application, allowing specific HTTP methods.
    *
    * @param registry the CORS registry.
    */
   @Override
   public void addCorsMappings( final CorsRegistry registry ) {
      registry.addMapping( "/**" )
            .allowedMethods( "GET", "POST", "PUT", "DELETE" );
   }

   /**
    * Creates a bean of {@link AspectModelValidator} with JavaScript evaluations disabled.
    *
    * @return a new instance of AspectModelValidator.
    */
   @Bean
   public AspectModelValidator getAspectModelValidator() {
      // Spring and GraalVM cannot launch Javascript engines at the moment, so this must be disabled for now.
      JsConstraint.setEvaluateJavaScript( false );

      return new AspectModelValidator();
   }

   /**
    * Determines and returns the path for models based on the environment profile.
    *
    * @return the absolute path to the models.
    */
   @Bean
   public Path modelPath() {
      if ( environment.acceptsProfiles( Profiles.of( "test" ) ) ) {
         return Path.of( "src", "test", "resources", "services" ).toAbsolutePath();
      }

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
