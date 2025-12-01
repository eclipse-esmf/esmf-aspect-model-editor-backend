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

package org.eclipse.esmf.ame.services.models;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;

/**
 * Represents a single migration result with success and erros.
 */
@Serdeable
@Introspected
public class FileEntry {
   private String absoluteName;
   private String fileName;
   private String aspectModelUrn;
   private String modelVersion;

   public FileEntry( final String aspectModelUrn ) {
      this.aspectModelUrn = aspectModelUrn;
   }

   public FileEntry( final String absoluteName, final String fileName, final String aspectModelUrn, final String modelVersion ) {
      this.absoluteName = absoluteName;
      this.fileName = fileName;
      this.aspectModelUrn = aspectModelUrn;
      this.modelVersion = modelVersion;
   }

   public String getAbsoluteName() {
      return absoluteName;
   }

   public void setAbsoluteName( final String absoluteName ) {
      this.absoluteName = absoluteName;
   }

   public String getFileName() {
      return fileName;
   }

   public void setFileName( final String fileName ) {
      this.fileName = fileName;
   }

   public String getAspectModelUrn() {
      return aspectModelUrn;
   }

   public void setAspectModelUrn( final String aspectModelUrn ) {
      this.aspectModelUrn = aspectModelUrn;
   }

   public String getModelVersion() {
      return modelVersion;
   }

   public void setModelVersion( final String modelVersion ) {
      this.modelVersion = modelVersion;
   }
}