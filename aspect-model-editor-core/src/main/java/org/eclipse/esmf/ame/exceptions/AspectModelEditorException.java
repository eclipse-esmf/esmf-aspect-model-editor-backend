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

/**
 * Base exception for all Aspect Model Editor specific exceptions.
 * Provides consistent error handling with HTTP status codes.
 */
public abstract class AspectModelEditorException extends RuntimeException {
   @Serial
   private static final long serialVersionUID = 1L;

   private final int httpStatusCode;

   /**
    * Constructs a new exception with the specified detail message and HTTP status code.
    *
    * @param message the detail message
    * @param httpStatusCode the HTTP status code associated with this exception
    */
   protected AspectModelEditorException( final String message, final int httpStatusCode ) {
      super( message );
      this.httpStatusCode = httpStatusCode;
   }

   /**
    * Constructs a new exception with the specified detail message, cause, and HTTP status code.
    *
    * @param message the detail message
    * @param cause the cause of this exception
    * @param httpStatusCode the HTTP status code associated with this exception
    */
   protected AspectModelEditorException( final String message, final Throwable cause, final int httpStatusCode ) {
      super( message, cause );
      this.httpStatusCode = httpStatusCode;
   }

   /**
    * Returns the HTTP status code associated with this exception.
    *
    * @return the HTTP status code
    */
   public int getHttpStatusCode() {
      return httpStatusCode;
   }
}

