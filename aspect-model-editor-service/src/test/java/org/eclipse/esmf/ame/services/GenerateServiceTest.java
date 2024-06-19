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

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.zip.ZipInputStream;

import org.eclipse.esmf.ame.config.TestConfig;
import org.eclipse.esmf.ame.exceptions.GenerationException;
import org.eclipse.esmf.aspectmodel.generator.openapi.OpenApiSchemaGenerationConfig;
import org.eclipse.esmf.aspectmodel.generator.openapi.PagingOption;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

@ExtendWith( SpringExtension.class )
@SpringBootTest( classes = GenerateService.class )
@DirtiesContext( classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD )
@Import( TestConfig.class )
@ActiveProfiles( "test" )
class GenerateServiceTest {

   @Autowired
   private GenerateService generateService;

   private static final Path resourcesPath = Path.of( "src", "test", "resources", "services" );
   private static final Path eclipseTestPath = Path.of( resourcesPath.toString(), "org.eclipse.esmf.example", "1.0.0" );

   private static final String model = "AspectModelForService.ttl";

   @Test
   void testAspectModelJsonSample() throws IOException {
      final Path storagePath = Path.of( eclipseTestPath.toString(), model );
      final String testModel = Files.readString( storagePath, StandardCharsets.UTF_8 );

      final String payload = generateService.sampleJSONPayload( testModel );

      assertEquals( "{\"property\":\"eOMtThyhVNLWUZNRcBaQKxI\"}", payload );
   }

   @Test
   void testAspectModelJsonSchema() throws IOException {
      final Path storagePath = Path.of( eclipseTestPath.toString(), model );
      final String testModel = Files.readString( storagePath, StandardCharsets.UTF_8 );

      final String payload = generateService.jsonSchema( testModel, "en-EN" );

      assertTrue( payload.contains( "#/components/schemas/Characteristic" ) );
   }

   @Test
   void testAspectModelJsonOpenApiSpecWithoutResourcePath() throws IOException {
      final Path storagePath = Path.of( eclipseTestPath.toString(), model );
      final String testModel = Files.readString( storagePath, StandardCharsets.UTF_8 );

      final OpenApiSchemaGenerationConfig config = new OpenApiSchemaGenerationConfig( Locale.forLanguageTag( "en" ),
            false, false,
            "https://test.com", "", new ObjectMapper().createObjectNode(), PagingOption.TIME_BASED_PAGING, false, false,
            false, false, false, null );

      final String payload = generateService.generateJsonOpenApiSpec( testModel, config );

      assertTrue( payload.contains( "\"openapi\" : \"3.0.3\"" ) );
      assertTrue( payload.contains( "\"version\" : \"v1\"" ) );
      assertTrue( payload.contains( "\"title\" : \"AspectModelForService\"" ) );
      assertTrue( payload.contains( "\"url\" : \"https://test.com/api/v1\"" ) );
   }

   @Test
   void testAspectModelJsonOpenApiSpecWithResourcePath() throws IOException {
      final Path storagePath = Path.of( eclipseTestPath.toString(), model );
      final String testModel = Files.readString( storagePath, StandardCharsets.UTF_8 );

      final ObjectMapper objectMapper = new ObjectMapper();
      final ObjectNode jsonProperties = (ObjectNode) objectMapper.readTree( """
            {
              "resourceId": {
                "name": "resourceId",
                "in": "path",
                "description": "An example resource Id.",
                "required": true,
                "schema": {
                  "type": "string"
                }
              }
            }
            """ );

      final OpenApiSchemaGenerationConfig config = new OpenApiSchemaGenerationConfig( Locale.forLanguageTag( "en" ),
            false, false,
            "https://test.com", "/resource/{resourceId}", jsonProperties, PagingOption.TIME_BASED_PAGING, false, false,
            false, false, false, null );

      final String payload = generateService.generateJsonOpenApiSpec( testModel, config );

      assertTrue( payload.contains( "\"openapi\" : \"3.0.3\"" ) );
      assertTrue( payload.contains( "\"version\" : \"v1\"" ) );
      assertTrue( payload.contains( "\"title\" : \"AspectModelForService\"" ) );
      assertTrue( payload.contains( "\"url\" : \"https://test.com/api/v1\"" ) );
      assertTrue( payload.contains( "\"/resource/{resourceId}\"" ) );
      assertTrue( payload.contains( "\"name\" : \"resourceId\"" ) );
      assertTrue( payload.contains( "\"in\" : \"path\"" ) );
   }

   @Test
   void testAspectModelJsonOpenApiSpecWithWrongResourcePathProperties() throws IOException {
      final Path storagePath = Path.of( eclipseTestPath.toString(), model );
      final String testModel = Files.readString( storagePath, StandardCharsets.UTF_8 );

      final ObjectMapper objectMapper = new ObjectMapper();
      final ObjectNode jsonProperties = (ObjectNode) objectMapper.readTree( """
            {
              "wrongId": {
                "name": "wrongId",
                "in": "path",
                "description": "An example resource Id.",
                "required": true,
                "schema": {
                  "type": "string"
                }
              }
            }
            """ );

      final OpenApiSchemaGenerationConfig config = new OpenApiSchemaGenerationConfig( Locale.forLanguageTag( "en" ),
            false, false,
            "https://test.com", "/resource/{resourceId}", jsonProperties, PagingOption.TIME_BASED_PAGING, false, false,
            false, false, false, null );

      assertThrows( GenerationException.class, () -> generateService.generateJsonOpenApiSpec( testModel, config ) );
   }

   @Test
   void testAspectModelJsonOpenApiSpecWithIncludeCrud() throws IOException {
      final Path storagePath = Path.of( eclipseTestPath.toString(), model );
      final String testModel = Files.readString( storagePath, StandardCharsets.UTF_8 );

      final OpenApiSchemaGenerationConfig config = new OpenApiSchemaGenerationConfig( Locale.forLanguageTag( "en" ),
            false, false,
            "https://test.com", "", new ObjectMapper().createObjectNode(), PagingOption.TIME_BASED_PAGING, false, true,
            false, false, false, null );

      final String payload = generateService.generateJsonOpenApiSpec( testModel, config );

      assertTrue( payload.contains( "\"openapi\" : \"3.0.3\"" ) );
      assertTrue( payload.contains( "\"version\" : \"v1\"" ) );
      assertTrue( payload.contains( "\"title\" : \"AspectModelForService\"" ) );
      assertTrue( payload.contains( "\"url\" : \"https://test.com/api/v1\"" ) );
      assertTrue( payload.contains( "\"get\" : " ) );
      assertTrue( payload.contains( "\"post\" : " ) );
      assertTrue( payload.contains( "\"patch\" : " ) );
      assertTrue( payload.contains( "\"put\" : " ) );
   }

   @Test
   void testAspectModelJsonOpenApiSpecWithIncludePost() throws IOException {
      final Path storagePath = Path.of( eclipseTestPath.toString(), model );
      final String testModel = Files.readString( storagePath, StandardCharsets.UTF_8 );

      final OpenApiSchemaGenerationConfig config = new OpenApiSchemaGenerationConfig( Locale.forLanguageTag( "en" ),
            false, false,
            "https://test.com", "", new ObjectMapper().createObjectNode(), PagingOption.TIME_BASED_PAGING, false, false,
            true, false, false, null );

      final String payload = generateService.generateJsonOpenApiSpec( testModel, config );

      assertTrue( payload.contains( "\"openapi\" : \"3.0.3\"" ) );
      assertTrue( payload.contains( "\"version\" : \"v1\"" ) );
      assertTrue( payload.contains( "\"title\" : \"AspectModelForService\"" ) );
      assertTrue( payload.contains( "\"url\" : \"https://test.com/api/v1\"" ) );
      assertTrue( payload.contains( "\"get\" : " ) );
      assertTrue( payload.contains( "\"post\" : " ) );
      assertFalse( payload.contains( "\"patch\" : " ) );
      assertFalse( payload.contains( "\"put\" : " ) );
   }

   @Test
   void testAspectModelJsonOpenApiSpecWithIncludePatch() throws IOException {
      final Path storagePath = Path.of( eclipseTestPath.toString(), model );
      final String testModel = Files.readString( storagePath, StandardCharsets.UTF_8 );

      final OpenApiSchemaGenerationConfig config = new OpenApiSchemaGenerationConfig( Locale.forLanguageTag( "en" ),
            false, false,
            "https://test.com", "", new ObjectMapper().createObjectNode(), PagingOption.TIME_BASED_PAGING, false, false,
            false, false, true, null );

      final String payload = generateService.generateJsonOpenApiSpec( testModel, config );

      assertTrue( payload.contains( "\"openapi\" : \"3.0.3\"" ) );
      assertTrue( payload.contains( "\"version\" : \"v1\"" ) );
      assertTrue( payload.contains( "\"title\" : \"AspectModelForService\"" ) );
      assertTrue( payload.contains( "\"url\" : \"https://test.com/api/v1\"" ) );
      assertTrue( payload.contains( "\"get\" : " ) );
      assertFalse( payload.contains( "\"post\" : " ) );
      assertTrue( payload.contains( "\"patch\" : " ) );
      assertFalse( payload.contains( "\"put\" : " ) );
   }

   @Test
   void testAspectModelJsonOpenApiSpecWithIncludePut() throws IOException {
      final Path storagePath = Path.of( eclipseTestPath.toString(), model );
      final String testModel = Files.readString( storagePath, StandardCharsets.UTF_8 );

      final OpenApiSchemaGenerationConfig config = new OpenApiSchemaGenerationConfig( Locale.forLanguageTag( "en" ),
            false, false,
            "https://test.com", "", new ObjectMapper().createObjectNode(), PagingOption.TIME_BASED_PAGING, false, false,
            false, true, false, null );

      final String payload = generateService.generateJsonOpenApiSpec( testModel, config );

      assertTrue( payload.contains( "\"openapi\" : \"3.0.3\"" ) );
      assertTrue( payload.contains( "\"version\" : \"v1\"" ) );
      assertTrue( payload.contains( "\"title\" : \"AspectModelForService\"" ) );
      assertTrue( payload.contains( "\"url\" : \"https://test.com/api/v1\"" ) );
      assertTrue( payload.contains( "\"get\" : " ) );
      assertFalse( payload.contains( "\"post\" : " ) );
      assertFalse( payload.contains( "\"patch\" : " ) );
      assertTrue( payload.contains( "\"put\" : " ) );
   }

   @Test
   void testAspectModelYamlOpenApiSpecWithoutResourcePath() throws IOException {
      final Path storagePath = Path.of( eclipseTestPath.toString(), model );
      final String testModel = Files.readString( storagePath, StandardCharsets.UTF_8 );

      final OpenApiSchemaGenerationConfig config = new OpenApiSchemaGenerationConfig( Locale.forLanguageTag( "en" ),
            false, false,
            "https://test.com", "", new ObjectMapper( new YAMLFactory() ).createObjectNode(),
            PagingOption.TIME_BASED_PAGING, false, false,
            false, false, false, null );

      final String payload = generateService.generateYamlOpenApiSpec( testModel, config );

      assertTrue( payload.contains( "openapi: 3.0.3" ) );
      assertTrue( payload.contains( "title: AspectModel" ) );
      assertTrue( payload.contains( "version: v1" ) );
      assertTrue( payload.contains( "url: https://test.com/api/v1" ) );
   }

   @Test
   void testAspectModelYamlOpenApiSpecWithResourcePath() throws IOException {
      final Path storagePath = Path.of( eclipseTestPath.toString(), model );
      final String testModel = Files.readString( storagePath, StandardCharsets.UTF_8 );
      final ObjectMapper objectMapper = new ObjectMapper( new YAMLFactory() );

      final ObjectNode yamlProperties = (ObjectNode) objectMapper.readTree( """
               resourceId:
                       name: resourceId
                       in: path
                       description: An example resource Id.
                       required: true
                       schema:
                         type: string
            """ );

      final OpenApiSchemaGenerationConfig config = new OpenApiSchemaGenerationConfig( Locale.forLanguageTag( "en" ),
            false, false,
            "https://test.com", "/resource/{resourceId}", yamlProperties, PagingOption.TIME_BASED_PAGING, false, false,
            false, false, false, null );

      final String payload = generateService.generateYamlOpenApiSpec( testModel, config );

      assertTrue( payload.contains( "openapi: 3.0.3" ) );
      assertTrue( payload.contains( "title: AspectModel" ) );
      assertTrue( payload.contains( "version: v1" ) );
      assertTrue( payload.contains( "url: https://test.com/api/v1" ) );
      assertTrue( payload.contains( "/resource/{resourceId}" ) );
      assertTrue( payload.contains( "name: resourceId" ) );
      assertTrue( payload.contains( "in: path" ) );
   }

   @Test
   void testAspectModelYamlOpenApiSpecWithWrongResourcePathProperties() throws IOException {
      final Path storagePath = Path.of( eclipseTestPath.toString(), model );
      final String testModel = Files.readString( storagePath, StandardCharsets.UTF_8 );
      final ObjectMapper objectMapper = new ObjectMapper( new YAMLFactory() );

      final ObjectNode yamlProperties = (ObjectNode) objectMapper.readTree( """
               wrongId:
                       name: wrongId
                       in: path
                       description: An example resource Id.
                       required: true
                       schema:
                         type: string
            """ );

      final OpenApiSchemaGenerationConfig config = new OpenApiSchemaGenerationConfig( Locale.forLanguageTag( "en" ),
            false, false,
            "https://test.com", "/resource/{resourceId}", yamlProperties, PagingOption.TIME_BASED_PAGING, false, false,
            false, false, false, null );

      assertThrows( GenerationException.class, () -> generateService.generateYamlOpenApiSpec( testModel, config ) );
   }

   @Test
   void testAspectModelYamlOpenApiSpecWithIncludeCrud() throws IOException {
      final Path storagePath = Path.of( eclipseTestPath.toString(), model );
      final String testModel = Files.readString( storagePath, StandardCharsets.UTF_8 );

      final OpenApiSchemaGenerationConfig config = new OpenApiSchemaGenerationConfig( Locale.forLanguageTag( "en" ),
            false, false,
            "https://test.com", "", new ObjectMapper( new YAMLFactory() ).createObjectNode(),
            PagingOption.TIME_BASED_PAGING, false, true,
            false, false, false, null );

      final String payload = generateService.generateYamlOpenApiSpec( testModel, config );

      assertTrue( payload.contains( "openapi: 3.0.3" ) );
      assertTrue( payload.contains( "title: AspectModel" ) );
      assertTrue( payload.contains( "version: v1" ) );
      assertTrue( payload.contains( "url: https://test.com/api/v1" ) );
      assertTrue( payload.contains( "get:" ) );
      assertTrue( payload.contains( "post:" ) );
      assertTrue( payload.contains( "patch:" ) );
      assertTrue( payload.contains( "put:" ) );
   }

   @Test
   void testAspectModelYamlOpenApiSpecWithIncludePost() throws IOException {
      final Path storagePath = Path.of( eclipseTestPath.toString(), model );
      final String testModel = Files.readString( storagePath, StandardCharsets.UTF_8 );

      final OpenApiSchemaGenerationConfig config = new OpenApiSchemaGenerationConfig( Locale.forLanguageTag( "en" ),
            false, false,
            "https://test.com", "", new ObjectMapper( new YAMLFactory() ).createObjectNode(),
            PagingOption.TIME_BASED_PAGING, false, false,
            true, false, false, null );

      final String payload = generateService.generateYamlOpenApiSpec( testModel, config );

      assertTrue( payload.contains( "openapi: 3.0.3" ) );
      assertTrue( payload.contains( "title: AspectModel" ) );
      assertTrue( payload.contains( "version: v1" ) );
      assertTrue( payload.contains( "url: https://test.com/api/v1" ) );
      assertTrue( payload.contains( "get:" ) );
      assertTrue( payload.contains( "post:" ) );
      assertFalse( payload.contains( "patch:" ) );
      assertFalse( payload.contains( "put:" ) );
   }

   @Test
   void testAspectModelYamlOpenApiSpecWithIncludePatch() throws IOException {
      final Path storagePath = Path.of( eclipseTestPath.toString(), model );
      final String testModel = Files.readString( storagePath, StandardCharsets.UTF_8 );

      final OpenApiSchemaGenerationConfig config = new OpenApiSchemaGenerationConfig( Locale.forLanguageTag( "en" ),
            false, false,
            "https://test.com", "", new ObjectMapper( new YAMLFactory() ).createObjectNode(),
            PagingOption.TIME_BASED_PAGING, false, false,
            false, false, true, null );

      final String payload = generateService.generateYamlOpenApiSpec( testModel, config );

      assertTrue( payload.contains( "openapi: 3.0.3" ) );
      assertTrue( payload.contains( "title: AspectModel" ) );
      assertTrue( payload.contains( "version: v1" ) );
      assertTrue( payload.contains( "url: https://test.com/api/v1" ) );
      assertTrue( payload.contains( "get:" ) );
      assertFalse( payload.contains( "post:" ) );
      assertTrue( payload.contains( "patch:" ) );
      assertFalse( payload.contains( "put:" ) );
   }

   @Test
   void testAspectModelYamlOpenApiSpecWithIncludePut() throws IOException {
      final Path storagePath = Path.of( eclipseTestPath.toString(), model );
      final String testModel = Files.readString( storagePath, StandardCharsets.UTF_8 );

      final OpenApiSchemaGenerationConfig config = new OpenApiSchemaGenerationConfig( Locale.forLanguageTag( "en" ),
            false, false,
            "https://test.com", "", new ObjectMapper( new YAMLFactory() ).createObjectNode(),
            PagingOption.TIME_BASED_PAGING, false, false,
            false, true, false, null );

      final String payload = generateService.generateYamlOpenApiSpec( testModel, config );

      assertTrue( payload.contains( "openapi: 3.0.3" ) );
      assertTrue( payload.contains( "title: AspectModel" ) );
      assertTrue( payload.contains( "version: v1" ) );
      assertTrue( payload.contains( "url: https://test.com/api/v1" ) );
      assertTrue( payload.contains( "get:" ) );
      assertFalse( payload.contains( "post:" ) );
      assertFalse( payload.contains( "patch:" ) );
      assertTrue( payload.contains( "put:" ) );
   }

   @Test
   void testAspectModelAASX() throws IOException {
      final Path storagePath = Path.of( eclipseTestPath.toString(), model );
      final String testModel = Files.readString( storagePath, StandardCharsets.UTF_8 );

      final String payload = generateService.generateAASXFile( testModel );

      assertTrue( payload.contains( "aasx" ) );
   }

   @Test
   void testAspectModelAASXml() throws IOException {
      final Path storagePath = Path.of( eclipseTestPath.toString(), model );
      final String testModel = Files.readString( storagePath, StandardCharsets.UTF_8 );

      final String payload = generateService.generateAasXmlFile( testModel );

      assertTrue( payload.contains( "<?xml version='1.0' encoding='UTF-8'?>" ) );
      assertTrue( payload.contains( "https://admin-shell.io/aas/3/0" ) );
   }

   @Test
   void testAspectModelAASJson() throws IOException {
      final Path storagePath = Path.of( eclipseTestPath.toString(), model );
      final String testModel = Files.readString( storagePath, StandardCharsets.UTF_8 );

      final String payload = generateService.generateAasJsonFile( testModel );

      assertTrue( payload.contains( "{" ) );
      assertTrue( payload.contains( "}" ) );
      assertTrue( payload.contains( "assetAdministrationShells" ) );
      assertTrue( payload.contains( "submodels" ) );
   }

   @Test
   void testAspectModelJsonAsyncApiSpec() throws IOException {
      final Path storagePath = Path.of( eclipseTestPath.toString(), model );
      final String testModel = Files.readString( storagePath, StandardCharsets.UTF_8 );

      final String payload = new String(
            generateService.generateAsyncApiSpec( testModel, "en", "json", "application:id", "foo/bar", false, false ),
            StandardCharsets.UTF_8 );

      assertTrue( payload.contains( "\"asyncapi\":\"3.0.0\"" ) );
      assertTrue( payload.contains( "\"id\":\"application:id\"" ) );
      assertTrue( payload.contains( "\"title\":\"AspectModelForService MQTT API\"" ) );
      assertTrue( payload.contains( "\"version\":\"v1\"" ) );
      assertTrue( payload.contains( "\"defaultContentType\":\"application/json\"" ) );
      assertTrue( payload.contains( "\"address\":\"foo/bar\"" ) );
   }

   @Test
   void testAspectModelJsonAsyncApiSpecWithSeparateFiles() throws IOException {
      final Path storagePath = Path.of( eclipseTestPath.toString(), model );
      final String testModel = Files.readString( storagePath, StandardCharsets.UTF_8 );

      final byte[] payload = generateService.generateAsyncApiSpec( testModel, "en", "json", "application:id", "foo/bar",
            false, true );

      assertTrue( isValidZipFile( payload ) );
   }

   @Test
   void testAspectModelYAMLAsyncApiSpec() throws IOException {
      final Path storagePath = Path.of( eclipseTestPath.toString(), model );
      final String testModel = Files.readString( storagePath, StandardCharsets.UTF_8 );

      final String payload = new String(
            generateService.generateAsyncApiSpec( testModel, "en", "yaml", "application:id", "foo/bar", false, false ),
            StandardCharsets.UTF_8 );

      assertTrue( payload.contains( "asyncapi: 3.0.0" ) );
      assertTrue( payload.contains( "id: application:id" ) );
      assertTrue( payload.contains( "title: AspectModelForService MQTT API" ) );
      assertTrue( payload.contains( "version: v1" ) );
      assertTrue( payload.contains( "defaultContentType: application/json" ) );
      assertTrue( payload.contains( "address: foo/bar" ) );
   }

   @Test
   void testAspectModelYAMLAsyncApiSpecWithSeparateFiles() throws IOException {
      final Path storagePath = Path.of( eclipseTestPath.toString(), model );
      final String testModel = Files.readString( storagePath, StandardCharsets.UTF_8 );

      final byte[] payload = generateService.generateAsyncApiSpec( testModel, "en", "yaml", "application:id", "foo/bar",
            false, true );

      assertTrue( isValidZipFile( payload ) );
   }

   private boolean isValidZipFile( final byte[] payload ) {
      try ( final ZipInputStream zis = new ZipInputStream( new ByteArrayInputStream( payload ) ) ) {
         return zis.getNextEntry() != null;
      } catch ( final IOException e ) {
         return false;
      }
   }
}
