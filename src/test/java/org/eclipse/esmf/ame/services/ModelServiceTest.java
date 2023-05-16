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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.eclipse.esmf.ame.exceptions.FileNotFoundException;
import org.eclipse.esmf.ame.model.ProcessPath;
import org.eclipse.esmf.ame.model.migration.Namespaces;
import org.eclipse.esmf.ame.model.validation.ViolationReport;
import org.eclipse.esmf.ame.repository.strategy.LocalFolderResolverStrategy;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith( SpringExtension.class )
@SpringBootTest
@DirtiesContext( classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD )
class ModelServiceTest {

   @Autowired
   private ModelService modelService;

   private static final String VERSION = "1.0.0";
   private static final String EXAMPLE_NAMESPACE = "org.eclipse.esmf.example";
   private static final String NAMESPACE_VERSION = EXAMPLE_NAMESPACE + ":" + VERSION;
   private static final Path RESOURCE_PATH = Path.of( "src", "test", "resources" );
   private static final Path TEST_NAMESPACE_PATH = Path.of( RESOURCE_PATH.toString(), EXAMPLE_NAMESPACE, VERSION );
   private static final Path MIGRATION_WORKSPACE_PATH = Path.of( RESOURCE_PATH.toString(), "workspace-to-migrate" );
   private static final Path TO_MIGRATE_WORKSPACE_ONE = Path.of( MIGRATION_WORKSPACE_PATH.toString(),
         "io.migrate-workspace-one", VERSION );
   private static final Path TO_MIGRATE_WORKSPACE_TWO = Path.of( MIGRATION_WORKSPACE_PATH.toString(),
         "io.migrate-workspace-two", VERSION );

   private static final String TEST_MODEL = "AspectModel.ttl";
   private static final String TEST_MODEL_TO_DELTE = "FileToDelete.ttl";
   private static final String TEST_MODEL_NOT_FOUND = "NOTFOUND.ttl";

   @Test
   void testGetModel() throws IOException {
      try ( final MockedStatic<LocalFolderResolverStrategy> utilities = Mockito.mockStatic(
            LocalFolderResolverStrategy.class ) ) {
         utilities.when( () -> LocalFolderResolverStrategy.transformToValidModelDirectory( any() ) )
                  .thenReturn( RESOURCE_PATH.toString() );

         ProcessPath mockedEnum = Mockito.mock(ProcessPath.class);
         when(mockedEnum.getPath()).thenReturn(RESOURCE_PATH);


         final String result = modelService.getModel( NAMESPACE_VERSION, TEST_MODEL );

         assertEquals( result, Files.readString( TEST_NAMESPACE_PATH.resolve( TEST_MODEL ) ) );
      }
   }

   @Test()
   void testGetModelThrowsIOException() {
      try ( final MockedStatic<LocalFolderResolverStrategy> utilities = Mockito.mockStatic(
            LocalFolderResolverStrategy.class ) ) {
         utilities.when( () -> LocalFolderResolverStrategy.transformToValidModelDirectory( any() ) )
                  .thenReturn( RESOURCE_PATH.toString() );

         assertThrows( FileNotFoundException.class,
               () -> modelService.getModel( NAMESPACE_VERSION, TEST_MODEL_NOT_FOUND) );
      }
   }

   @Test
   void testValidateNewModel() throws IOException {
      try ( final MockedStatic<LocalFolderResolverStrategy> utilities = Mockito.mockStatic(
            LocalFolderResolverStrategy.class ) ) {
         final Path storagePath = Path.of( TEST_NAMESPACE_PATH.toString(), TEST_MODEL );

         utilities.when( () -> LocalFolderResolverStrategy.transformToValidModelDirectory( any() ) )
                  .thenReturn( storagePath.toString() );

         final ProcessPath processPath = Mockito.mock( ProcessPath.class );

         when( processPath.getPath() ).thenReturn( RESOURCE_PATH );

         final ViolationReport validateReport = modelService.validateModel( Files.readString( storagePath, StandardCharsets.UTF_8 ) );
         assertTrue( validateReport.getViolationErrors().isEmpty() );
      }
   }

   @Test
   void testSaveModel() throws IOException {
      try ( final MockedStatic<LocalFolderResolverStrategy> utilities = Mockito.mockStatic(
            LocalFolderResolverStrategy.class ) ) {
         final Path fileToReplace = Path.of( TEST_NAMESPACE_PATH.toString(), TEST_MODEL );

         final String turtleData = Files.readString( fileToReplace, StandardCharsets.UTF_8 );

         utilities.when( () -> LocalFolderResolverStrategy.transformToValidModelDirectory( any() ) )
                  .thenReturn( RESOURCE_PATH.toString() );

         utilities.when( () -> LocalFolderResolverStrategy.transformToValidModelDirectory( any() ) )
                  .thenReturn( fileToReplace.toString() );

         final String result = modelService.saveModel( Optional.of( NAMESPACE_VERSION ), Optional.of( TEST_MODEL ),
               turtleData );
         assertEquals( result, Path.of( EXAMPLE_NAMESPACE, VERSION, TEST_MODEL ).toString() );
      }
   }

   @Test()
   void testDeleteModel() {
         modelService.deleteModel( NAMESPACE_VERSION, TEST_MODEL_TO_DELTE );
         assertThrows( FileNotFoundException.class, () -> modelService.getModel( NAMESPACE_VERSION, TEST_MODEL_TO_DELTE ) );
   }

   @Disabled( "Should be reactivated as soon as there is something to migrate again." )
   @Test
   void testMigrateModel() throws IOException {
      try ( final MockedStatic<LocalFolderResolverStrategy> utilities = Mockito.mockStatic(
            LocalFolderResolverStrategy.class ) ) {
         final Path storagePath = Path.of( TEST_NAMESPACE_PATH.toString(), "OldAspectModel.ttl" );

         utilities.when( () -> LocalFolderResolverStrategy.transformToValidModelDirectory( any() ) )
                  .thenReturn( storagePath.toString() );

         final ProcessPath processPath = Mockito.mock( ProcessPath.class );
         when( processPath.getPath() ).thenReturn( storagePath );

         final String migratedModel = modelService.migrateModel( Files.readString( storagePath, StandardCharsets.UTF_8 ) );

         checkMigratedModel( migratedModel );
      }
   }

   @Test
   void testMigrateWorkspaceWithoutVersionUpgrade() throws IOException {
      try ( final MockedStatic<LocalFolderResolverStrategy> strategyUtilities = Mockito.mockStatic(
            LocalFolderResolverStrategy.class );
            final MockedStatic<FileUtils> fileUtilities = Mockito.mockStatic( FileUtils.class ) ) {
         final Path storagePath = MIGRATION_WORKSPACE_PATH.toAbsolutePath();
         final File OneToMigrateOne = new File( TO_MIGRATE_WORKSPACE_ONE + File.separator + "ToMigrateOne.ttl" );
         final File OneToMigrateTwo = new File( TO_MIGRATE_WORKSPACE_ONE + File.separator + "ToMigrateTwo.ttl" );
         final File TwoToMigrateOne = new File( TO_MIGRATE_WORKSPACE_TWO + File.separator + "ToMigrateOne.ttl" );
         final File TwoToMigrateTwo = new File( TO_MIGRATE_WORKSPACE_TWO + File.separator + "ToMigrateTwo.ttl" );

         fileUtilities.when( () -> FileUtils.listFiles( any( File.class ), any(), anyBoolean() ) )
                      .thenReturn( List.of( OneToMigrateOne, OneToMigrateTwo, TwoToMigrateOne, TwoToMigrateTwo ) );

         strategyUtilities.when( () -> LocalFolderResolverStrategy.transformToValidModelDirectory( any() ) )
                          .thenReturn( storagePath.toString() );

         final Namespaces namespaces = modelService.migrateWorkspace();

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
      assertTrue( migratedModel.contains( "@prefix samm: <urn:samm:org.eclipse.esmf.samm:meta-model:2.0.0#>" ) );
      assertTrue( migratedModel.contains( "@prefix samm-c: <urn:samm:org.eclipse.esmf.samm:characteristic:2.0.0#>" ) );
      assertTrue( migratedModel.contains( "@prefix samm-e: <urn:samm:org.eclipse.esmf.samm:entity:2.0.0#>" ) );
      assertTrue( migratedModel.contains( "@prefix unit: <urn:samm:org.eclipse.esmf.samm:unit:2.0.0#>" ) );
   }
}
