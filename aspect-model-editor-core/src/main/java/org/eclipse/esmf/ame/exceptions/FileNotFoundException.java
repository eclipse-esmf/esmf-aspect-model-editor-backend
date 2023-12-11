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

package org.eclipse.esmf.ame.exceptions;

import java.io.Serial;

import lombok.Getter;

@Getter
public class FileNotFoundException extends RuntimeException {
   @Serial
   private static final long serialVersionUID = 1L;

   /**
    * Constructs a FileNotFoundException with message and cause.
    *
    * @param message the message of the exception
    */
   public FileNotFoundException( final String message ) {
      super( message );
   }

   public FileNotFoundException( final String message, final Throwable cause ) {
      super( message, cause );
   }
}
