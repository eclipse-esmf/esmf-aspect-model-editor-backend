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

package org.eclipse.esmf.ame.validation.model;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.esmf.aspectmodel.urn.AspectModelUrn;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotNull;

@Serdeable
@Introspected
public class ViolationError {

   @NotNull
   @JsonProperty( "message" )
   private String message;
   @JsonInclude( JsonInclude.Include.NON_NULL )
   @JsonProperty( "focusNode" )
   private AspectModelUrn focusNode;
   @JsonInclude( JsonInclude.Include.NON_NULL )
   @JsonProperty( "fix" )
   private List<String> fix = new ArrayList<>();
   @JsonInclude( JsonInclude.Include.NON_NULL )
   @JsonProperty( "errorCode" )
   private String errorCode;

   public ViolationError() {
   }

   public ViolationError( final String message ) {
      this.message = message;
   }

   public ViolationError( final String message, final AspectModelUrn focusNode, final List<String> fix, final String errorCode ) {
      this.message = message;
      this.focusNode = focusNode;
      this.fix = fix != null ? fix : new ArrayList<>();
      this.errorCode = errorCode;
   }

   public String getMessage() {
      return message;
   }

   public void setMessage( final String message ) {
      this.message = message;
   }

   public AspectModelUrn getFocusNode() {
      return focusNode;
   }

   public void setFocusNode( final AspectModelUrn focusNode ) {
      this.focusNode = focusNode;
   }

   public List<String> getFix() {
      return fix;
   }

   public void setFix( final List<String> fix ) {
      this.fix = fix != null ? fix : new ArrayList<>();
   }

   public void addFix( final String fix ) {
      this.fix.add( fix );
   }

   public String getErrorCode() {
      return errorCode;
   }

   public void setErrorCode( final String errorCode ) {
      this.errorCode = errorCode;
   }
}
