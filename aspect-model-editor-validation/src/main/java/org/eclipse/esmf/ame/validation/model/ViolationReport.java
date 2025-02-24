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

package org.eclipse.esmf.ame.validation.model;

import java.util.ArrayList;
import java.util.List;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotNull;

@Serdeable
@Introspected
public class ViolationReport {

   @NotNull
   private List<ViolationError> violationErrors;

   public ViolationReport() {
      this.violationErrors = new ArrayList<>();
   }

   public void addViolation( final ViolationError violationError ) {
      this.violationErrors.add( violationError );
   }

   public @NotNull List<ViolationError> getViolationErrors() {
      return violationErrors;
   }

   public void setViolationErrors( @NotNull final List<ViolationError> violationErrors ) {
      this.violationErrors = violationErrors;
   }
}