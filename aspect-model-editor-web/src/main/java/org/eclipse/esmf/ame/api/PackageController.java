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

package org.eclipse.esmf.ame.api;

import java.util.List;
import java.util.Objects;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.esmf.ame.MediaTypeExtension;
import org.eclipse.esmf.ame.exceptions.FileReadException;
import org.eclipse.esmf.ame.model.StoragePath;
import org.eclipse.esmf.ame.services.PackageService;
import org.eclipse.esmf.ame.services.model.FileValidationReport;
import org.eclipse.esmf.ame.services.model.NamespaceFileCollection;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Controller class that supports the importing and exporting of the Aspect Model packages.
 */
@RestController
@RequestMapping( "package" )
public class PackageController {
   private final PackageService packageService;

   public PackageController( final PackageService packageService ) {
      this.packageService = packageService;
   }

   /**
    * Method to validate a list of Aspect Models which are saved on local workspace.
    *
    * @param aspectModelFiles - a list of Aspect Model file names.
    * @return information which Aspect Models are valid/invalid or missing.
    */
   @PostMapping( "/validate-models-for-export" )
   public FileValidationReport validateAspectModelsForExport(
         @RequestBody final List<NamespaceFileCollection> aspectModelFiles ) {
      return packageService.validateAspectModelsForExport( aspectModelFiles );
   }

   /**
    * Method to validate imported zip
    *
    * @param zipFile zip file as multipart form data
    * @return The information is returned which Aspect Models have validation errors, are already defined and
    *       which files are not Aspect Models.
    */
   @PostMapping( "/validate-import-zip" )
   public ResponseEntity<FileValidationReport> validateImportAspectModelPackage(
         @RequestParam( "zipFile" ) final MultipartFile zipFile ) {
      final String extension = FilenameUtils.getExtension( zipFile.getOriginalFilename() );

      if ( !Objects.requireNonNull( extension ).equals( "zip" ) ) {
         throw new FileReadException("The file you selected is not in ZIP format.");
      }

      return ResponseEntity.ok( packageService.validateImportAspectModelPackage( zipFile ) );
   }

   /**
    * Method to import a zip file with Aspect Models.
    *
    * @param aspectModelFiles - a list of Aspect Model file names.
    * @return information which Aspect Models are valid/invalid or missing.
    */
   @PostMapping( "/import" )
   public ResponseEntity<List<String>> importAspectModelPackage(
         @RequestBody final List<NamespaceFileCollection> aspectModelFiles ) {
      return ResponseEntity.ok( packageService.importAspectModelPackage( aspectModelFiles ) );
   }

   /**
    * Method to export a zip file with Aspect Models.
    *
    * @return the zip file as byte array.
    */
   @GetMapping( path = "/export-zip", produces = MediaTypeExtension.APPLICATION_ZIP_VALUE )
   public ResponseEntity<byte[]> exportAspectModelPackage() {
      final String zipFileName = "package.zip";

      return ResponseEntity.ok()
                           .header( HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + zipFileName + "\"" )
                           .body( packageService.exportAspectModelPackage( zipFileName ) );
   }

   /**
    * Method to back up workspace files into package.
    *
    * @return HttpState 200 if the backup succeeded.
    */
   @GetMapping( path = "/backup-workspace" )
   public ResponseEntity<Void> backupWorkspace() {
      String aspectModelPath = StoragePath.AspectModel.getPath().toString();
      packageService.backupWorkspace( aspectModelPath );
      return new ResponseEntity<>( HttpStatus.CREATED );
   }
}
