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

import org.eclipse.esmf.ame.model.validation.ViolationReport;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ValidFile {
   @JsonProperty( "namespace" )
   private final String namespace;

   @JsonProperty( "fileName" )
   private final String fileName;

   @JsonProperty( "violationReport" )
   private final ViolationReport violationReport;

   @JsonInclude( JsonInclude.Include.NON_NULL )
   @JsonProperty( "fileAlreadyDefined" )
   private Boolean fileAlreadyDefined;

   public ValidFile( final String namespace, final String fileName, final ViolationReport violationReport ) {
      this.namespace = namespace;
      this.fileName = fileName;
      this.violationReport = violationReport;
   }
}
