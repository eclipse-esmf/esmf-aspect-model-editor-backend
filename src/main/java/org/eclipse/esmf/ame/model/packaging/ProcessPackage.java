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

package org.eclipse.esmf.ame.model.packaging;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class ProcessPackage {
   @JsonProperty( "invalidFiles" )
   private List<String> invalidFiles;

   @JsonProperty( "validFiles" )
   private final List<ValidFile> validFiles = new ArrayList<>();

   @JsonProperty( "missingElements" )
   private final List<MissingElement> missingElements = new ArrayList<>();

   public ProcessPackage() {
   }

   public ProcessPackage( final List<String> invalidFiles ) {
      this.invalidFiles = invalidFiles;
   }

   public void addValidFiles( final ValidFile validFile ) {
      validFiles.add( validFile );
   }

   public void addMissingElement( final MissingElement missingElement ) {
      missingElements.add( missingElement );
   }
}
