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

package io.openmanufacturing.ame.services.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class ProcessedExportedPackage {
   @JsonProperty("missingFiles")
   private final List<MissingFileInfo> missingFiles = new ArrayList<>();

   @JsonProperty("correctFiles")
   private final List<FileInformation> correctFiles = new ArrayList<>();

   public void addFileInformation( final FileInformation fileInformation ) {
      correctFiles.add( fileInformation );
   }

   public void addMissingFiles( final MissingFileInfo missingFiles ) {
      this.missingFiles.add( missingFiles );
   }
}
