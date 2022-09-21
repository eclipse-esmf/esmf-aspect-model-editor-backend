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

package io.openmanufacturing.ame.services.model.packaging;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.openmanufacturing.sds.aspectmodel.validation.report.ValidationReport;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ValidFile {
   @JsonProperty( "aspectModelFileName" )
   private final String aspectModelFileName;

   @JsonProperty( "validationReport" )
   private final ValidationReport validationReport;

   @JsonInclude( JsonInclude.Include.NON_NULL )
   @JsonProperty( "fileAlreadyDefined" )
   private Boolean fileAlreadyDefined;

   public ValidFile( final String aspectModelFileName, final ValidationReport validationReport ) {
      this.aspectModelFileName = aspectModelFileName;
      this.validationReport = validationReport;
   }
}
