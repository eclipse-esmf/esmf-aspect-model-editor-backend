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

package org.eclipse.esmf.ame.web;

import java.io.IOException;
import java.util.Optional;

import org.eclipse.esmf.ame.model.ValidationProcess;
import org.eclipse.esmf.ame.services.GenerateService;
import org.eclipse.esmf.aspectmodel.generator.openapi.PagingOption;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
    * @param aspectModel the Aspect Model Data
    * @return the aspect model definition as documentation html file.
    */
   @PostMapping( "documentation" )
   public ResponseEntity<byte[]> generateHtml( @RequestBody final String aspectModel,
         @RequestParam( name = "language" ) final String language ) throws IOException {
      return ResponseEntity.ok(
            generateService.generateHtmlDocument( aspectModel, language, ValidationProcess.GENERATE ) );
   }

   /**
    * This Method is used to generate a sample JSON Payload of the aspect model
    *
    * @param aspectModel the Aspect Model Data
    * @return The JSON Sample Payload
    */
   @PostMapping( "json-sample" )
   public ResponseEntity<Object> jsonSample( @RequestBody final String aspectModel ) {
      return ResponseEntity.ok( generateService.sampleJSONPayload( aspectModel, ValidationProcess.GENERATE ) );
   }

   /**
    * This Method is used to generate a JSON Schema of the aspect model
    *
    * @param aspectModel The Aspect Model Data
    * @param language of the generated json schema
    * @return The JSON Schema
    */
   @PostMapping( "json-schema" )
   public ResponseEntity<String> jsonSchema( @RequestBody final String aspectModel,
         @RequestParam( name = "language", defaultValue = "en" ) final String language ) {
      return ResponseEntity.ok( generateService.jsonSchema( aspectModel, ValidationProcess.GENERATE, language ) );
   }

   /**
    * This method is used to generate an OpenAPI specification of the Aspect Model
    *
    * @param aspectModel the Aspect Model Data
    * @param language of the generated OpenAPI specification
    * @param output of the OpenAPI specification
    * @param baseUrl the base URL for the Aspect API
    * @param includeQueryApi if set to true, a path section for the Query API Endpoint of the Aspect API will be
    *       included in the specification
    * @param useSemanticVersion if set to true, the complete semantic version of the Aspect Model will be used as
    *       the version of the API, otherwise only the major part of the Aspect Version is used as the version of the
    *       API.
    * @param pagingOption if defined, the chosen paging type will be in the JSON.
    * @return The OpenAPI specification
    */
   @PostMapping( "open-api-spec" )
   public ResponseEntity<String> openApiSpec( @RequestBody final String aspectModel,
         @RequestParam( name = "language", defaultValue = "en" ) final String language,
         @RequestParam( name = "output", defaultValue = "yaml" ) final String output,
         @RequestParam( name = "baseUrl", defaultValue = "https://www.eclipse.org" ) final String baseUrl,
         @RequestParam( name = "includeQueryApi", defaultValue = "false" ) final boolean includeQueryApi,
         @RequestParam( name = "useSemanticVersion", defaultValue = "false" ) final boolean useSemanticVersion,
         @RequestParam( name = "pagingOption", defaultValue = "TIME_BASED_PAGING" )
         final Optional<PagingOption> pagingOption ) {

      final String openApiOutput = output.equals( "json" ) ?
            generateService.generateJsonOpenApiSpec( language, aspectModel, baseUrl, includeQueryApi,
                  useSemanticVersion,
                  pagingOption ) :
            generateService.generateYamlOpenApiSpec( language, aspectModel, baseUrl, includeQueryApi,
                  useSemanticVersion,
                  pagingOption );

      return ResponseEntity.ok( openApiOutput );
   }
}
