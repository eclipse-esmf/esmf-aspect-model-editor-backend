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
import java.util.List;

/**
 * Exception thrown when an aspect model is valid on its own, but references elements which no workspace file defines.
 * Results in HTTP 409 Conflict response which lists the unresolved elements, so that clients can still open the model.
 */
public class UnresolvedReferencesException extends InvalidAspectModelException {
   @Serial
   private static final long serialVersionUID = 1L;

   private final List<String> unresolvedElements;

   /**
    * @param message the detail message
    * @param unresolvedElements the URNs of the referenced elements which could not be resolved
    * @param cause the cause of this exception
    */
   public UnresolvedReferencesException( final String message, final List<String> unresolvedElements, final Throwable cause ) {
      super( message, cause );
      this.unresolvedElements = List.copyOf( unresolvedElements );
   }

   public List<String> getUnresolvedElements() {
      return unresolvedElements;
   }
}
