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

import java.io.IOException;

import io.openmanufacturing.sds.aspectmodel.urn.AspectModelUrn;
import lombok.Getter;

@Getter
public class UrnNotFoundException extends IOException {
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

   /**
    * Constructs a UrnFoundException with message, cause and value.
    *
    * @param message the message of the exception
    * @param cause of the exception
    * @param urn Not found AspectModelUrn
    */
   public UrnNotFoundException( final String message, final Throwable cause, final AspectModelUrn urn ) {
      super( message, cause );
      this.urn = urn;
   }

   public AspectModelUrn getUrn() {
      return urn;
   }
}
