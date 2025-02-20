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

import java.util.Locale;
import java.util.Optional;

import org.eclipse.esmf.ame.services.GenerateService;
import org.eclipse.esmf.aspectmodel.generator.openapi.OpenApiSchemaGenerationConfig;
import org.eclipse.esmf.aspectmodel.generator.openapi.PagingOption;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.springframework.http.HttpHeaders;
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
public class GenerateController {

   private final GenerateService generateService;

   public GenerateController( final GenerateService generateService ) {
      this.generateService = generateService;
   }

   /**
    * This Method is used to generate a documentation of the aspect model
    *
    * @param turtleData the Aspect Model Data
    * @param language the language for the generated documentation
    * @return the aspect model definition as documentation html file.
    */
   @PostMapping( "documentation" )
   public ResponseEntity<byte[]> generateHtml( @RequestBody final String turtleData,
         @RequestParam( name = "language" ) final String language ) {
      return ResponseEntity.ok( generateService.generateHtmlDocument( turtleData, language ) );
   }

   /**
    * This Method is used to generate a JSON Schema of the aspect model
    *
    * @param turtleData The Aspect Model Data
    * @param language the language of the generated JSON schema
    * @return The JSON Schema
    */
   @PostMapping( "json-schema" )
   public ResponseEntity<String> jsonSchema( @RequestBody final String turtleData,
         @RequestParam( name = "language", defaultValue = "en" ) final String language ) {
      return ResponseEntity.ok( generateService.jsonSchema( turtleData, language ) );
   }

   /**
    * This Method is used to generate a sample JSON Payload of the aspect model
    *
    * @param turtleData the Aspect Model Data
    * @return The JSON Sample Payload
    */
   @PostMapping( "json-sample" )
   public ResponseEntity<Object> jsonSample( @RequestBody final String turtleData ) {
      return ResponseEntity.ok( generateService.sampleJSONPayload( turtleData ) );
   }

   /**
    * Handles the request to generate an AASX file based on the given aspect model.
    *
    * @param turtleData The model provided in the request body used to generate the AASX file.
    * @return A {@link ResponseEntity} containing the result of the AASX file generation.
    */
   @PostMapping( "aasx" )
   public ResponseEntity<String> assx( @RequestBody final String turtleData ) {
      return ResponseEntity.ok( generateService.generateAASXFile( turtleData ) );
   }

   /**
    * Handles the request to generate an AAS XML file based on the provided aspect model.
    *
    * @param turtleData The model provided in the request body used to generate the AAS XML file.
    * @return A {@link ResponseEntity} containing the result of the AAS XML file generation.
    */
   @PostMapping( "aas-xml" )
   public ResponseEntity<String> assXml( @RequestBody final String turtleData ) {
      return ResponseEntity.ok( generateService.generateAasXmlFile( turtleData ) );
   }

   /**
    * Handles the request to generate an AAS JSON file based on the provided aspect model.
    *
    * @param turtleData The model provided in the request body used to generate the AAS JSON file.
    * @return A {@link ResponseEntity} containing the result of the AAS JSON file generation.
    */
   @PostMapping( "aas-json" )
   public ResponseEntity<String> assJson( @RequestBody final String turtleData ) {
      return ResponseEntity.ok( generateService.generateAasJsonFile( turtleData ) );
   }

   /**
    * This method is used to generate an OpenAPI specification of the Aspect Model
    *
    * @param turtleData the Aspect Model Data
    * @param language the language of the generated OpenAPI specification
    * @param output the format of the OpenAPI specification (json or yaml)
    * @param baseUrl the base URL for the Aspect API
    * @param includeQueryApi if set to true, a path section for the Query API Endpoint of the Aspect API will be
    * included in the specification
    * @param useSemanticVersion if set to true, the complete semantic version of the Aspect Model will be used as
    * the version of the API, otherwise only the major part of the Aspect Version is used as the version of the
    * API.
    * @param pagingOption if defined, the chosen paging type will be in the JSON.
    * @param includeCrud if set to true, CRUD operations will be included in the specification
    * @param includePost if set to true, POST operations will be included in the specification
    * @param includePut if set to true, PUT operations will be included in the specification
    * @param includePatch if set to true, PATCH operations will be included in the specification
    * @param resourcePath the resource path for the API
    * @param ymlProperties additional YAML properties for the OpenAPI specification
    * @param jsonProperties additional JSON properties for the OpenAPI specification
    * @return The OpenAPI specification
    * @throws JsonProcessingException if there is an error processing JSON
    */
   @PostMapping( "open-api-spec" )
   public ResponseEntity<String> openApiSpec( @RequestBody final String turtleData,
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
            !resourcePath.isEmpty() && ( !ymlProperties.isEmpty() || !jsonProperties.isEmpty() ) ?
                  Optional.of( !ymlProperties.isEmpty() ? ymlProperties : jsonProperties ) :
                  Optional.empty();

      final String openApiOutput = generateOpenApiSpec( language, turtleData, baseUrl, includeQueryApi,
            useSemanticVersion, pagingOption, resourcePath, includeCrud, includePost, includePut, includePatch,
            properties, output );

      return ResponseEntity.ok( openApiOutput );
   }

   private String generateOpenApiSpec( final String language, final String turtleData, final String baseUrl,
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
            generateService.generateJsonOpenApiSpec( turtleData, config ) :
            generateService.generateYamlOpenApiSpec( turtleData, config );
   }

   private OpenApiSchemaGenerationConfig createOpenApiSchemaGenerationConfig( final String language,
         final String baseUrl, final boolean useSemanticVersion, final String resourcePath,
         final PagingOption pagingOption, final boolean includeQueryApi, final boolean includeCrud,
         final boolean includePost, final boolean includePut, final boolean includePatch,
         final Optional<String> properties, final ObjectMapper objectMapper ) throws JsonProcessingException {
      final ObjectNode propertiesNode = objectMapper.readValue( properties.orElse( "{}" ), ObjectNode.class );

      return new OpenApiSchemaGenerationConfig( Locale.forLanguageTag( language ), false, useSemanticVersion, baseUrl,
            resourcePath, propertiesNode, pagingOption, includeQueryApi, includeCrud, includePost, includePut,
            includePatch, null );
   }

   /**
    * This method is used to generate an AsyncApi specification of the Aspect Model
    *
    * @param turtleData the Aspect Model Data
    * @param language the language of the generated AsyncApi specification
    * @param output the format of the AsyncApi specification (json or yaml)
    * @param applicationId Sets the application id, e.g. an identifying URL
    * @param channelAddress Sets the channel address (i.e., for MQTT, the topic's name)
    * @param useSemanticVersion if set to true, the complete semantic version of the Aspect Model will be used as
    * the version of the API, otherwise only the major part of the Aspect Version is used as the version of the
    * API.
    * @param writeSeparateFiles Create separate files for each schema
    * @return The AsyncApi specification
    */
   @PostMapping( "async-api-spec" )
   public ResponseEntity<byte[]> asyncApiSpec( @RequestBody final String turtleData,
         @RequestParam( name = "language", defaultValue = "en" ) final String language,
         @RequestParam( name = "output", defaultValue = "yaml" ) final String output,
         @RequestParam( name = "applicationId", defaultValue = "" ) final String applicationId,
         @RequestParam( name = "channelAddress", defaultValue = "" ) final String channelAddress,
         @RequestParam( name = "useSemanticVersion", defaultValue = "false" ) final boolean useSemanticVersion,
         @RequestParam( name = "writeSeparateFiles", defaultValue = "false" ) final boolean writeSeparateFiles ) {
      final byte[] asyncApiSpec = generateService.generateAsyncApiSpec( turtleData, language, output, applicationId,
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
