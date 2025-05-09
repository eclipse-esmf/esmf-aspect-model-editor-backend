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

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;

/**
 * Represents a single model with its name or properties.
 */
@Serdeable
@Introspected
public class Model {
   private String model;
   private boolean existing;

   public Model() {
   }

   public Model( final String model, final boolean existing ) {
      this.model = model;
      this.existing = existing;
   }

   public String getModel() {
      return model;
   }

   public void setModel( final String model ) {
      this.model = model;
   }

   public boolean isExisting() {
      return existing;
   }

   public void setExisting( final boolean existing ) {
      this.existing = existing;
   }
}