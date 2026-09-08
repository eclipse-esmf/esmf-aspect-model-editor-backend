/*
 * Copyright (c) 2026 Robert Bosch Manufacturing Solutions GmbH
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.eclipse.esmf.ame.constants.ApplicationConstants;
import org.eclipse.esmf.ame.exceptions.CreateFileException;

import io.micronaut.context.annotation.ConfigurationProperties;
import jakarta.inject.Singleton;
import org.apache.commons.io.FileUtils;

/**
 * Application settings configuration class.
 * <p>
 * This class provides static methods to retrieve paths used in the application,
 * such as the models storage path. It uses system properties and constants to construct these paths.
 * </p>
 */
@Singleton
@ConfigurationProperties( "setting" )
public class ApplicationSettings {
   private static final String USER_HOME = System.getProperty( "user.home" );
   private static final String ASPECT_MODEL_PATH = USER_HOME + File.separator + ApplicationConstants.Directories.ASPECT_MODEL_EDITOR;
   private static final String META_MODEL_PATH = ASPECT_MODEL_PATH + File.separator + ApplicationConstants.Directories.MODELS;

   private ApplicationSettings() {
   }

   /**
    * Gets the path for storing aspect model metadata.
    * Creates the directory structure if it doesn't exist.
    *
    * @return the path to the metadata storage directory
    * @throws CreateFileException if the directory cannot be created
    */
   public static Path getMetaModelStoragePath() {
      try {
         final Path modelPath = Path.of( META_MODEL_PATH );
         FileUtils.forceMkdir( modelPath.toFile() );
         return modelPath;
      } catch ( final IOException e ) {
         throw new CreateFileException( String.format(
               "Unable to create the meta model storage directory at: %s. Please check your permissions or the validity of the path.",
               META_MODEL_PATH ), e );
      }
   }
}
