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

package io.openmanufacturing.ame.services;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import io.openmanufacturing.ame.repository.strategy.LocalFolderResolverStrategy;

@RunWith( SpringRunner.class )
@SpringBootTest
public class GenerateServiceTest {

   @Autowired
   private GenerateService generateService;

   private static final Path resourcesPath = Path.of( "src", "test", "resources" );
   private static final Path openManufacturingTestPath = Path.of( resourcesPath.toString(), "io.openmanufacturing",
         "1.0.0" );

   private static final String aspectModelFile = "AspectModel.ttl";

   @Test
   public void testAspectModelJsonSample() throws IOException {
      try ( final MockedStatic<LocalFolderResolverStrategy> utilities = Mockito.mockStatic(
            LocalFolderResolverStrategy.class ) ) {
         final Path storagePath = Path.of( openManufacturingTestPath.toString(), aspectModelFile );

         utilities.when( () -> LocalFolderResolverStrategy.transformToValidModelDirectory( any() ) )
                  .thenReturn( storagePath.toString() );

         final String payload = generateService.sampleJSONPayload(
               Files.readString( storagePath, StandardCharsets.UTF_8 ), Optional.empty() );
         assertEquals( "{\"property\":\"eOMtThyhVNLWUZNRcBaQKxI\"}", payload );
      }
   }

   @Test
   public void testAspectModelJsonSchema() throws IOException {
      try ( final MockedStatic<LocalFolderResolverStrategy> utilities = Mockito.mockStatic(
            LocalFolderResolverStrategy.class ) ) {
         final Path storagePath = Path.of( openManufacturingTestPath.toString(), aspectModelFile );

         utilities.when( () -> LocalFolderResolverStrategy.transformToValidModelDirectory( any() ) )
                  .thenReturn( storagePath.toString() );

         final String payload = generateService.jsonSchema(
               Files.readString( storagePath, StandardCharsets.UTF_8 ), Optional.empty() );
         assertTrue( payload.contains( "#/components/schemas/urn_bamm_io.openmanufacturing_1.0.0_Characteristic" ) );
      }
   }
}
