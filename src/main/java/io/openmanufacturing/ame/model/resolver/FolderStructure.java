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

package io.openmanufacturing.ame.model.resolver;

import java.io.File;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FolderStructure {
   private String fileRootPath;
   private String version;
   private String fileName;

   public FolderStructure() {
   }

   public FolderStructure( final String fileRootPath ) {
      this.fileRootPath = fileRootPath;
   }

   public FolderStructure( final String fileRootPath, final String version ) {
      this.fileRootPath = fileRootPath;
      this.version = version;
   }

   @Override
   public String toString() {
      if ( fileName != null ) {
         return fileRootPath + File.separator + version + File.separator + fileName;
      }

      if ( version != null ) {
         return fileRootPath + File.separator + version;
      }

      return fileRootPath;
   }
}
