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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.eclipse.esmf.ame.config.TestConfig;
import org.eclipse.esmf.aspectmodel.generator.openapi.PagingOption;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

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

      assertTrue( payload.contains( "#/components/schemas/urn_samm_org.eclipse.esmf.example_1.0.0_Characteristic" ) );
   }

   @Test
   void testAspectModelJsonOpenApiSpec() throws IOException {
      final Path storagePath = Path.of( eclipseTestPath.toString(), model );
      final String testModel = Files.readString( storagePath, StandardCharsets.UTF_8 );

      final String payload = generateService.generateJsonOpenApiSpec( "en", testModel, "https://test.com", false, false,
            Optional.of( PagingOption.TIME_BASED_PAGING ) );

      assertTrue( payload.contains( "\"openapi\" : \"3.0.3\"" ) );
      assertTrue( payload.contains( "\"version\" : \"v1\"" ) );
      assertTrue( payload.contains( "\"title\" : \"AspectModelForService\"" ) );
      assertTrue( payload.contains( "\"url\" : \"https://test.com/api/v1\"" ) );
   }

   @Test
   void testAspectModelYamlOpenApiSpec() throws IOException {
      final Path storagePath = Path.of( eclipseTestPath.toString(), model );
      final String testModel = Files.readString( storagePath, StandardCharsets.UTF_8 );

      final String payload = generateService.generateYamlOpenApiSpec( "en", testModel, "https://test.com", false, false,
            Optional.of( PagingOption.TIME_BASED_PAGING ) );

      assertTrue( payload.contains( "openapi: 3.0.3" ) );
      assertTrue( payload.contains( "title: AspectModel" ) );
      assertTrue( payload.contains( "version: v1" ) );
      assertTrue( payload.contains( "url: https://test.com/api/v1" ) );
   }

   @Test
   void testAspectModelAASX() throws IOException {
      final Path storagePath = Path.of( eclipseTestPath.toString(), model );
      final String testModel = Files.readString( storagePath, StandardCharsets.UTF_8 );

      final String payload = generateService.generateAASXFile( testModel );

      assertTrue( payload.contains( "aasx" ) );
   }

   @Test
   void testAspectModelAASXML() throws IOException {
      final Path storagePath = Path.of( eclipseTestPath.toString(), model );
      final String testModel = Files.readString( storagePath, StandardCharsets.UTF_8 );

      final String payload = generateService.generateAasXmlFile( testModel );

      assertTrue( payload.contains( "<?xml version='1.0' encoding='UTF-8'?>" ) );
      assertTrue( payload.contains( "https://admin-shell.io/aas/3/0" ) );
   }
}
