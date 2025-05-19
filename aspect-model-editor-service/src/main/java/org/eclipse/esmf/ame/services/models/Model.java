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

import org.eclipse.esmf.aspectmodel.urn.AspectModelUrn;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;

/**
 * Represents a single model with its name or properties.
 */
@Serdeable
@Introspected
public class Model {
   private String model;
   private AspectModelUrn aspectModelUrn;
   private boolean existing;

   public Model() {
   }

   public Model( final String model, final AspectModelUrn aspectModelUrn, final boolean existing ) {
      this.model = model;
      this.aspectModelUrn = aspectModelUrn;
      this.existing = existing;
   }

   public String getModel() {
      return model;
   }

   public void setModel( final String model ) {
      this.model = model;
   }

   public AspectModelUrn getAspectModelUrn() {
      return aspectModelUrn;
   }

   public void setAspectModelUrn( final AspectModelUrn aspectModelUrn ) {
      this.aspectModelUrn = aspectModelUrn;
   }

   public boolean isExisting() {
      return existing;
   }

   public void setExisting( final boolean existing ) {
      this.existing = existing;
   }
}