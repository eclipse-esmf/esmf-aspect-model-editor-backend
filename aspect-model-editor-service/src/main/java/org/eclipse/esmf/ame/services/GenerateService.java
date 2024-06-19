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

package org.eclipse.esmf.ame.services;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.LocaleUtils;
import org.eclipse.esmf.ame.exceptions.FileHandlingException;
import org.eclipse.esmf.ame.exceptions.GenerationException;
import org.eclipse.esmf.ame.exceptions.InvalidAspectModelException;
import org.eclipse.esmf.ame.resolver.strategy.FileSystemStrategy;
import org.eclipse.esmf.ame.resolver.strategy.utils.ResolverUtils;
import org.eclipse.esmf.ame.services.utils.ZipUtils;
import org.eclipse.esmf.ame.utils.ModelUtils;
import org.eclipse.esmf.aspectmodel.aas.AasFileFormat;
import org.eclipse.esmf.aspectmodel.aas.AspectModelAasGenerator;
import org.eclipse.esmf.aspectmodel.generator.asyncapi.AspectModelAsyncApiGenerator;
import org.eclipse.esmf.aspectmodel.generator.asyncapi.AsyncApiSchemaArtifact;
import org.eclipse.esmf.aspectmodel.generator.asyncapi.AsyncApiSchemaGenerationConfig;
import org.eclipse.esmf.aspectmodel.generator.asyncapi.AsyncApiSchemaGenerationConfigBuilder;
import org.eclipse.esmf.aspectmodel.generator.docu.AspectModelDocumentationGenerator;
import org.eclipse.esmf.aspectmodel.generator.json.AspectModelJsonPayloadGenerator;
import org.eclipse.esmf.aspectmodel.generator.jsonschema.AspectModelJsonSchemaGenerator;
import org.eclipse.esmf.aspectmodel.generator.openapi.AspectModelOpenApiGenerator;
import org.eclipse.esmf.aspectmodel.generator.openapi.OpenApiSchemaGenerationConfig;
import org.eclipse.esmf.aspectmodel.resolver.services.DataType;
import org.eclipse.esmf.aspectmodel.resolver.services.VersionedModel;
import org.eclipse.esmf.metamodel.Aspect;
import org.eclipse.esmf.metamodel.AspectContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.google.common.collect.ImmutableMap;

import io.vavr.control.Try;

@Service
public class GenerateService {
   private static final Logger LOG = LoggerFactory.getLogger( GenerateService.class );
   private static final ObjectMapper YAML_MAPPER = new YAMLMapper().enable( YAMLGenerator.Feature.MINIMIZE_QUOTES );
   private static final String COULD_NOT_LOAD_ASPECT = "Could not load Aspect";
   private static final String COULD_NOT_LOAD_ASPECT_MODEL = "Could not load Aspect model, please make sure the model is valid.";
   public static final String WRONG_RESOURCE_PATH_ID = "The resource path ID and properties ID do not match. Please verify and correct them.";

   public GenerateService() {
      DataType.setupTypeMapping();
   }

   public byte[] generateHtmlDocument( final String aspectModel, final String language ) throws IOException {
      final AspectModelDocumentationGenerator generator = new AspectModelDocumentationGenerator( language,
            generateAspectContext( aspectModel ) );

      final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

      generator.generate( artifactName -> byteArrayOutputStream,
            ImmutableMap.<AspectModelDocumentationGenerator.HtmlGenerationOption, String> builder().build() );

      return byteArrayOutputStream.toByteArray();
   }

   public String jsonSchema( final String aspectModel, final String language ) {
      try {
         final AspectContext aspectContext = generateAspectContext( aspectModel );

         final AspectModelJsonSchemaGenerator generator = new AspectModelJsonSchemaGenerator();
         final JsonNode jsonSchema = generator.apply( aspectContext.aspect(), Locale.forLanguageTag( language ) );

         final ByteArrayOutputStream out = new ByteArrayOutputStream();
         final ObjectMapper objectMapper = new ObjectMapper();

         objectMapper.writerWithDefaultPrettyPrinter().writeValue( out, jsonSchema );

         return out.toString();
      } catch ( final IOException e ) {
         LOG.error( COULD_NOT_LOAD_ASPECT_MODEL );
         throw new InvalidAspectModelException( COULD_NOT_LOAD_ASPECT, e );
      }
   }

   public String sampleJSONPayload( final String aspectModel ) {
      try {
         return new AspectModelJsonPayloadGenerator( generateAspectContext( aspectModel ) ).generateJson();
      } catch ( final IOException e ) {
         LOG.error( COULD_NOT_LOAD_ASPECT_MODEL );
         throw new InvalidAspectModelException( COULD_NOT_LOAD_ASPECT, e );
      }
   }

   public String generateAASXFile( final String aspectModel ) {
      final AspectModelAasGenerator generator = new AspectModelAasGenerator();
      final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

      final AspectContext aspectContext = generateAspectContext( aspectModel );

      generator.generate( AasFileFormat.AASX, aspectContext.aspect(), name -> outputStream );

      return outputStream.toString( StandardCharsets.UTF_8 );
   }

   public String generateAasXmlFile( final String aspectModel ) {
      final AspectModelAasGenerator generator = new AspectModelAasGenerator();
      final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

      final AspectContext aspectContext = generateAspectContext( aspectModel );

      generator.generate( AasFileFormat.XML, aspectContext.aspect(), name -> outputStream );

      return outputStream.toString( StandardCharsets.UTF_8 );
   }

   public String generateAasJsonFile( final String aspectModel ) {
      final AspectModelAasGenerator generator = new AspectModelAasGenerator();
      final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

      final AspectContext aspectContext = generateAspectContext( aspectModel );

      generator.generate( AasFileFormat.JSON, aspectContext.aspect(), name -> outputStream );

      return outputStream.toString( StandardCharsets.UTF_8 );
   }

   private AspectContext generateAspectContext( final String aspectModel ) {
      final FileSystemStrategy fileSystemStrategy = new FileSystemStrategy( aspectModel );
      final Try<VersionedModel> versionedModels = ResolverUtils.fetchVersionModel( fileSystemStrategy );

      final Try<AspectContext> context = versionedModels.flatMap(
            model -> ResolverUtils.resolveSingleAspect( fileSystemStrategy, model ) );

      return ModelUtils.getAspectContext( context );
   }

   public String generateYamlOpenApiSpec( final String aspectModel, final OpenApiSchemaGenerationConfig config ) {

      final String ymlOutput = new AspectModelOpenApiGenerator().apply(
            ResolverUtils.resolveAspectFromModel( aspectModel ), config ).getContentAsYaml();

      if ( ymlOutput.equals( "--- {}\n" ) ) {
         throw new GenerationException( WRONG_RESOURCE_PATH_ID );
      }

      return ymlOutput;
   }

   public String generateJsonOpenApiSpec( final String aspectModel, final OpenApiSchemaGenerationConfig config ) {
      try {
         final JsonNode json = new AspectModelOpenApiGenerator()
               .apply( ResolverUtils.resolveAspectFromModel( aspectModel ), config ).getContent();

         final ByteArrayOutputStream out = new ByteArrayOutputStream();
         final ObjectMapper objectMapper = new ObjectMapper();

         objectMapper.writerWithDefaultPrettyPrinter().writeValue( out, json );

         final String jsonOutput = out.toString();

         if ( jsonOutput.equals( "{ }" ) ) {
            throw new GenerationException( WRONG_RESOURCE_PATH_ID );
         }

         return jsonOutput;
      } catch ( final IOException e ) {
         LOG.error( "JSON OpenAPI specification could not be generated." );
         throw new InvalidAspectModelException( "Error generating JSON OpenAPI specification", e );
      }
   }

   public byte[] generateAsyncApiSpec( final String aspectModel, final String language, final String output,
         final String applicationId, final String channelAddress, final boolean useSemanticVersion,
         final boolean writeSeparateFiles ) {
      final AspectModelAsyncApiGenerator generator = new AspectModelAsyncApiGenerator();
      final AsyncApiSchemaGenerationConfig config = buildAsyncApiSchemaGenerationConfig( applicationId, channelAddress,
            useSemanticVersion, language );

      final Aspect aspect = ResolverUtils.resolveAspectFromModel( aspectModel );
      final AsyncApiSchemaArtifact asyncApiSpec = generator.apply( aspect, config );

      if ( writeSeparateFiles ) {
         return generateZipFile( asyncApiSpec, output );
      }

      return generateSingleFile( asyncApiSpec, output );
   }

   private AsyncApiSchemaGenerationConfig buildAsyncApiSchemaGenerationConfig( final String applicationId,
         final String channelAddress, final boolean useSemanticVersion, final String language ) {
      return AsyncApiSchemaGenerationConfigBuilder.builder().useSemanticVersion( useSemanticVersion )
                                                  .applicationId( applicationId ).channelAddress( channelAddress )
                                                  .locale( LocaleUtils.toLocale( language ) ).build();
   }

   private byte[] generateZipFile( final AsyncApiSchemaArtifact asyncApiSpec, final String output ) {
      if ( output.equals( "json" ) ) {
         return jsonZip( asyncApiSpec.getContentWithSeparateSchemasAsJson() );
      }

      return yamlZip( asyncApiSpec.getContentWithSeparateSchemasAsYaml() );
   }

   private byte[] jsonZip( final Map<Path, JsonNode> separateFilesContent ) {
      final ObjectMapper objectMapper = new ObjectMapper();
      final Map<Path, byte[]> content = new HashMap<>();

      for ( final Map.Entry<Path, JsonNode> entry : separateFilesContent.entrySet() ) {
         try {
            final byte[] bytes = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes( entry.getValue() );
            content.put( entry.getKey(), bytes );
         } catch ( final JsonProcessingException e ) {
            LOG.error( "Failed to convert JSON to bytes.", e );
            throw new FileHandlingException( "Failed to get JSON async api.", e );
         }
      }

      return ZipUtils.createAsyncApiPackage( content );
   }

   private byte[] yamlZip( final Map<Path, String> separateFilesContent ) {
      final Map<Path, byte[]> content = new HashMap<>();

      for ( final Map.Entry<Path, String> entry : separateFilesContent.entrySet() ) {
         final byte[] bytes = entry.getValue().getBytes( StandardCharsets.UTF_8 );
         content.put( entry.getKey(), bytes );
      }

      return ZipUtils.createAsyncApiPackage( content );
   }

   private byte[] generateSingleFile( final AsyncApiSchemaArtifact asyncApiSpec, final String output ) {
      final JsonNode json = asyncApiSpec.getContent();

      if ( output.equals( "yaml" ) ) {
         return jsonToYaml( json ).getBytes( StandardCharsets.UTF_8 );
      }

      return json.toString().getBytes( StandardCharsets.UTF_8 );
   }

   private String jsonToYaml( final JsonNode json ) {
      try {
         return YAML_MAPPER.writeValueAsString( json );
      } catch ( final JsonProcessingException e ) {
         LOG.error( "JSON could not be converted to YAML", e );
         throw new FileHandlingException( "Failed to get YAML async api.", e );
      }
   }
}
