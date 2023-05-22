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

package org.eclipse.esmf.ame.model.migration;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Namespace {
   @JsonInclude( JsonInclude.Include.NON_NULL )
   @JsonProperty( "namespace" )
   public String versionedNamespace;

   @JsonInclude( JsonInclude.Include.NON_NULL )
   @JsonProperty( "files" )
   public List<FileInformation> files = new ArrayList<>();

   public Namespace( final String versionedNamespace ) {
      this.versionedNamespace = versionedNamespace;
   }

   public void addAspectModelFile( final FileInformation file ) {
      files.add( file );
   }
}
