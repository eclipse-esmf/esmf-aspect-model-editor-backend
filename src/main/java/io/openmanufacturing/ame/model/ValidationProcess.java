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

package io.openmanufacturing.ame.model;

import java.nio.file.Path;
import java.util.Arrays;

import io.openmanufacturing.ame.config.ApplicationSettings;

public enum ValidationProcess {
   VALIDATION( ApplicationSettings.getMetaModelStoragePath() ),
   EXPORT( ApplicationSettings.getExportPackageStoragePath() ),
   IMPORT( ApplicationSettings.getImportPackageStoragePath() ),
   GENERATE( ApplicationSettings.getMetaModelStoragePath() ),
   MIGRATE( ApplicationSettings.getMetaModelStoragePath() ),
   MODELS( ApplicationSettings.getMetaModelStoragePath() );

   private final Path path;

   ValidationProcess( final Path path ) {
      this.path = path;
   }

   public Path getPath() {
      return path;
   }

   public static ValidationProcess getEnum( final String value ) {
      return Arrays.stream( values() )
                   .filter( v -> v.getPath().toString().equalsIgnoreCase( value ) )
                   .findFirst()
                   .orElseThrow( IllegalArgumentException::new );
   }
}
