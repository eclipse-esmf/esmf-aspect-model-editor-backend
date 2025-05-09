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

import java.util.Locale;
import java.util.Optional;

import org.eclipse.esmf.ame.MediaTypeExtension;
import org.eclipse.esmf.ame.services.GenerateService;
import org.eclipse.esmf.aspectmodel.generator.openapi.OpenApiSchemaGenerationConfig;
import org.eclipse.esmf.aspectmodel.generator.openapi.PagingOption;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.annotation.QueryValue;

/**
 * Controller class that supports the generation of the aspect model into other formats.
 */
@Controller( "generate" )
public class GenerateController {

   private final GenerateService generateService;

   public GenerateController( final GenerateService generateService ) {
      this.generateService = generateService;
   }

   /**
    * This Method is used to generate a documentation of the aspect model
    *
    * @param aspectModel the Aspect Model Data
    * @param language the language for the generated documentation
    * @return the aspect model definition as documentation html file.
    */
   @Post( uri = "/documentation", consumes = { MediaType.TEXT_PLAIN, "text/turtle", "application/json" } )
   @Produces( MediaType.TEXT_HTML )
   public HttpResponse<byte[]> generateHtml( @Body final String aspectModel, @QueryValue( defaultValue = "en" ) final String language ) {
      return HttpResponse.ok( generateService.generateHtmlDocument( aspectModel, language ) );
   }

   /**
    * This Method is used to generate a JSON Schema of the aspect model
    *
    * @param aspectModel The Aspect Model Data
    * @param language the language of the generated JSON schema
    * @return The JSON Schema
    */
   @Post( uri = "/json-schema", consumes = { MediaType.TEXT_PLAIN, "text/turtle", "application/json" } )
   public HttpResponse<String> jsonSchema( @Body final String aspectModel, @QueryValue( defaultValue = "en" ) final String language ) {
      return HttpResponse.ok( generateService.jsonSchema( aspectModel, language ) );
   }

   /**
    * This Method is used to generate a sample JSON Payload of the aspect model
    *
    * @param aspectModel the Aspect Model Data
    * @return The JSON Sample Payload
    */
   @Post( uri = "/json-sample", consumes = { MediaType.TEXT_PLAIN, "text/turtle", "application/json" } )
   public HttpResponse<Object> jsonSample( @Body final String aspectModel ) {
      return HttpResponse.ok( generateService.sampleJSONPayload( aspectModel ) );
   }

   /**
    * Handles the request to generate an AASX file based on the given aspect model.
    *
    * @param aspectModel The model provided in the request body used to generate the AASX file.
    * @return A {@link HttpResponse} containing the result of the AASX file generation.
    */
   @Post( uri = "/aasx", consumes = "text/plain" )
   @Produces( MediaTypeExtension.APPLICATION_AASX )
   public HttpResponse<String> generateAasx( @Body final String aspectModel ) {
      return HttpResponse.ok( generateService.generateAASXFile( aspectModel ) );
   }

   /**
    * Handles the request to generate an AAS XML file based on the provided aspect model.
    *
    * @param aspectModel The model provided in the request body used to generate the AAS XML file.
    * @return A {@link HttpResponse} containing the result of the AAS XML file generation.
    */
   @Post( uri = "/aas-xml", consumes = "text/plain" )
   @Produces( MediaType.APPLICATION_XML )
   public HttpResponse<String> generateAasXml( @Body final String aspectModel ) {
      return HttpResponse.ok( generateService.generateAasXmlFile( aspectModel ) );
   }

   /**
    * Handles the request to generate an AAS JSON file based on the provided aspect model.
    *
    * @param aspectModel The model provided in the request body used to generate the AAS JSON file.
    * @return A {@link String} containing the result of the AAS JSON file generation.
    */
   @Post( uri = "/aas-json", consumes = "text/plain" )
   public HttpResponse<String> generateAasJson( @Body final String aspectModel ) {
      return HttpResponse.ok( generateService.generateAasJsonFile( aspectModel ) );
   }

   /**
    * This method is used to generate an OpenAPI specification of the Aspect Model
    *
    * @param aspectModel the Aspect Model Data
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
   @Post( uri = "/open-api-spec", consumes = "text/plain", produces = MediaType.APPLICATION_JSON )
   @Produces( { MediaType.APPLICATION_YAML, MediaType.APPLICATION_JSON } )
   public HttpResponse<String> openApiSpec( @Body final String aspectModel,
         @QueryValue( defaultValue = "en" ) final String language,
         @QueryValue( defaultValue = "yaml" ) final String output,
         @QueryValue( defaultValue = "https://www.eclipse.org" ) final String baseUrl,
         @QueryValue( defaultValue = "false" ) final boolean includeQueryApi,
         @QueryValue( defaultValue = "false" ) final boolean useSemanticVersion,
         @QueryValue( defaultValue = "TIME_BASED_PAGING" ) final PagingOption pagingOption,
         @QueryValue( defaultValue = "false" ) final boolean includeCrud,
         @QueryValue( defaultValue = "false" ) final boolean includePost,
         @QueryValue( defaultValue = "false" ) final boolean includePut,
         @QueryValue( defaultValue = "false" ) final boolean includePatch,
         @QueryValue( defaultValue = "" ) final String resourcePath,
         @QueryValue( defaultValue = "" ) final String ymlProperties,
         @QueryValue( defaultValue = "" ) final String jsonProperties )
         throws JsonProcessingException {

      final Optional<String> properties =
            !resourcePath.isEmpty() && ( !ymlProperties.isEmpty() || !jsonProperties.isEmpty() ) ?
                  Optional.of( !ymlProperties.isEmpty() ? ymlProperties : jsonProperties ) :
                  Optional.empty();

      final String openApiOutput = generateOpenApiSpec( language, aspectModel, baseUrl, includeQueryApi,
            useSemanticVersion, pagingOption, resourcePath, includeCrud, includePost, includePut, includePatch,
            properties, output );

      final String contentType = output.equalsIgnoreCase( "json" ) ? MediaType.APPLICATION_JSON : MediaType.APPLICATION_YAML;

      return HttpResponse.ok( openApiOutput ).contentType( contentType );
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
            includePatch, null );
   }

   /**
    * This method is used to generate an AsyncApi specification of the Aspect Model
    *
    * @param aspectModel the Aspect Model Data
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
   @Post( uri = "/async-api-spec", consumes = "text/plain" )
   @Produces( { MediaType.APPLICATION_YAML, MediaType.APPLICATION_JSON, MediaType.APPLICATION_ZIP } )
   public HttpResponse<byte[]> asyncApiSpec( @Body final String aspectModel,
         @QueryValue( defaultValue = "en" ) final String language,
         @QueryValue( defaultValue = "yaml" ) final String output,
         @QueryValue( defaultValue = "" ) final String applicationId,
         @QueryValue( defaultValue = "" ) final String channelAddress,
         @QueryValue( defaultValue = "false" ) final boolean useSemanticVersion,
         @QueryValue( defaultValue = "false" ) final boolean writeSeparateFiles ) {
      final byte[] asyncApiSpec = generateService.generateAsyncApiSpec( aspectModel, language, output, applicationId,
            channelAddress, useSemanticVersion, writeSeparateFiles );

      return buildResponse( asyncApiSpec, writeSeparateFiles, output );
   }

   private HttpResponse<byte[]> buildResponse( final byte[] asyncApiSpec, final boolean writeSeparateFiles, final String output ) {
      if ( writeSeparateFiles ) {
         return HttpResponse.ok()
               .header( HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"async-api-package.zip\"" )
               .body( asyncApiSpec )
               .contentType( MediaType.APPLICATION_ZIP );
      }

      final String contentType = output.equalsIgnoreCase( "json" ) ? MediaType.APPLICATION_JSON : MediaType.APPLICATION_YAML;

      return HttpResponse.ok( asyncApiSpec ).contentType( contentType );
   }
}
