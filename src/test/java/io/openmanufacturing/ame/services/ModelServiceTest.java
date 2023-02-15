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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import io.openmanufacturing.ame.exceptions.FileNotFoundException;
import io.openmanufacturing.ame.model.ValidationProcess;
import io.openmanufacturing.ame.model.migration.Namespaces;
import io.openmanufacturing.ame.model.validation.ViolationReport;
import io.openmanufacturing.ame.repository.strategy.LocalFolderResolverStrategy;

@ExtendWith( SpringExtension.class )
@SpringBootTest
@DirtiesContext( classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD )
class ModelServiceTest {

   @Autowired
   private ModelService modelService;

   private final String namespace = StringUtils.EMPTY;
   private static final Path resourcesPath = Path.of( "src", "test", "resources" );
   private static final Path openManufacturingTestPath = Path.of( resourcesPath.toString(), "io.openmanufacturing",
         "1.0.0" );
   private static final Path migrationWorkspacePath = Path.of( resourcesPath.toString(), "workspace-to-migrate" );
   private static final Path toMigrationWorkspaceOne = Path.of( migrationWorkspacePath.toString(),
         "io.migrate-workspace-one", "1.0.0" );
   private static final Path toMigrationWorkspaceTwo = Path.of( migrationWorkspacePath.toString(),
         "io.migrate-workspace-two", "1.0.0" );

   private static final String aspectModelFile = "AspectModel.ttl";
   private static final String oldAspectModelFile = "OldAspectModel.ttl";

   @Test
   void testGetModel() throws IOException {
      try ( final MockedStatic<LocalFolderResolverStrategy> utilities = Mockito.mockStatic(
            LocalFolderResolverStrategy.class ) ) {
         final Path storagePath = Path.of( openManufacturingTestPath.toString(), aspectModelFile );
         utilities.when( () -> LocalFolderResolverStrategy.transformToValidModelDirectory( any() ) )
                  .thenReturn( storagePath.toString() );

         final String result = modelService.getModel( namespace, Optional.of( storagePath.toString() ) );
         assertEquals( result, Files.readString( storagePath ) );
      }
   }

   @Test()
   void testGetModelThrowsIOException() {
      try ( final MockedStatic<LocalFolderResolverStrategy> utilities = Mockito.mockStatic(
            LocalFolderResolverStrategy.class ) ) {
         final Path storagePath = Path.of( openManufacturingTestPath.toString(), "NoFile.ttl" );

         utilities.when( () -> LocalFolderResolverStrategy.transformToValidModelDirectory( any() ) )
                  .thenReturn( storagePath.toString() );

         assertThrows( FileNotFoundException.class, () -> modelService.getModel( namespace, Optional.empty() ) );
      }
   }

   @Test
   void testValidateNewModel() throws IOException {
      try ( final MockedStatic<LocalFolderResolverStrategy> utilities = Mockito.mockStatic(
            LocalFolderResolverStrategy.class ) ) {
         final Path storagePath = Path.of( openManufacturingTestPath.toString(), aspectModelFile );

         utilities.when( () -> LocalFolderResolverStrategy.transformToValidModelDirectory( any() ) )
                  .thenReturn( storagePath.toString() );

         final ValidationProcess validationProcess = Mockito.mock( ValidationProcess.class );

         Mockito.when( validationProcess.getPath() ).thenReturn( resourcesPath );

         final ViolationReport validateReport = modelService.validateModel(
               Files.readString( storagePath, StandardCharsets.UTF_8 ), validationProcess );
         assertTrue( validateReport.getViolationErrors().isEmpty() );
      }
   }

   @Test
   void testSaveModel() throws IOException {
      try ( final MockedStatic<LocalFolderResolverStrategy> utilities = Mockito.mockStatic(
            LocalFolderResolverStrategy.class ) ) {
         final Path fileToReplace = Path.of( openManufacturingTestPath.toString(), aspectModelFile );

         final String turtleData = Files.readString( fileToReplace, StandardCharsets.UTF_8 );

         utilities.when( () -> LocalFolderResolverStrategy.transformToValidModelDirectory( any() ) )
                  .thenReturn( resourcesPath.toString() );

         utilities.when( () -> LocalFolderResolverStrategy.transformToValidModelDirectory( any() ) )
                  .thenReturn( fileToReplace.toString() );

         final String result = modelService.saveModel( Optional.of( "" ), turtleData, Optional.empty() );
         assertEquals( result, Path.of( "io.openmanufacturing", "1.0.0", aspectModelFile ).toString() );
      }
   }

   @Test()
   void testDeleteModel() throws IOException {
      try ( final MockedStatic<LocalFolderResolverStrategy> utilities = Mockito.mockStatic(
            LocalFolderResolverStrategy.class ) ) {
         final Path fileToReplace = Path.of( openManufacturingTestPath.toString(), aspectModelFile );

         final String turtleData = Files.readString( fileToReplace, StandardCharsets.UTF_8 )
                                        .replace( "AspectModel", "SavedModel" );

         utilities.when( () -> LocalFolderResolverStrategy.transformToValidModelDirectory( any() ) )
                  .thenReturn( resourcesPath.toString() );

         utilities.when( () -> LocalFolderResolverStrategy.transformToValidModelDirectory( any() ) )
                  .thenReturn( fileToReplace.toString() );

         modelService.saveModel( Optional.of( "" ), turtleData, Optional.empty() );
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

         assertThrows( FileNotFoundException.class, () -> modelService.getModel( namespace, Optional.empty() ) );
      }
   }

   @Test
   void testGetAllNamespaces() {
      final ValidationProcess validationProcess = Mockito.mock( ValidationProcess.class );

      Mockito.when( validationProcess.getPath() ).thenReturn( resourcesPath );

      final Map<String, List<String>> result = modelService.getAllNamespaces( true, validationProcess );
      assertEquals( 6, result.size() );
   }

   @Test()
   void testGetAllNamespacesThrowsIOException() {
      final Path storagePath = Paths.get( "src", "test", "noResources" );
      final ValidationProcess validationProcess = Mockito.mock( ValidationProcess.class );

      Mockito.when( validationProcess.getPath() ).thenReturn( storagePath );

      assertThrows( FileNotFoundException.class, () -> modelService.getAllNamespaces( true, validationProcess ) );
   }

   @Test
   void testMigrateModel() throws IOException {
      try ( final MockedStatic<LocalFolderResolverStrategy> utilities = Mockito.mockStatic(
            LocalFolderResolverStrategy.class ) ) {
         final Path storagePath = Path.of( openManufacturingTestPath.toString(), oldAspectModelFile );

         utilities.when( () -> LocalFolderResolverStrategy.transformToValidModelDirectory( any() ) )
                  .thenReturn( storagePath.toString() );

         final ValidationProcess validationProcess = Mockito.mock( ValidationProcess.class );
         Mockito.when( validationProcess.getPath() ).thenReturn( storagePath );

         final String migratedModel = modelService.migrateModel(
               Files.readString( storagePath, StandardCharsets.UTF_8 ), validationProcess );

         checkMigratedModel( migratedModel );
      }
   }

   @Test
   void testMigrateWorkspaceWithoutVersionUpgrade() throws IOException {
      try ( final MockedStatic<LocalFolderResolverStrategy> strategyUtilities = Mockito.mockStatic(
            LocalFolderResolverStrategy.class );
            final MockedStatic<FileUtils> fileUtilities = Mockito.mockStatic( FileUtils.class ) ) {
         final Path storagePath = migrationWorkspacePath.toAbsolutePath();
         final File OneToMigrateOne = new File( toMigrationWorkspaceOne + File.separator + "ToMigrateOne.ttl" );
         final File OneToMigrateTwo = new File( toMigrationWorkspaceOne + File.separator + "ToMigrateTwo.ttl" );
         final File TwoToMigrateOne = new File( toMigrationWorkspaceTwo + File.separator + "ToMigrateOne.ttl" );
         final File TwoToMigrateTwo = new File( toMigrationWorkspaceTwo + File.separator + "ToMigrateTwo.ttl" );

         fileUtilities.when( () -> FileUtils.listFiles( any( File.class ), any(), anyBoolean() ) )
                      .thenReturn( List.of( OneToMigrateOne, OneToMigrateTwo, TwoToMigrateOne, TwoToMigrateTwo ) );

         strategyUtilities.when( () -> LocalFolderResolverStrategy.transformToValidModelDirectory( any() ) )
                          .thenReturn( storagePath.toString() );

         final Namespaces namespaces = modelService.migrateWorkspace( storagePath );

         assertEquals( 2, namespaces.namespaces.size() );
         assertEquals( "io.migrate-workspace-one:1.0.0", namespaces.namespaces.get( 0 ).versionedNamespace );
         assertEquals( 2, namespaces.namespaces.get( 0 ).files.size() );
         assertEquals( "ToMigrateOne.ttl", namespaces.namespaces.get( 0 ).files.get( 0 ).getName() );
         assertEquals( true, namespaces.namespaces.get( 0 ).files.get( 0 ).getSuccess() );
         assertEquals( "ToMigrateTwo.ttl", namespaces.namespaces.get( 0 ).files.get( 1 ).getName() );
         assertEquals( true, namespaces.namespaces.get( 0 ).files.get( 1 ).getSuccess() );

         assertEquals( "io.migrate-workspace-two:1.0.0", namespaces.namespaces.get( 1 ).versionedNamespace );
         assertEquals( 2, namespaces.namespaces.get( 1 ).files.size() );
         assertEquals( "ToMigrateOne.ttl", namespaces.namespaces.get( 1 ).files.get( 0 ).getName() );
         assertEquals( true, namespaces.namespaces.get( 1 ).files.get( 0 ).getSuccess() );
         assertEquals( "ToMigrateTwo.ttl", namespaces.namespaces.get( 1 ).files.get( 1 ).getName() );
         assertEquals( true, namespaces.namespaces.get( 1 ).files.get( 1 ).getSuccess() );

         final String migratedModelOne = Files.readString( OneToMigrateOne.toPath(), StandardCharsets.UTF_8 );
         final String migratedModelTwo = Files.readString( OneToMigrateTwo.toPath(), StandardCharsets.UTF_8 );
         final String migratedModelThree = Files.readString( TwoToMigrateOne.toPath(), StandardCharsets.UTF_8 );
         final String migratedModelFour = Files.readString( TwoToMigrateTwo.toPath(), StandardCharsets.UTF_8 );

         checkMigratedModel( migratedModelOne );
         checkMigratedModel( migratedModelTwo );
         checkMigratedModel( migratedModelThree );
         checkMigratedModel( migratedModelFour );
      }
   }

   private void checkMigratedModel( final String migratedModel ) {
      assertTrue( migratedModel.contains( "@prefix bamm: <urn:bamm:io.openmanufacturing:meta-model:2.0.0#>" ) );
      assertTrue( migratedModel.contains( "@prefix bamm-c: <urn:bamm:io.openmanufacturing:characteristic:2.0.0#>" ) );
      assertTrue( migratedModel.contains( "@prefix bamm-e: <urn:bamm:io.openmanufacturing:entity:2.0.0#>" ) );
      assertTrue( migratedModel.contains( "@prefix unit: <urn:bamm:io.openmanufacturing:unit:2.0.0#>" ) );
   }
}
