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

package org.eclipse.esmf.ame.exceptions;

import java.io.IOException;
import java.io.Serial;

import org.eclipse.esmf.aspectmodel.urn.AspectModelUrn;

public class UrnNotFoundException extends IOException {
   @Serial
   private static final long serialVersionUID = 1L;
   private final transient AspectModelUrn urn;

   /**
    * Constructs a UrnFoundException with message, cause and value.
    *
    * @param message the message of the exception
    * @param urn Not found AspectModelUrn
    */
   public UrnNotFoundException( final String message, final AspectModelUrn urn ) {
      super( message );
      this.urn = urn;
   }

   public AspectModelUrn getUrn() {
      return urn;
   }
}
