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

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.eclipse.esmf.ame.api.ModelController;
import org.eclipse.esmf.ame.exceptions.CreateFileException;
import org.eclipse.esmf.ame.exceptions.ResponseExceptionHandler;
import org.eclipse.esmf.ame.model.StoragePath;
import org.eclipse.esmf.ame.repository.strategy.LocalFolderResolverStrategy;
import org.eclipse.esmf.ame.repository.strategy.ModelResolverStrategy;
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

import com.github.marschall.memoryfilesystem.MemoryFileSystemBuilder;

/**
 * Configuration class for setting up various application-level beans and configurations.
 * This class primarily sets up properties, file systems, model paths, and CORS mappings.
 */
@Configuration
@ComponentScan( basePackageClasses = { ResponseExceptionHandler.class, ModelController.class } )
@EnableConfigurationProperties( ApplicationSettings.class )
public class ApplicationConfig implements WebMvcConfigurer {

   private final ApplicationSettings applicationSettings;
   private final Environment environment;
   private FileSystem importFileSystem;

   /**
    * Constructs an instance of ApplicationConfig with the provided settings and environment.
    *
    * @param applicationSettings The settings of the application.
    * @param environment The environment the application is running in.
    */
   public ApplicationConfig( final ApplicationSettings applicationSettings, final Environment environment ) {
      this.applicationSettings = applicationSettings;
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
    * Creates and returns an in-memory file system for imports.
    *
    * @return a new or existing in-memory file system.
    */
   @Bean
   public FileSystem importFileSystem() {
      if ( importFileSystem == null ) {
         try {
            importFileSystem = MemoryFileSystemBuilder.newEmpty().build();
         } catch ( IOException e ) {
            throw new CreateFileException( "Failed to create in-memory import file system.", e );
         }
      }
      return importFileSystem;
   }

   /**
    * Determines and returns the path for models based on the environment profile.
    *
    * @return the absolute path to the models.
    */
   @Bean
   public String modelPath() {
      if ( environment.acceptsProfiles( Profiles.of( "test" ) ) ) {
         return Path.of( "src", "test", "resources", "services" ).toAbsolutePath().toString();
      }

      return StoragePath.MetaModel.getPath().toString();
   }

   /**
    * Creates a list of model resolver strategies with settings and file systems configured.
    *
    * @return a list containing an instance of LocalFolderResolverStrategy.
    */
   @Bean
   public List<ModelResolverStrategy> modelStrategies() {
      return Collections.singletonList(
            new LocalFolderResolverStrategy( applicationSettings, importFileSystem(), modelPath() ) );
   }
}
