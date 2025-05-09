/*
 * Copyright (c) 2025 Robert Bosch Manufacturing Solutions GmbH
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

package org.eclipse.esmf.ame.services.models;

import java.util.List;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;

/**
 * Represents a single migration result with success and erros.
 */
@Serdeable
@Introspected
public class MigrationResult {
   private boolean success;
   private List<String> errors;

   public MigrationResult() {
   }

   public MigrationResult( final boolean success, final List<String> errors ) {
      this.success = success;
      this.errors = errors;
   }

   public boolean isSuccess() {
      return success;
   }

   public void setSuccess( final boolean success ) {
      this.success = success;
   }

   public List<String> getErrors() {
      return errors;
   }

   public void setErrors( final List<String> errors ) {
      this.errors = errors;
   }
}