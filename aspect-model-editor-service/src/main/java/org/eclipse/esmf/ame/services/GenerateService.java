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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.eclipse.esmf.ame.exceptions.FileHandlingException;
import org.eclipse.esmf.ame.exceptions.GenerationException;
import org.eclipse.esmf.ame.exceptions.InvalidAspectModelException;
import org.eclipse.esmf.ame.services.utils.ModelUtils;
import org.eclipse.esmf.ame.services.utils.ZipUtils;
import org.eclipse.esmf.aspectmodel.aas.AasFileFormat;
import org.eclipse.esmf.aspectmodel.aas.AspectModelAasGenerator;
import org.eclipse.esmf.aspectmodel.generator.asyncapi.AspectModelAsyncApiGenerator;
import org.eclipse.esmf.aspectmodel.generator.asyncapi.AsyncApiSchemaArtifact;
import org.eclipse.esmf.aspectmodel.generator.asyncapi.AsyncApiSchemaGenerationConfig;
import org.eclipse.esmf.aspectmodel.generator.asyncapi.AsyncApiSchemaGenerationConfigBuilder;
import org.eclipse.esmf.aspectmodel.generator.docu.AspectModelDocumentationGenerator;
import org.eclipse.esmf.aspectmodel.generator.docu.DocumentationGenerationConfig;
import org.eclipse.esmf.aspectmodel.generator.docu.DocumentationGenerationConfigBuilder;
import org.eclipse.esmf.aspectmodel.generator.json.AspectModelJsonPayloadGenerator;
import org.eclipse.esmf.aspectmodel.generator.jsonschema.AspectModelJsonSchemaGenerator;
import org.eclipse.esmf.aspectmodel.generator.jsonschema.JsonSchemaGenerationConfig;
import org.eclipse.esmf.aspectmodel.generator.jsonschema.JsonSchemaGenerationConfigBuilder;
import org.eclipse.esmf.aspectmodel.generator.openapi.AspectModelOpenApiGenerator;
import org.eclipse.esmf.aspectmodel.generator.openapi.OpenApiSchemaGenerationConfig;
import org.eclipse.esmf.aspectmodel.loader.AspectModelLoader;
import org.eclipse.esmf.metamodel.AspectModel;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service class for generating various specifications and files from Aspect Models.
 */
@Service
public class GenerateService {
   public static final String WRONG_RESOURCE_PATH_ID = "The resource path ID and properties ID do not match. Please verify and correct "
         + "them.";
   private static final Logger LOG = LoggerFactory.getLogger( GenerateService.class );

   private final AspectModelLoader aspectModelLoader;

   public GenerateService( final AspectModelLoader aspectModelLoader ) {
      this.aspectModelLoader = aspectModelLoader;
   }

   public byte[] generateHtmlDocument( final String turtleData, final String language ) {
      final ByteArrayInputStream inputStream = ModelUtils.createInputStream( turtleData );
      final AspectModel aspectModel = aspectModelLoader.load( inputStream );

      final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

      final DocumentationGenerationConfig config = DocumentationGenerationConfigBuilder.builder().build();
      final AspectModelDocumentationGenerator generator = new AspectModelDocumentationGenerator( aspectModel.aspect(), config );

      generator.generate( artifactName -> byteArrayOutputStream );
      return byteArrayOutputStream.toByteArray();
   }

   public String jsonSchema( final String turtleData, final String language ) {
      final ByteArrayInputStream inputStream = ModelUtils.createInputStream( turtleData );
      final AspectModel aspectModel = aspectModelLoader.load( inputStream );

      final JsonSchemaGenerationConfig config = JsonSchemaGenerationConfigBuilder.builder().locale(
            Locale.forLanguageTag( language ) ).build();

      final AspectModelJsonSchemaGenerator generator = new AspectModelJsonSchemaGenerator( aspectModel.aspect(), config );

      return generator.generateJson();
   }

   public String sampleJSONPayload( final String turtleData ) {
      final ByteArrayInputStream inputStream = ModelUtils.createInputStream( turtleData );
      final AspectModel aspectModel = aspectModelLoader.load( inputStream );

      final AspectModelJsonPayloadGenerator generator = new AspectModelJsonPayloadGenerator( aspectModel.aspect() );

      return generator.generateJson();
   }

   public String generateAASXFile( final String turtleData ) {
      final ByteArrayInputStream inputStream = ModelUtils.createInputStream( turtleData );
      final AspectModel aspectModel = aspectModelLoader.load( inputStream );

      final AspectModelAasGenerator generator = new AspectModelAasGenerator();
      final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

      generator.generate( AasFileFormat.AASX, aspectModel.aspect(), name -> outputStream );

      return outputStream.toString( StandardCharsets.UTF_8 );
   }

   public String generateAasXmlFile( final String turtleData ) {
      final ByteArrayInputStream inputStream = ModelUtils.createInputStream( turtleData );
      final AspectModel aspectModel = aspectModelLoader.load( inputStream );

      final AspectModelAasGenerator generator = new AspectModelAasGenerator();
      final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

      generator.generate( AasFileFormat.XML, aspectModel.aspect(), name -> outputStream );

      return outputStream.toString( StandardCharsets.UTF_8 );
   }

   public String generateAasJsonFile( final String turtleData ) {
      final ByteArrayInputStream inputStream = ModelUtils.createInputStream( turtleData );
      final AspectModel aspectModel = aspectModelLoader.load( inputStream );

      final AspectModelAasGenerator generator = new AspectModelAasGenerator();
      final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

      generator.generate( AasFileFormat.JSON, aspectModel.aspect(), name -> outputStream );

      return outputStream.toString( StandardCharsets.UTF_8 );
   }

   public String generateYamlOpenApiSpec( final String turtleData, final OpenApiSchemaGenerationConfig config ) {
      final ByteArrayInputStream inputStream = ModelUtils.createInputStream( turtleData );
      final AspectModel aspectModel = aspectModelLoader.load( inputStream );

      final String ymlOutput = new AspectModelOpenApiGenerator( aspectModel.aspect(), config ).generateYaml();

      if ( ymlOutput.equals( "--- {}\n" ) ) {
         throw new GenerationException( WRONG_RESOURCE_PATH_ID );
      }

      return ymlOutput;
   }

   public String generateJsonOpenApiSpec( final String turtleData, final OpenApiSchemaGenerationConfig config ) {
      try {
         final ByteArrayInputStream inputStream = ModelUtils.createInputStream( turtleData );
         final AspectModel aspectModel = aspectModelLoader.load( inputStream );

         final JsonNode json = new AspectModelOpenApiGenerator( aspectModel.aspect(), config ).getContent();

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

   public byte[] generateAsyncApiSpec( final String turtleData, final String language, final String output,
         final String applicationId, final String channelAddress, final boolean useSemanticVersion,
         final boolean writeSeparateFiles ) {
      final ByteArrayInputStream inputStream = ModelUtils.createInputStream( turtleData );
      final AspectModel aspectModel = aspectModelLoader.load( inputStream );

      final AsyncApiSchemaGenerationConfig config = buildAsyncApiSchemaGenerationConfig( applicationId, channelAddress,
            useSemanticVersion, language );
      final AspectModelAsyncApiGenerator generator = new AspectModelAsyncApiGenerator( aspectModel.aspect(), config );

      if ( writeSeparateFiles ) {
         return generateZipFile( generator.generate().toList(), output );
      }

      return generateSingleFile( generator, output );
   }

   private AsyncApiSchemaGenerationConfig buildAsyncApiSchemaGenerationConfig( final String applicationId,
         final String channelAddress, final boolean useSemanticVersion, final String language ) {
      return AsyncApiSchemaGenerationConfigBuilder.builder().useSemanticVersion( useSemanticVersion )
            .applicationId( applicationId ).channelAddress( channelAddress )
            .locale( Locale.forLanguageTag( language ) ).build();
   }

   private byte[] generateZipFile( final List<AsyncApiSchemaArtifact> asyncApiSchemaArtifacts, final String output ) {
      if ( output.equals( "json" ) ) {
         return jsonZip( asyncApiSchemaArtifacts.get( 0 ).getContentWithSeparateSchemasAsJson() );
      }

      return yamlZip( asyncApiSchemaArtifacts.get( 0 ).getContentWithSeparateSchemasAsYaml() );
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

      return ZipUtils.createPackage( content );
   }

   private byte[] yamlZip( final Map<Path, String> separateFilesContent ) {
      final Map<Path, byte[]> content = new HashMap<>();

      for ( final Map.Entry<Path, String> entry : separateFilesContent.entrySet() ) {
         final byte[] bytes = entry.getValue().getBytes( StandardCharsets.UTF_8 );
         content.put( entry.getKey(), bytes );
      }

      return ZipUtils.createPackage( content );
   }

   private byte[] generateSingleFile( final AspectModelAsyncApiGenerator asyncApiSpec, final String output ) {
      if ( output.equals( "yaml" ) ) {
         return asyncApiSpec.generateYaml().getBytes( StandardCharsets.UTF_8 );
      }

      return asyncApiSpec.generateJson().getBytes( StandardCharsets.UTF_8 );
   }
}
