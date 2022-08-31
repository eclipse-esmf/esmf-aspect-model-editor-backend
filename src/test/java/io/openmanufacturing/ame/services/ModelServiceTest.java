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
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import io.openmanufacturing.ame.exceptions.FileNotFoundException;
import io.openmanufacturing.ame.repository.strategy.LocalFolderResolverStrategy;
import io.openmanufacturing.sds.aspectmodel.validation.report.ValidationReport;

@RunWith( SpringRunner.class )
@SpringBootTest
public class ModelServiceTest {

   @Autowired
   private ModelService modelService;

   final String namespace = StringUtils.EMPTY;

   private static final Path resourcesPath = Path.of( "src", "test", "resources" );
   private static final Path openManufacturingTestPath = Path.of( resourcesPath.toString(), "io.openmanufacturing",
         "1.0.0" );

   private static final String aspectModelFile = "AspectModel.ttl";
   private static final String oldAspectModelFile = "OldAspectModel.ttl";

   @Test
   public void testGetModel() throws IOException {
      try ( final MockedStatic<LocalFolderResolverStrategy> utilities = Mockito.mockStatic(
            LocalFolderResolverStrategy.class ) ) {
         final Path storagePath = Path.of( openManufacturingTestPath.toString(), aspectModelFile );
         utilities.when( () -> LocalFolderResolverStrategy.transformToValidModelDirectory( any() ) )
                  .thenReturn( storagePath.toString() );

         final String result = modelService.getModel( namespace, Optional.of( storagePath.toString() ) );
         assertEquals( result, Files.readString( storagePath ) );
      }
   }

   @Test( expected = FileNotFoundException.class )
   public void testGetModelThrowsIOException() {
      try ( final MockedStatic<LocalFolderResolverStrategy> utilities = Mockito.mockStatic(
            LocalFolderResolverStrategy.class ) ) {
         final Path storagePath = Path.of( openManufacturingTestPath.toString(), "NoFile.ttl" );

         utilities.when( () -> LocalFolderResolverStrategy.transformToValidModelDirectory( any() ) )
                  .thenReturn( storagePath.toString() );
         modelService.getModel( namespace, Optional.empty() );
      }
   }

   @Test
   public void testValidateNewModel() throws IOException {
      try ( final MockedStatic<LocalFolderResolverStrategy> utilities = Mockito.mockStatic(
            LocalFolderResolverStrategy.class ) ) {
         final Path storagePath = Path.of( openManufacturingTestPath.toString(), aspectModelFile );

         utilities.when( () -> LocalFolderResolverStrategy.transformToValidModelDirectory( any() ) )
                  .thenReturn( storagePath.toString() );

         final ValidationReport validationReport = modelService.validateModel(
               Files.readString( storagePath, StandardCharsets.UTF_8 ), storagePath.toString() );
         assertEquals( "Validation report: Input model is valid", validationReport.toString() );
      }
   }

   @Test
   public void testSaveModel() throws IOException {
      try ( final MockedStatic<LocalFolderResolverStrategy> utilities = Mockito.mockStatic(
            LocalFolderResolverStrategy.class ) ) {
         final Path fileToReplace = Path.of( openManufacturingTestPath.toString(), aspectModelFile );

         final String turtleData = Files.readString( fileToReplace, StandardCharsets.UTF_8 );

         utilities.when( () -> LocalFolderResolverStrategy.transformToValidModelDirectory( any() ) )
                  .thenReturn( resourcesPath.toString() );

         final String result = modelService.saveModel( Optional.of( "" ), turtleData );
         assertEquals( result, Path.of( "io.openmanufacturing", "1.0.0", aspectModelFile ).toString() );
      }
   }

   @Test( expected = FileNotFoundException.class )
   public void testDeleteModel() throws IOException {
      try ( final MockedStatic<LocalFolderResolverStrategy> utilities = Mockito.mockStatic(
            LocalFolderResolverStrategy.class ) ) {
         final Path fileToReplace = Path.of( openManufacturingTestPath.toString(), aspectModelFile );

         final String turtleData = Files.readString( fileToReplace, StandardCharsets.UTF_8 )
                                        .replace( "AspectModel", "SavedModel" );

         utilities.when( () -> LocalFolderResolverStrategy.transformToValidModelDirectory( any() ) )
                  .thenReturn( resourcesPath.toString() );

         modelService.saveModel( Optional.of( "" ), turtleData );
      }

      try ( final MockedStatic<LocalFolderResolverStrategy> utilities = Mockito.mockStatic(
            LocalFolderResolverStrategy.class ) ) {
         final Path storagePath = Path.of( openManufacturingTestPath.toString(), aspectModelFile );

         utilities.when( () -> LocalFolderResolverStrategy.transformToValidModelDirectory( any() ) )
                  .thenReturn( storagePath.toString() );

         modelService.deleteModel( namespace );
      }

      try ( final MockedStatic<LocalFolderResolverStrategy> utilities = Mockito.mockStatic(
            LocalFolderResolverStrategy.class ) ) {
         final Path storagePath = Path.of( openManufacturingTestPath.toString(), aspectModelFile );

         utilities.when( () -> LocalFolderResolverStrategy.transformToValidModelDirectory( any() ) )
                  .thenReturn( storagePath.toString() );

         modelService.getModel( namespace, Optional.empty() );
      }
   }

   @Test
   public void testGetAllNamespaces() {
      final Map<String, List<String>> result = modelService.getAllNamespaces( true,
            Optional.of( resourcesPath.toFile().getAbsolutePath() ) );
      assertEquals( result.size(), 2 );
   }

   @Test( expected = FileNotFoundException.class )
   public void testGetAllNamespacesThrowsIOException() {
      final Path storagePath = Paths.get( "src", "test", "noResources" );
      modelService.getAllNamespaces( true, Optional.of( storagePath.toFile().getAbsolutePath() ) );
   }

   @Test
   public void testMigrateModel() throws IOException {
      try ( final MockedStatic<LocalFolderResolverStrategy> utilities = Mockito.mockStatic(
            LocalFolderResolverStrategy.class ) ) {
         final Path storagePath = Path.of( openManufacturingTestPath.toString(), oldAspectModelFile );

         utilities.when( () -> LocalFolderResolverStrategy.transformToValidModelDirectory( any() ) )
                  .thenReturn( storagePath.toString() );

         final String migrateModel = modelService.migrateModel(
               Files.readString( storagePath, StandardCharsets.UTF_8 ), storagePath.toString() );
         assertTrue( migrateModel.contains( "@prefix bamm: <urn:bamm:io.openmanufacturing:meta-model:2.0.0#>" ) );
         assertTrue( migrateModel.contains( "@prefix bamm-c: <urn:bamm:io.openmanufacturing:characteristic:2.0.0#>" ) );
         assertTrue( migrateModel.contains( "@prefix bamm-e: <urn:bamm:io.openmanufacturing:entity:2.0.0#>" ) );
         assertTrue( migrateModel.contains( "@prefix unit: <urn:bamm:io.openmanufacturing:unit:2.0.0#>" ) );
      }
   }
}
