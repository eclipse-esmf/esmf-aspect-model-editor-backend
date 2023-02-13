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

package io.openmanufacturing.ame.model.validation;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.NonNull;

@Data
public class ViolationReport {
   @NonNull
   @JsonProperty( "violationErrors" )
   private List<ViolationError> violationErrors;

   public ViolationReport() {
      violationErrors = new ArrayList<>();
   }

   public void addViolation( final ViolationError violationError ) {
      violationErrors.add( violationError );
   }
}
