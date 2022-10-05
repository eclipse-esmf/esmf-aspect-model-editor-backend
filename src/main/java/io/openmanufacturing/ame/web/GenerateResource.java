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

package io.openmanufacturing.ame.web;

import java.io.IOException;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.openmanufacturing.ame.services.GenerateService;
import io.openmanufacturing.sds.aspectmodel.generator.openapi.PagingOption;

/**
 * Controller class that supports the generation of the aspect model into other formats.
 */
@RestController
@RequestMapping( "generate" )
public class GenerateResource {

   private final GenerateService generateService;

   public GenerateResource( final GenerateService generateService ) {
      this.generateService = generateService;
   }

   /**
    * This Method is used to generate a documentation of the aspect model
    *
    * @param aspectModel - The Aspect Model Data
    * @return the aspect model definition as documentation html file.
    */
   @PostMapping( "documentation" )
   public ResponseEntity<byte[]> generateHtml( @RequestBody final String aspectModel ) throws IOException {
      return ResponseEntity.ok( generateService.generateHtmlDocument( aspectModel ) );
   }

   /**
    * This Method is used to generate a sample JSON Payload of the aspect model
    *
    * @param aspectModel The Aspect Model Data
    * @return The JSON Sample Payload
    */
   @PostMapping( "json-sample" )
   public ResponseEntity<Object> jsonSample( @RequestBody final String aspectModel ) {
      return ResponseEntity.ok( generateService.sampleJSONPayload( aspectModel ) );
   }

   /**
    * This Method is used to generate a JSON Schema of the aspect model
    *
    * @param aspectModel The Aspect Model Data
    * @return The JSON Schema
    */
   @PostMapping( "json-schema" )
   public ResponseEntity<String> jsonSchema( @RequestBody final String aspectModel ) {
      return ResponseEntity.ok( generateService.jsonSchema( aspectModel ) );
   }

   /**
    * This Method is used to generate an open api specification of the aspect model
    *
    * @param aspectModel The Aspect Model Data
    * @return The open api specification
    */
   @PostMapping( "open-api-spec" )
   public ResponseEntity<String> openApiSpec( @RequestBody final String aspectModel,
         @RequestParam( name = "json", defaultValue = "false" ) final boolean jsonOutput,
         @RequestParam( name = "baseUrl", defaultValue = "https://www.example.com" ) final String baseUrl,
         @RequestParam( name = "includeQueryApi", defaultValue = "false" ) final boolean includeQueryApi,
         @RequestParam( name = "useSemanticVersion", defaultValue = "false" ) final boolean useSemanticVersion,
         @RequestParam( name = "pagingOption", defaultValue = "TIME_BASED_PAGING" )
         final Optional<PagingOption> pagingOption ) {

      final String openApiOutput = jsonOutput ?
            generateService.generateJsonOpenApiSpec( aspectModel, baseUrl, includeQueryApi, useSemanticVersion,
                  pagingOption ) :
            generateService.generateYamlOpenApiSpec( aspectModel, baseUrl, includeQueryApi, useSemanticVersion,
                  pagingOption );

      return ResponseEntity.ok( openApiOutput );
   }
}
