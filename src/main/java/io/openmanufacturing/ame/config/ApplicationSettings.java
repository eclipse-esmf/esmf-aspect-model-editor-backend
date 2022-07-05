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

import java.io.File;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties( prefix = "setting" )
public class ApplicationSettings {
   private static final String USER_HOME = System.getProperty( "user.home" );
   private static final String ASPECT_MODEL_EDITOR_END_PATH = "aspect-model-editor";
   private static final String END_FILE_PATH = "models";
   private static final String IMPORT_END_PATH = "packages-to-import";
   private static final String EXPORT_END_PATH = "packages-to-export";
   private static final String ASPECT_MODEL_PATH = USER_HOME + File.separator + ASPECT_MODEL_EDITOR_END_PATH;
   private static final String META_MODEL_PATH = ASPECT_MODEL_PATH + File.separator + END_FILE_PATH;
   private static final String IMPORT_PACKAGE_PATH = ASPECT_MODEL_PATH + File.separator + IMPORT_END_PATH;
   private static final String EXPORT_PACKAGE_PATH = ASPECT_MODEL_PATH + File.separator + EXPORT_END_PATH;

   private Boolean local;
   private String fileType;

   public Boolean getLocal() {
      return local;
   }

   public void setLocal( final Boolean local ) {
      this.local = local;
   }

   public static String getMetaModelStoragePath() {
      return META_MODEL_PATH;
   }

   public static String getImportPackageStoragePath() {
      return IMPORT_PACKAGE_PATH;
   }

   public static String getExportPackageStoragePath() {
      return EXPORT_PACKAGE_PATH;
   }

   public String getEndFilePath() {
      return END_FILE_PATH;
   }

   public String getFileType() {
      return fileType;
   }

   public void setFileType( final String fileType ) {
      this.fileType = fileType;
   }
}
