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

import java.io.File;
import java.nio.file.Path;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties( prefix = "setting" )
public class ApplicationSettings {
   private static final String USER_HOME = System.getProperty( "user.home" );
   private static final String ASPECT_MODEL_EDITOR_END_PATH = "aspect-model-editor";
   private static final String END_FILE_PATH = "models";
   private static final String IMPORT_END_PATH = "packages-to-import";
   private static final String EXPORT_END_PATH = "packages-to-export";

   private static final String MIGRATION_END_PATH = "migration";
   private static final String ASPECT_MODEL_PATH = USER_HOME + File.separator + ASPECT_MODEL_EDITOR_END_PATH;
   private static final String META_MODEL_PATH = ASPECT_MODEL_PATH + File.separator + END_FILE_PATH;
   private static final String IMPORT_PACKAGE_PATH = ASPECT_MODEL_PATH + File.separator + IMPORT_END_PATH;
   private static final String EXPORT_PACKAGE_PATH = ASPECT_MODEL_PATH + File.separator + EXPORT_END_PATH;

   private static final String MIGRATION_PATH = ASPECT_MODEL_PATH + File.separator + MIGRATION_END_PATH;

   private String fileType = ".ttl";

   public static Path getAspectModelEditorStoragePath() {
      return Path.of( ASPECT_MODEL_PATH );
   }

   public static Path getMetaModelStoragePath() {
      return Path.of( META_MODEL_PATH );
   }

   public static Path getImportPackageStoragePath() {
      return Path.of( IMPORT_PACKAGE_PATH );
   }

   public static Path getExportPackageStoragePath() {
      return Path.of( EXPORT_PACKAGE_PATH );
   }

   public static Path getMigrationStoragePath() {
      return Path.of( MIGRATION_PATH );
   }

   public Path getEndFilePath() {
      return Path.of( END_FILE_PATH );
   }

   public String getFileType() {
      return fileType;
   }

   public void setFileType( final String fileType ) {
      this.fileType = fileType;
   }
}
