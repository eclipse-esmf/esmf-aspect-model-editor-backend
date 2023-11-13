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

package org.eclipse.esmf.ame.exceptions.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@JsonIgnoreProperties( ignoreUnknown = true )
@JsonInclude( JsonInclude.Include.NON_EMPTY )
public class ErrorResponse {
   private final Error error;

   public ErrorResponse( final ErrorResponse.Error error ) {
      this.error = error;
   }

   @JsonCreator
   public ErrorResponse( @JsonProperty( "message" ) final String message, @JsonProperty( "path" ) final String path,
         @JsonProperty( "code " ) final int code ) {
      this( new ErrorResponse.Error( message, path, code ) );
   }

   @Data
   @AllArgsConstructor
   @JsonIgnoreProperties( ignoreUnknown = true )
   @JsonInclude( JsonInclude.Include.NON_EMPTY )
   public static class Error {
      @NotEmpty
      private final String message;
      @NotEmpty
      private final String path;
      @NotEmpty
      private final int code;
   }
}
