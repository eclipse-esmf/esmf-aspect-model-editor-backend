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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.esmf.ame.exceptions.FileNotFoundException;
import org.eclipse.esmf.ame.validation.model.ViolationReport;
import org.eclipse.esmf.aspectmodel.resolver.exceptions.ModelResolutionException;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@MicronautTest
class ModelServiceTest {
   @Inject
   private ModelService modelService;

   private static final String FILE_EXTENSION = ".ttl";

   private static final Path RESOURCE_PATH = Path.of( "src", "test", "resources", "services" );

   private static final String SAMM_URN = "urn:samm";
   private static final String EXAMPLE_NAMESPACE = "org.eclipse.esmf.example";
   private static final String VERSION = "1.0.0";

   private static final String NAMESPACE_VERSION = SAMM_URN + ":" + EXAMPLE_NAMESPACE + ":" + VERSION;

   private static final Path TEST_NAMESPACE_PATH = Path.of( RESOURCE_PATH.toString(), EXAMPLE_NAMESPACE, VERSION );

   private static final Path MIGRATION_WORKSPACE_PATH = Path.of( RESOURCE_PATH.toString(), "workspace-to-migrate" );
   private static final Path TO_MIGRATE_WORKSPACE_ONE = Path.of( MIGRATION_WORKSPACE_PATH.toString(),
         "io.migrate-workspace-one", VERSION );
   private static final Path TO_MIGRATE_WORKSPACE_TWO = Path.of( MIGRATION_WORKSPACE_PATH.toString(),
         "io.migrate-workspace-two", VERSION );
   private static final Path MIGRATE_WORKSPACE_ONE = Path.of( RESOURCE_PATH.toString(), "io.migrate-workspace-one",
         VERSION );
   private static final Path MIGRATE_WORKSPACE_TWO = Path.of( RESOURCE_PATH.toString(), "io.migrate-workspace-two",
         VERSION );

   private static final String TEST_MODEL_FOR_SERVICE = "ModelService";
   private static final String TEST_MODEL_NOT_FOUND = "NOTFOUND";
   private static final String TEST_MODEL_TO_DELETE = "FileToDelete";
   private static final String TEST_FILEPATH = Path.of( EXAMPLE_NAMESPACE, VERSION, TEST_MODEL_FOR_SERVICE + FILE_EXTENSION ).toString();

   private static final String URN =
         "urn:samm:org.eclipse.esmf.example:1.0.0#FileToDelete";
   private static final Path TTL_PATH = Path.of(
         "src", "test", "resources", "services",
         "org.eclipse.esmf.example", "1.0.0", "FileToDelete.ttl" );

   @Test
   void testDeleteModel() {
      modelService.deleteModel( NAMESPACE_VERSION + "#" + TEST_MODEL_TO_DELETE );

      final Path deletedFilePath = Path.of( TEST_NAMESPACE_PATH.toString(), "#", TEST_MODEL_TO_DELETE );
      assertFalse( Files.exists( deletedFilePath ), "The file should not exist after deleteModel() is called." );

      assertThrows( FileNotFoundException.class, () -> modelService.getModel( URN, TTL_PATH.toString() ),
            "Expected FileNotFoundException when accessing a deleted model." );
   }

   @Test
   void testGetModel() throws ModelResolutionException {
      final String result = modelService.getModel( NAMESPACE_VERSION + "#" + TEST_MODEL_FOR_SERVICE, TEST_FILEPATH );
      assertTrue( result.contains( "@prefix samm: <urn:samm:org.eclipse.esmf.samm:meta-model:2.1.0#> ." ) );
      assertTrue( result.contains( "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> ." ) );
      assertTrue( result.contains( "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> ." ) );
      assertTrue( result.contains( "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> ." ) );
      assertTrue( result.contains( "@prefix : <urn:samm:org.eclipse.esmf.example:1.0.0#> ." ) );
      assertTrue( result.contains( ":ModelService a samm:Aspect ;" ) );
      assertTrue( result.contains( "samm:properties ( :property ) ;" ) );
      assertTrue( result.contains( "samm:operations ( ) ." ) );
      assertTrue( result.contains( ":property a samm:Property ;" ) );
      assertTrue( result.contains( "samm:characteristic :Characteristic ;" ) );
      assertTrue( result.contains( "samm:properties ( :property )" ) );
      assertTrue( result.contains( "samm:dataType xsd:string ." ) );
      assertTrue( result.contains( ":Characteristic a samm:Characteristic ;" ) );
      assertTrue( result.contains( "samm:dataType xsd:string ." ) );
   }

   @Test()
   void testGetModelThrowsNotFoundException() {
      assertThrows( FileNotFoundException.class,
            () -> modelService.getModel( NAMESPACE_VERSION + "#" + TEST_MODEL_NOT_FOUND, TEST_FILEPATH ) );
   }

   @Test
   void testValidateModel() throws IOException {
      final Path storagePath = Path.of( TEST_NAMESPACE_PATH.toString(), TEST_MODEL_FOR_SERVICE + FILE_EXTENSION );
      final String testModelForService = Files.readString( storagePath, StandardCharsets.UTF_8 );
      final ViolationReport validateReport = modelService.validateModel( testModelForService );

      assertTrue( validateReport.getViolationErrors().isEmpty() );
   }

   @Test
   void testSaveModel() throws IOException {
      assertDoesNotThrow( () -> {
         final Path fileToReplace = Path.of( TEST_NAMESPACE_PATH.toString(), TEST_MODEL_FOR_SERVICE + FILE_EXTENSION );
         final String turtleData = Files.readString( fileToReplace, StandardCharsets.UTF_8 );

         modelService.createOrSaveModel( turtleData, NAMESPACE_VERSION + "#" + TEST_MODEL_FOR_SERVICE,
               TEST_MODEL_FOR_SERVICE + FILE_EXTENSION,
               RESOURCE_PATH.toAbsolutePath() );
      } );
   }

   @Test
   void testMigrateModel() throws IOException {
      final Path storagePath = Path.of( TEST_NAMESPACE_PATH.toString(), "OldAspectModel.ttl" );
      final String testModel = Files.readString( storagePath, StandardCharsets.UTF_8 );
      final String migratedModel = modelService.migrateModel( testModel );

      checkMigratedModel( migratedModel );
   }

   //      @Test
   //      void testMigrateWorkspaceWithoutVersionUpgrade() throws IOException {
   //         try ( final MockedStatic<LocalFolderResolverStrategy> strategyUtilities = Mockito.mockStatic(
   //               LocalFolderResolverStrategy.class );
   //               final MockedStatic<FileUtils> fileUtilities = Mockito.mockStatic( FileUtils.class ) ) {
   //            final Path storagePath = MIGRATION_WORKSPACE_PATH.toAbsolutePath();
   //            final File OneToMigrateOne = new File( TO_MIGRATE_WORKSPACE_ONE + File.separator + "ToMigrateOne.ttl" );
   //            final File OneToMigrateTwo = new File( TO_MIGRATE_WORKSPACE_ONE + File.separator + "ToMigrateTwo.ttl" );
   //            final File TwoToMigrateOne = new File( TO_MIGRATE_WORKSPACE_TWO + File.separator + "ToMigrateOne.ttl" );
   //            final File TwoToMigrateTwo = new File( TO_MIGRATE_WORKSPACE_TWO + File.separator + "ToMigrateTwo.ttl" );
   //
   //            fileUtilities.when( () -> FileUtils.listFiles( any( File.class ), any(), anyBoolean() ) )
   //                         .thenReturn( List.of( OneToMigrateOne, OneToMigrateTwo, TwoToMigrateOne, TwoToMigrateTwo ) );
   //
   //            strategyUtilities.when( () -> LocalFolderResolverStrategy.transformToValidModelDirectory( any() ) )
   //                             .thenReturn( storagePath.toString() );
   //
   //            final NamespaceFileCollection collection = modelService.migrateWorkspace();
   //
   //            assertEquals( 2, collection.versionedNamespaceFiles.size() );
   //            assertEquals( "io.migrate-workspace-one:1.0.0",
   //                  collection.versionedNamespaceFiles.get( 0 ).versionedNamespace );
   //            assertEquals( 2, collection.versionedNamespaceFiles.get( 0 ).files.size() );
   //            assertEquals( "ToMigrateOne.ttl", collection.versionedNamespaceFiles.get( 0 ).files.get( 0 ).getName() );
   //            assertEquals( true, collection.versionedNamespaceFiles.get( 0 ).files.get( 0 ).getSuccess() );
   //            assertEquals( "ToMigrateTwo.ttl", collection.versionedNamespaceFiles.get( 0 ).files.get( 1 ).getName() );
   //            assertEquals( true, collection.versionedNamespaceFiles.get( 0 ).files.get( 1 ).getSuccess() );
   //
   //            assertEquals( "io.migrate-workspace-two:1.0.0",
   //                  collection.versionedNamespaceFiles.get( 1 ).versionedNamespace );
   //            assertEquals( 2, collection.versionedNamespaceFiles.get( 1 ).files.size() );
   //            assertEquals( "ToMigrateOne.ttl", collection.versionedNamespaceFiles.get( 1 ).files.get( 0 ).getName() );
   //            assertEquals( true, collection.versionedNamespaceFiles.get( 1 ).files.get( 0 ).getSuccess() );
   //            assertEquals( "ToMigrateTwo.ttl", collection.versionedNamespaceFiles.get( 1 ).files.get( 1 ).getName() );
   //            assertEquals( true, collection.versionedNamespaceFiles.get( 1 ).files.get( 1 ).getSuccess() );
   //
   //            final String migratedModelOne = Files.readString(
   //                  new File( MIGRATE_WORKSPACE_ONE + File.separator + "ToMigrateOne.ttl" ).toPath(),
   //                  StandardCharsets.UTF_8 );
   //            final String migratedModelTwo = Files.readString(
   //                  new File( MIGRATE_WORKSPACE_ONE + File.separator + "ToMigrateTwo.ttl" ).toPath(),
   //                  StandardCharsets.UTF_8 );
   //            final String migratedModelThree = Files.readString(
   //                  new File( MIGRATE_WORKSPACE_TWO + File.separator + "ToMigrateOne.ttl" ).toPath(),
   //                  StandardCharsets.UTF_8 );
   //            final String migratedModelFour = Files.readString(
   //                  new File( MIGRATE_WORKSPACE_TWO + File.separator + "ToMigrateTwo.ttl" ).toPath(),
   //                  StandardCharsets.UTF_8 );
   //
   //            checkMigratedModel( migratedModelOne );
   //            checkMigratedModel( migratedModelTwo );
   //            checkMigratedModel( migratedModelThree );
   //            checkMigratedModel( migratedModelFour );
   //         }
   //      }

   public void checkMigratedModel( final String migratedModel ) {
      assertTrue( migratedModel.contains( "@prefix samm: <urn:samm:org.eclipse.esmf.samm:meta-model:2.1.0#>" ) );
   }
}
