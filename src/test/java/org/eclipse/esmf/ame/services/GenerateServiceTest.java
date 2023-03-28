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
import static org.mockito.ArgumentMatchers.any;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.eclipse.esmf.ame.model.ValidationProcess;
import org.eclipse.esmf.ame.repository.strategy.LocalFolderResolverStrategy;
import org.eclipse.esmf.aspectmodel.generator.openapi.PagingOption;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith( SpringExtension.class )
@SpringBootTest
@DirtiesContext( classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD )
class GenerateServiceTest {

   @Autowired
   private GenerateService generateService;

   private static final Path resourcesPath = Path.of( "src", "test", "resources" );
   private static final Path eclipseTestPath = Path.of( resourcesPath.toString(), "org.eclipse.esmf.example",
         "1.0.0" );

   private static final String aspectModelFile = "AspectModel.ttl";

   @Test
   void testAspectModelJsonSample() throws IOException {
      try ( final MockedStatic<LocalFolderResolverStrategy> utilities = Mockito.mockStatic(
            LocalFolderResolverStrategy.class ) ) {
         final Path storagePath = Path.of( eclipseTestPath.toString(), aspectModelFile );

         utilities.when( () -> LocalFolderResolverStrategy.transformToValidModelDirectory( any() ) )
                  .thenReturn( storagePath.toString() );

         final ValidationProcess validationProcess = Mockito.mock( ValidationProcess.class );
         Mockito.when( validationProcess.getPath() ).thenReturn( resourcesPath );

         final String payload = generateService.sampleJSONPayload(
               Files.readString( storagePath, StandardCharsets.UTF_8 ), validationProcess );
         assertEquals( "{\"property\":\"eOMtThyhVNLWUZNRcBaQKxI\"}", payload );
      }
   }

   @Test
   void testAspectModelJsonSchema() throws IOException {
      try ( final MockedStatic<LocalFolderResolverStrategy> utilities = Mockito.mockStatic(
            LocalFolderResolverStrategy.class ) ) {
         final Path storagePath = Path.of( eclipseTestPath.toString(), aspectModelFile );

         utilities.when( () -> LocalFolderResolverStrategy.transformToValidModelDirectory( any() ) )
                  .thenReturn( storagePath.toString() );

         final ValidationProcess validationProcess = Mockito.mock( ValidationProcess.class );
         Mockito.when( validationProcess.getPath() ).thenReturn( resourcesPath );

         final String payload = generateService.jsonSchema( Files.readString( storagePath, StandardCharsets.UTF_8 ),
               validationProcess, "en-EN" );
         assertTrue(
               payload.contains( "#/components/schemas/urn_samm_org.eclipse.esmf.example_1.0.0_Characteristic" ) );
      }
   }

   @Test
   void testAspectModelJsonOpenApiSpec() throws IOException {
      try ( final MockedStatic<LocalFolderResolverStrategy> utilities = Mockito.mockStatic(
            LocalFolderResolverStrategy.class ) ) {
         final Path storagePath = Path.of( eclipseTestPath.toString(), aspectModelFile );

         utilities.when( () -> LocalFolderResolverStrategy.transformToValidModelDirectory( any() ) )
                  .thenReturn( storagePath.toString() );

         final String payload = generateService.generateJsonOpenApiSpec( "en",
               Files.readString( storagePath, StandardCharsets.UTF_8 ), "https://test.com", false, false,
               Optional.of( PagingOption.TIME_BASED_PAGING ) );

         assertTrue( payload.contains( "\"openapi\" : \"3.0.3\"" ) );
         assertTrue( payload.contains( "\"version\" : \"v1\"" ) );
         assertTrue( payload.contains( "\"title\" : \"AspectModel\"" ) );
         assertTrue( payload.contains( "\"url\" : \"https://test.com/api/v1\"" ) );
      }
   }

   @Test
   void testAspectModelYamlOpenApiSpec() throws IOException {
      try ( final MockedStatic<LocalFolderResolverStrategy> utilities = Mockito.mockStatic(
            LocalFolderResolverStrategy.class ) ) {
         final Path storagePath = Path.of( eclipseTestPath.toString(), aspectModelFile );

         utilities.when( () -> LocalFolderResolverStrategy.transformToValidModelDirectory( any() ) )
                  .thenReturn( storagePath.toString() );

         final String payload = generateService.generateYamlOpenApiSpec( "en",
               Files.readString( storagePath, StandardCharsets.UTF_8 ), "https://test.com", false, false,
               Optional.of( PagingOption.TIME_BASED_PAGING ) );

         assertTrue( payload.contains( "openapi: 3.0.3" ) );
         assertTrue( payload.contains( "title: AspectModel" ) );
         assertTrue( payload.contains( "version: v1" ) );
         assertTrue( payload.contains( "url: https://test.com/api/v1" ) );
      }
   }
}
