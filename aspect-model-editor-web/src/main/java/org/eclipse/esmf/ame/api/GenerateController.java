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

import java.io.IOException;
import java.util.Locale;
import java.util.Optional;

import org.eclipse.esmf.ame.services.GenerateService;
import org.eclipse.esmf.aspectmodel.generator.openapi.OpenApiSchemaGenerationConfig;
import org.eclipse.esmf.aspectmodel.generator.openapi.PagingOption;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * Controller class that supports the generation of the aspect model into other formats.
 */
@RestController
@RequestMapping( "generate" )
public class GenerateController {

   private final GenerateService generateService;

   public GenerateController( final GenerateService generateService ) {
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
      return ResponseEntity.ok( generateService.generateHtmlDocument( aspectModel, language ) );
   }

   /**
    * This Method is used to generate a sample JSON Payload of the aspect model
    *
    * @param aspectModel the Aspect Model Data
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
    * @param language of the generated json schema
    * @return The JSON Schema
    */
   @PostMapping( "json-schema" )
   public ResponseEntity<String> jsonSchema( @RequestBody final String aspectModel,
         @RequestParam( name = "language", defaultValue = "en" ) final String language ) {
      return ResponseEntity.ok( generateService.jsonSchema( aspectModel, language ) );
   }

   /**
    * Handles the request to generate an AASX file based on the given aspect model.
    *
    * @param aspectModel The model provided in the request body used to generate the AASX file.
    * @return A {@link ResponseEntity} containing the result of the AASX file generation.
    */
   @PostMapping( "aasx" )
   public ResponseEntity<String> assx( @RequestBody final String aspectModel ) {
      return ResponseEntity.ok( generateService.generateAASXFile( aspectModel ) );
   }

   /**
    * Handles the request to generate an AAS XML file based on the provided aspect model.
    *
    * @param aspectModel The model provided in the request body used to generate the AAS XML file.
    * @return A {@link ResponseEntity} containing the result of the AAS XML file generation.
    */
   @PostMapping( "aas-xml" )
   public ResponseEntity<String> assXml( @RequestBody final String aspectModel ) {
      return ResponseEntity.ok( generateService.generateAasXmlFile( aspectModel ) );
   }

   /**
    * Handles the request to generate an AAS JSON file based on the provided aspect model.
    *
    * @param aspectModel The model provided in the request body used to generate the AAS XML file.
    * @return A {@link ResponseEntity} containing the result of the AAS XML file generation.
    */
   @PostMapping( "aas-json" )
   public ResponseEntity<String> assJson( @RequestBody final String aspectModel ) {
      return ResponseEntity.ok( generateService.generateAasJsonFile( aspectModel ) );
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
         @RequestParam( name = "pagingOption", defaultValue = "TIME_BASED_PAGING" ) final PagingOption pagingOption,
         @RequestParam( name = "includeCrud", defaultValue = "false" ) final boolean includeCrud,
         @RequestParam( name = "includePost", defaultValue = "false" ) final boolean includePost,
         @RequestParam( name = "includePut", defaultValue = "false" ) final boolean includePut,
         @RequestParam( name = "includePatch", defaultValue = "false" ) final boolean includePatch,
         @RequestParam( name = "resourcePath", defaultValue = "" ) final String resourcePath,
         @RequestParam( name = "ymlProperties", defaultValue = "" ) final String ymlProperties,
         @RequestParam( name = "jsonProperties", defaultValue = "" ) final String jsonProperties )
         throws JsonProcessingException {

      final Optional<String> properties =
            !resourcePath.isEmpty() && (!ymlProperties.isEmpty() || !jsonProperties.isEmpty()) ?
                  Optional.of( !ymlProperties.isEmpty() ? ymlProperties : jsonProperties ) :
                  Optional.empty();

      final String openApiOutput = generateOpenApiSpec( language, aspectModel, baseUrl, includeQueryApi,
            useSemanticVersion, pagingOption, resourcePath, includeCrud, includePost, includePut, includePatch,
            properties, output );

      return ResponseEntity.ok( openApiOutput );
   }

   private String generateOpenApiSpec( final String language, final String aspectModel, final String baseUrl,
         final boolean includeQueryApi, final boolean useSemanticVersion, final PagingOption pagingOption,
         final String resourcePath, final boolean includeCrud, final boolean includePost, final boolean includePut,
         final boolean includePatch, final Optional<String> properties, final String output )
         throws JsonProcessingException {

      final ObjectMapper objectMapper = output.equals( "json" ) ?
            new ObjectMapper() :
            new ObjectMapper( new YAMLFactory() );

      final OpenApiSchemaGenerationConfig config = createOpenApiSchemaGenerationConfig( language, baseUrl,
            useSemanticVersion, resourcePath, pagingOption, includeQueryApi, includeCrud, includePost, includePut,
            includePatch, properties, objectMapper );

      return output.equals( "json" ) ?
            generateService.generateJsonOpenApiSpec( aspectModel, config ) :
            generateService.generateYamlOpenApiSpec( aspectModel, config );
   }

   private OpenApiSchemaGenerationConfig createOpenApiSchemaGenerationConfig( final String language,
         final String baseUrl, final boolean useSemanticVersion, final String resourcePath,
         final PagingOption pagingOption, final boolean includeQueryApi, final boolean includeCrud,
         final boolean includePost, final boolean includePut, final boolean includePatch,
         final Optional<String> properties, final ObjectMapper objectMapper ) throws JsonProcessingException {
      final ObjectNode propertiesNode = objectMapper.readValue( properties.orElse( "{}" ), ObjectNode.class );

      return new OpenApiSchemaGenerationConfig( Locale.forLanguageTag( language ), false, useSemanticVersion, baseUrl,
            resourcePath, propertiesNode, pagingOption, includeQueryApi, includeCrud, includePost, includePut,
            includePatch );
   }

   /**
    * This method is used to generate an AsyncApi specification of the Aspect Model
    *
    * @param aspectModel the Aspect Model Data
    * @param language of the generated AsyncApi specification
    * @param output of the AsyncApi specification
    * @param applicationId Sets the application id, e.g. an identifying URL
    * @param channelAddress Sets the channel address (i.e., for MQTT, the topic's name)
    * @param useSemanticVersion if set to true, the complete semantic version of the Aspect Model will be used as
    *       the version of the API, otherwise only the major part of the Aspect Version is used as the version of the
    *       API.
    * @param writeSeparateFiles Create separate files for each schema
    * @return The AsyncApi specification
    */
   @PostMapping( "async-api-spec" )
   public ResponseEntity<byte[]> asyncApiSpec( @RequestBody final String aspectModel,
         @RequestParam( name = "language", defaultValue = "en" ) final String language,
         @RequestParam( name = "output", defaultValue = "yaml" ) final String output,
         @RequestParam( name = "applicationId", defaultValue = "" ) final String applicationId,
         @RequestParam( name = "channelAddress", defaultValue = "" ) final String channelAddress,
         @RequestParam( name = "useSemanticVersion", defaultValue = "false" ) final boolean useSemanticVersion,
         @RequestParam( name = "writeSeparateFiles", defaultValue = "false" ) final boolean writeSeparateFiles ) {
      final byte[] asyncApiSpec = generateService.generateAsyncApiSpec( aspectModel, language, output, applicationId,
            channelAddress, useSemanticVersion, writeSeparateFiles );

      return buildResponse( asyncApiSpec, writeSeparateFiles );
   }

   private ResponseEntity<byte[]> buildResponse( final byte[] asyncApiSpec, final boolean writeSeparateFiles ) {
      final ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.ok();

      if ( writeSeparateFiles ) {
         responseBuilder.header( HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"async-api-package.zip\"" );
      }

      return responseBuilder.body( asyncApiSpec );
   }
}
