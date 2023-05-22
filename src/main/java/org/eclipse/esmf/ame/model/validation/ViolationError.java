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

package org.eclipse.esmf.ame.model.validation;

import java.util.List;

import org.eclipse.esmf.aspectmodel.urn.AspectModelUrn;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class ViolationError {
   @NonNull
   @JsonProperty( "message" )
   private String message;

   @JsonInclude( JsonInclude.Include.NON_NULL )
   @JsonProperty( "focusNode" )
   private AspectModelUrn focusNode;

   @JsonInclude( JsonInclude.Include.NON_NULL )
   @JsonProperty( "fix" )
   private List<String> fix;

   @JsonInclude( JsonInclude.Include.NON_NULL )
   @JsonProperty( "errorCode" )
   private String errorCode;

   public void addFix( final String fix ) {
      this.fix.add( fix );
   }
}
