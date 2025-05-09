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

import java.io.File;
import java.nio.file.Path;

import io.micronaut.context.annotation.ConfigurationProperties;
import jakarta.inject.Singleton;

/**
 * Application settings configuration class.
 * <p>
 * This class provides static methods to retrieve paths used in the application,
 * such as the models storage path and the end file path.
 * It uses system properties and predefined constants to construct these paths.
 * </p>
 */
@Singleton
@ConfigurationProperties( "setting" )
public class ApplicationSettings {
   private static final String USER_HOME = System.getProperty( "user.home" );

   private static final String ASPECT_MODEL_EDITOR_END_PATH = "aspect-model-editor";
   private static final String END_FILE_PATH = "models";

   private static final String ASPECT_MODEL_PATH = USER_HOME + File.separator + ASPECT_MODEL_EDITOR_END_PATH;
   private static final String META_MODEL_PATH = ASPECT_MODEL_PATH + File.separator + END_FILE_PATH;

   private ApplicationSettings() {
   }

   public static Path getMetaModelStoragePath() {
      return Path.of( META_MODEL_PATH );
   }

   public static Path getEndFilePath() {
      return Path.of( END_FILE_PATH );
   }
}
