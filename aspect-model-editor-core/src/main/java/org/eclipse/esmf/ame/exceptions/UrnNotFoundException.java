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

import org.eclipse.esmf.aspectmodel.urn.AspectModelUrn;

import io.micronaut.http.HttpStatus;

/**
 * Exception thrown when an Aspect Model URN cannot be found.
 */
public class UrnNotFoundException extends AspectModelEditorException {
   @Serial
   private static final long serialVersionUID = 1L;
   private final transient AspectModelUrn urn;

   /**
    * Constructs a UrnNotFoundException with message and the URN that was not found.
    *
    * @param message the message of the exception
    * @param urn Not found AspectModelUrn
    */
   public UrnNotFoundException( final String message, final AspectModelUrn urn ) {
      super( message, HttpStatus.NOT_FOUND.getCode() );
      this.urn = urn;
   }

   public AspectModelUrn getUrn() {
      return urn;
   }
}
