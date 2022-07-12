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
import org.springframework.web.bind.annotation.RestController;

import io.openmanufacturing.ame.services.GenerateService;

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
      return ResponseEntity.ok( generateService.generateHtmlDocument( aspectModel, Optional.empty() ) );
   }

   /**
    * This Method is used to generate a sample JSON Payload of the aspect model
    *
    * @param aspectModel The Aspect Model Data
    * @return The JSON Sample Payload
    */
   @PostMapping( "json-sample" )
   public ResponseEntity<Object> jsonSample( @RequestBody final String aspectModel ) {
      return ResponseEntity.ok( generateService.sampleJSONPayload( aspectModel, Optional.empty() ) );
   }

   /**
    * This Method is used to generate a JSON Schema of the aspect model
    *
    * @param aspectModel The Aspect Model Data
    * @return The JSON Schema
    */
   @PostMapping( "json-schema" )
   public ResponseEntity<String> jsonSchema( @RequestBody final String aspectModel ) {
      return ResponseEntity.ok( generateService.jsonSchema( aspectModel, Optional.empty() ) );
   }
}
