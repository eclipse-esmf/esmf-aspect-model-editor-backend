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
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.esmf.ame.config.ApplicationSettings;
import org.eclipse.esmf.ame.exceptions.FileNotFoundException;
import org.eclipse.esmf.ame.exceptions.FileReadException;
import org.eclipse.esmf.ame.services.PackageService;
import org.eclipse.esmf.ame.services.models.Version;
import org.eclipse.esmf.ame.utils.ModelUtils;

import org.apache.commons.io.FilenameUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Controller class that supports the importing and exporting of the Aspect Model packages.
 */
@RestController
@RequestMapping( "package" )
public class PackageController {
   public static final String URN = "aspect-model-urn";

   private final PackageService packageService;

   public PackageController( final PackageService packageService ) {
      this.packageService = packageService;
   }

   /**
    * Exports an Aspect Model package based on the provided URN.
    *
    * @param headers - HTTP headers containing the URN of the Aspect Model.
    * @return A ResponseEntity containing the exported package as a byte array.
    */
   @GetMapping( "/export" )
   public ResponseEntity<byte[]> exportPackage( @RequestHeader final Map<String, String> headers ) {
      final Optional<String> optionalUrn = Optional.of(
            ModelUtils.sanitizeFileInformation( headers.get( URN ) ) );

      final String aspectModelUrn = optionalUrn.orElseThrow(
            () -> new FileNotFoundException( "Please specify an aspect model urn" ) );

      return ResponseEntity.ok()
            .header( HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=package.zip" )
            .header( HttpHeaders.CONTENT_TYPE, "application/zip" )
            .body( packageService.exportPackage( aspectModelUrn ) );
   }

   @GetMapping( "/check-import" )
   public ResponseEntity<Map<String, List<Version>>> checkImportPackage( @RequestParam( "zipFile" ) final MultipartFile zipFile ) {
      final String extension = FilenameUtils.getExtension( zipFile.getOriginalFilename() );

      if ( !Objects.requireNonNull( extension ).equals( "zip" ) ) {
         throw new FileReadException( "The file you selected is not in ZIP format." );
      }

      return ResponseEntity.ok( packageService.checkImportPackage( zipFile, ApplicationSettings.getMetaModelStoragePath() ) );
   }

   /**
    * Imports a zip file containing Aspect Models.
    *
    * @param zipFile - The zip file containing Aspect Model files.
    * @param filesToImport a list of file names to import from the zip file
    * @return A ResponseEntity indicating the result of the import operation.
    */
   @GetMapping( "/import" )
   public ResponseEntity<Map<String, List<Version>>> importPackage( @RequestParam( "zipFile" ) final MultipartFile zipFile,
         @RequestPart( "filesToImport" ) final List<String> filesToImport ) {
      final String extension = FilenameUtils.getExtension( zipFile.getOriginalFilename() );

      if ( !Objects.requireNonNull( extension ).equals( "zip" ) ) {
         throw new FileReadException( "The file you selected is not in ZIP format." );
      }

      if ( filesToImport == null ) {
         throw new NullPointerException( "Files to import should be set." );
      }

      return ResponseEntity.ok( packageService.importPackage( zipFile, filesToImport, ApplicationSettings.getMetaModelStoragePath() ) );
   }

   /**
    * Backs up workspace files into a package.
    *
    * @return HttpStatus 200 if the backup succeeded.
    */
   @GetMapping( path = "/backup-workspace" )
   public ResponseEntity<String> backupWorkspace() {
      packageService.backupWorkspace();
      return ResponseEntity.ok().build();
   }
}
