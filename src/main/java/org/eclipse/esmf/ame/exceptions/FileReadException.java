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

package org.eclipse.esmf.ame.exceptions;

public class FileReadException extends RuntimeException {
   private static final long serialVersionUID = 1L;

   /**
    * Constructs a FileReadException with message.
    *
    * @param message the message of the exception
    */
   public FileReadException( final String message ) {
      super( message );
   }

   /**
    * Constructs a FileReadException with message and cause.
    *
    * @param message the message of the exception
    * @param cause of the exception
    */
   public FileReadException( final String message, final Throwable cause ) {
      super( message, cause );
   }
}
