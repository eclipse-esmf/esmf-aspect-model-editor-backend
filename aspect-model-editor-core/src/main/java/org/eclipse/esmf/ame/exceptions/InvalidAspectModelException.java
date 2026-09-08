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

import io.micronaut.http.HttpStatus;

/**
 * Exception thrown when an aspect model is syntactically or semantically invalid.
 * Results in HTTP 409 Conflict response.
 */
public class InvalidAspectModelException extends AspectModelEditorException {
   @Serial
   private static final long serialVersionUID = 1L;

   /**
    * Constructs a new InvalidAspectModelException with the specified detail message.
    *
    * @param message the detail message explaining why the model is invalid
    */
   public InvalidAspectModelException( final String message ) {
      super( message, HttpStatus.CONFLICT.getCode() );
   }

   /**
    * Constructs a new InvalidAspectModelException with the specified detail message and cause.
    *
    * @param message the detail message explaining why the model is invalid
    * @param cause the cause of this exception
    */
   public InvalidAspectModelException( final String message, final Throwable cause ) {
      super( message, cause, HttpStatus.CONFLICT.getCode() );
   }
}
