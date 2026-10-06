/*
 * Copyright (c) 2026 Robert Bosch Manufacturing Solutions GmbH
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
package org.eclipse.esmf.ame.exceptions;

import java.io.Serial;

import org.eclipse.esmf.ame.model.ReferenceReport;

import io.micronaut.http.HttpStatus;

/**
 * Thrown when a namespace or an Aspect Model file cannot be deleted because other files still use its elements or could not be checked.
 */
public class ModelReferencedException extends AspectModelEditorException {
   @Serial
   private static final long serialVersionUID = 1L;

   private final transient ReferenceReport report;

   public ModelReferencedException( final String message, final ReferenceReport report ) {
      super( message, HttpStatus.CONFLICT.getCode() );
      this.report = report;
   }

   public ReferenceReport getReport() {
      return report;
   }
}
