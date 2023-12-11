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

package org.eclipse.esmf.ame.services.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class FileValidationReport {
   @JsonProperty( "validFiles" )
   private final List<NamespaceFileReport> namespaceFileReports = new ArrayList<>();

   @JsonProperty( "missingElements" )
   private final List<ElementMissingReport> elementMissingReports = new ArrayList<>();

   public FileValidationReport() {
   }

   public FileValidationReport( NamespaceFileReport namespaceFileReport, List<ElementMissingReport> missingFiles ) {
      namespaceFileReports.add( namespaceFileReport );
      elementMissingReports.addAll( missingFiles );
   }

   public void addValidFile( final NamespaceFileReport namespaceFileReport ) {
      namespaceFileReports.add( namespaceFileReport );
   }

   public void addMissingElement( final ElementMissingReport elementMissingReport ) {
      elementMissingReports.add( elementMissingReport );
   }

   public FileValidationReport merge( FileValidationReport other ) {
      // Create a new instance of ProcessPackage
      FileValidationReport mergedPackage = new FileValidationReport();

      // Merge the valid files
      mergedPackage.getNamespaceFileReports().addAll( this.getNamespaceFileReports() );
      mergedPackage.getNamespaceFileReports().addAll( other.getNamespaceFileReports() );

      // Merge the missing elements
      mergedPackage.getElementMissingReports().addAll( this.getElementMissingReports() );
      mergedPackage.getElementMissingReports().addAll( other.getElementMissingReports() );

      return mergedPackage;
   }
}
