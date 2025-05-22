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

package org.eclipse.esmf.ame.api;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.esmf.ame.exceptions.FileNotFoundException;
import org.eclipse.esmf.ame.exceptions.FileReadException;
import org.eclipse.esmf.ame.services.PackageService;
import org.eclipse.esmf.ame.services.models.Version;
import org.eclipse.esmf.ame.utils.ModelUtils;

import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Consumes;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.Part;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.multipart.CompletedFileUpload;
import org.apache.commons.io.FilenameUtils;

/**
 * Controller class that supports the importing and exporting of the Aspect Model packages.
 */
@Controller( "package" )

public class PackageController {
   public static final String URN = "aspect-model-urn";

   private final PackageService packageService;

   public PackageController( final PackageService packageService ) {
      this.packageService = packageService;
   }

   /**
    * Exports an Aspect Model package based on the provided URN.
    *
    * @param urn - HTTP headers containing the URN of the Aspect Model.
    * @return A HttpResponse containing the exported package as a byte array.
    */
   @Get( "/export" )
   @Produces( MediaType.APPLICATION_ZIP )
   public HttpResponse<byte[]> exportPackage( @Header( URN ) final Optional<String> urn ) {
      final Optional<String> optionalUrn = urn.map( ModelUtils::sanitizeFileInformation );

      final String aspectModelUrn = optionalUrn.orElseThrow( () -> new FileNotFoundException( "Please specify an aspect model urn" ) );

      return HttpResponse.ok().header( HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=package.zip" )
            .body( packageService.exportPackage( aspectModelUrn ) ).contentType( MediaType.APPLICATION_ZIP );
   }

   /**
    * Validates a ZIP file containing Aspect Model files.
    *
    * @param zipFile The uploaded ZIP file containing Aspect Model files.
    * @return An HttpResponse containing a list of maps, where each map represents a validated file with its details.
    * @throws FileReadException if the uploaded file is not in ZIP format.
    */
   @Post( "/validate-package" )
   @Consumes( MediaType.MULTIPART_FORM_DATA )
   public HttpResponse<List<Map<String, String>>> validatePackage( @Part( "zipFile" ) final CompletedFileUpload zipFile ) {
      final String extension = FilenameUtils.getExtension( zipFile.getFilename() );

      if ( !Objects.requireNonNull( extension ).equals( "zip" ) ) {
         throw new FileReadException( "The file you selected is not in ZIP format." );
      }

      return HttpResponse.ok( packageService.validatePackage( zipFile ) );
   }

   /**
    * Imports a zip file containing Aspect Models.
    *
    * @param zipFile - The zip file containing Aspect Model files.
    * @return A HttpResponse indicating the result of the import operation.
    * @throws FileReadException if the uploaded file is not in ZIP format.
    */
   @Post( "/import" )
   @Consumes( MediaType.MULTIPART_FORM_DATA )
   public HttpResponse<Map<String, List<Version>>> importPackage( @Part( "zipFile" ) final CompletedFileUpload zipFile ) {
      final String extension = FilenameUtils.getExtension( zipFile.getFilename() );

      if ( !Objects.requireNonNull( extension ).equals( "zip" ) ) {
         throw new FileReadException( "The file you selected is not in ZIP format." );
      }

      return HttpResponse.ok( packageService.importPackage( zipFile ) );
   }

   /**
    * Backs up workspace files into a package.
    *
    * @return HttpStatus 200 if the backup succeeded.
    */
   @Get( "/backup-workspace" )
   public HttpResponse<String> backupWorkspace() {
      packageService.backupWorkspace();
      return HttpResponse.status( HttpStatus.CREATED );
   }
}
