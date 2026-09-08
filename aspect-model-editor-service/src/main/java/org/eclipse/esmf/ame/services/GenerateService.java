/*
 * Copyright (c) 2026 Robert Bosch Manufacturing Solutions GmbH
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
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.eclipse.esmf.ame.constants.ApplicationConstants;
import org.eclipse.esmf.ame.exceptions.FileReadException;
import org.eclipse.esmf.ame.exceptions.GenerationException;
import org.eclipse.esmf.ame.services.utils.ZipUtils;
import org.eclipse.esmf.aspectmodel.aas.AasFileFormat;
import org.eclipse.esmf.aspectmodel.aas.AasGenerationConfigBuilder;
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

import io.micronaut.http.multipart.CompletedFileUpload;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Service class for generating various specifications and files from Aspect Models.
 */
@Singleton
public class GenerateService {
   public static final String WRONG_RESOURCE_PATH_ID = "The resource path ID and properties ID do not match. Please verify and correct "
         + "them.";
   private static final Logger LOG = LoggerFactory.getLogger( GenerateService.class );

   private final AspectModelLoader aspectModelLoader;

   public GenerateService( final AspectModelLoader aspectModelLoader ) {
      this.aspectModelLoader = aspectModelLoader;
   }

   private InputStream openInputStreamFromUpload( final CompletedFileUpload aspectModel ) {
      try {
         return aspectModel.getInputStream();
      } catch ( final IOException e ) {
         throw new FileReadException( "Failed to read uploaded file '" + aspectModel.getFilename() + "': " + e.getMessage(), e );
      }
   }

   private org.eclipse.esmf.metamodel.Aspect extractAspect( final AspectModel aspectModel, final URI uri ) {
      try {
         return aspectModel.aspect();
      } catch ( final java.util.NoSuchElementException e ) {
         throw new GenerationException( String.format( "No Aspect element found in model '%s'. Generation requires an Aspect definition.", uri ) );
      }
   }

   public byte[] generateHtmlDocument( final CompletedFileUpload aspectModelFile, final URI uri, final String language ) {
      final InputStream inputStream = openInputStreamFromUpload( aspectModelFile );
      final AspectModel aspectModel = aspectModelLoader.load( inputStream, uri );
      final org.eclipse.esmf.metamodel.Aspect aspect = extractAspect( aspectModel, uri );

      final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

      final DocumentationGenerationConfig config = DocumentationGenerationConfigBuilder.builder()
            .locale( Locale.forLanguageTag( language ) ).build();
      final AspectModelDocumentationGenerator generator = new AspectModelDocumentationGenerator( aspect, config );

      generator.generate( artifactName -> byteArrayOutputStream );
      return byteArrayOutputStream.toByteArray();
   }

   public String jsonSchema( final CompletedFileUpload aspectModelFile, final URI uri, final String language ) {
      final InputStream inputStream = openInputStreamFromUpload( aspectModelFile );
      final AspectModel aspectModel = aspectModelLoader.load( inputStream, uri );
      final org.eclipse.esmf.metamodel.Aspect aspect = extractAspect( aspectModel, uri );

      final JsonSchemaGenerationConfig config = JsonSchemaGenerationConfigBuilder.builder().locale(
            Locale.forLanguageTag( language ) ).build();

      final AspectModelJsonSchemaGenerator generator = new AspectModelJsonSchemaGenerator( aspect, config );

      return generator.generateJson();
   }

   public String sampleJSONPayload( final CompletedFileUpload aspectModelFile, final URI uri ) {
      final InputStream inputStream = openInputStreamFromUpload( aspectModelFile );
      final AspectModel aspectModel = aspectModelLoader.load( inputStream, uri );
      final org.eclipse.esmf.metamodel.Aspect aspect = extractAspect( aspectModel, uri );

      final AspectModelJsonPayloadGenerator generator = new AspectModelJsonPayloadGenerator( aspect );

      return generator.generateJson();
   }

   public String generateAASXFile( final CompletedFileUpload aspectModelFile, final URI uri ) {
      final InputStream inputStream = openInputStreamFromUpload( aspectModelFile );
      final AspectModel aspectModel = aspectModelLoader.load( inputStream, uri );
      final org.eclipse.esmf.metamodel.Aspect aspect = extractAspect( aspectModel, uri );

      final AspectModelAasGenerator generator = new AspectModelAasGenerator( aspect,
            AasGenerationConfigBuilder.builder().format( AasFileFormat.AASX ).build() );

      return new String( generator.getContent() );
   }

   public String generateAasXmlFile( final CompletedFileUpload aspectModelFile, final URI uri ) {
      final InputStream inputStream = openInputStreamFromUpload( aspectModelFile );
      final AspectModel aspectModel = aspectModelLoader.load( inputStream, uri );
      final org.eclipse.esmf.metamodel.Aspect aspect = extractAspect( aspectModel, uri );

      final AspectModelAasGenerator generator = new AspectModelAasGenerator( aspect,
            AasGenerationConfigBuilder.builder().format( AasFileFormat.XML ).build() );

      return new String( generator.getContent() );
   }

   public String generateAasJsonFile( final CompletedFileUpload aspectModelFile, final URI uri ) {
      final InputStream inputStream = openInputStreamFromUpload( aspectModelFile );
      final AspectModel aspectModel = aspectModelLoader.load( inputStream, uri );
      final org.eclipse.esmf.metamodel.Aspect aspect = extractAspect( aspectModel, uri );

      final AspectModelAasGenerator generator = new AspectModelAasGenerator( aspect,
            AasGenerationConfigBuilder.builder().format( AasFileFormat.JSON ).build() );

      return new String( generator.getContent() );
   }

   public String generateYamlOpenApiSpec( final CompletedFileUpload aspectModelFile, final URI uri,
         final OpenApiSchemaGenerationConfig config ) {
      final InputStream inputStream = openInputStreamFromUpload( aspectModelFile );
      final AspectModel aspectModel = aspectModelLoader.load( inputStream, uri );
      final org.eclipse.esmf.metamodel.Aspect aspect = extractAspect( aspectModel, uri );

      final String ymlOutput = new AspectModelOpenApiGenerator( aspect, config ).generateYaml();

      if ( ymlOutput.equals( "--- {}\n" ) ) {
         throw new GenerationException( WRONG_RESOURCE_PATH_ID );
      }

      return ymlOutput;
   }

   public String generateJsonOpenApiSpec( final CompletedFileUpload aspectModelFile, final URI uri,
         final OpenApiSchemaGenerationConfig config ) {
      final InputStream inputStream = openInputStreamFromUpload( aspectModelFile );
      final AspectModel aspectModel = aspectModelLoader.load( inputStream, uri );
      final org.eclipse.esmf.metamodel.Aspect aspect = extractAspect( aspectModel, uri );

      final JsonNode json = new AspectModelOpenApiGenerator( aspect, config ).getContent();

      final ByteArrayOutputStream out = new ByteArrayOutputStream();
      final ObjectMapper objectMapper = new ObjectMapper();

      objectMapper.writerWithDefaultPrettyPrinter().writeValue( out, json );

      final String jsonOutput = out.toString();

      if ( jsonOutput.equals( "{ }" ) ) {
         throw new GenerationException( WRONG_RESOURCE_PATH_ID );
      }

      return jsonOutput;
   }

   public byte[] generateAsyncApiSpec( final CompletedFileUpload aspectModelFile, final URI uri, final String language, final String output,
         final String applicationId, final String channelAddress, final boolean useSemanticVersion,
         final boolean writeSeparateFiles ) {
      final InputStream inputStream = openInputStreamFromUpload( aspectModelFile );
      final AspectModel aspectModel = aspectModelLoader.load( inputStream, uri );
      final org.eclipse.esmf.metamodel.Aspect aspect = extractAspect( aspectModel, uri );

      final AsyncApiSchemaGenerationConfig config = buildAsyncApiSchemaGenerationConfig( applicationId, channelAddress,
            useSemanticVersion, language );
      final AspectModelAsyncApiGenerator generator = new AspectModelAsyncApiGenerator( aspect, config );

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
      if ( output.equals( ApplicationConstants.OutputFormats.JSON ) ) {
         return jsonZip( asyncApiSchemaArtifacts.getFirst().getContentWithSeparateSchemasAsJson() );
      }

      return yamlZip( asyncApiSchemaArtifacts.getFirst().getContentWithSeparateSchemasAsYaml() );
   }

   private byte[] jsonZip( final Map<Path, JsonNode> separateFilesContent ) {
      final ObjectMapper objectMapper = new ObjectMapper();
      final Map<Path, byte[]> content = new HashMap<>();

      for ( final Map.Entry<Path, JsonNode> entry : separateFilesContent.entrySet() ) {
         final byte[] bytes = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes( entry.getValue() );
         content.put( entry.getKey(), bytes );
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
      if ( output.equals( ApplicationConstants.OutputFormats.YAML ) ) {
         return asyncApiSpec.generateYaml().getBytes( StandardCharsets.UTF_8 );
      }

      return asyncApiSpec.generateJson().getBytes( StandardCharsets.UTF_8 );
   }
}
