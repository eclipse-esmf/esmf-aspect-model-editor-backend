/*
 * Copyright (c) 2025 Robert Bosch Manufacturing Solutions GmbH
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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.esmf.ame.exceptions.FileNotFoundException;
import org.eclipse.esmf.ame.model.MockFileUpload;
import org.eclipse.esmf.ame.services.models.AspectModelResult;
import org.eclipse.esmf.ame.services.models.MigrationResult;
import org.eclipse.esmf.ame.validation.model.ViolationReport;
import org.eclipse.esmf.aspectmodel.loader.AspectModelLoader;
import org.eclipse.esmf.aspectmodel.resolver.FileSystemStrategy;
import org.eclipse.esmf.aspectmodel.resolver.exceptions.ModelResolutionException;
import org.eclipse.esmf.aspectmodel.urn.AspectModelUrn;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.MediaType;
import io.micronaut.http.multipart.CompletedFileUpload;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;

@MicronautTest
class ModelServiceTest {
   @Inject
   private ModelService modelService;

   private static final String FILE_EXTENSION = ".ttl";

   private static final Path RESOURCE_PATH = Path.of( "src", "test", "resources", "services" );

   private static final String EXAMPLE_NAMESPACE = "org.eclipse.esmf.example";
   private static final String VERSION = "1.0.0";

   private static final AspectModelUrn NAMESPACE = AspectModelUrn.fromUrn( "urn:samm:" + EXAMPLE_NAMESPACE + ":" + VERSION );

   private static final Path TEST_NAMESPACE_PATH = Path.of( RESOURCE_PATH.toString(), EXAMPLE_NAMESPACE, VERSION );

   private static final String TEST_MODEL_FOR_SERVICE = "Movement";
   private static final String TEST_MODEL_NOT_FOUND = "NOTFOUND";
   private static final String TEST_MODEL_TO_DELETE = "FileToDelete";
   private static final String TEST_FILEPATH = Path.of( EXAMPLE_NAMESPACE, VERSION, TEST_MODEL_FOR_SERVICE + FILE_EXTENSION ).toString();

   private static final Path TTL_PATH = Path.of( "src", "test", "resources", "services", "org.eclipse.esmf.example", "1.0.0",
         "FileToDelete.ttl" );

   @Test
   void testDeleteModel() {
      modelService.deleteModel( NAMESPACE.withName( TEST_MODEL_TO_DELETE ) );

      final Path deletedFilePath = Path.of( TEST_NAMESPACE_PATH.toString(), "#", TEST_MODEL_TO_DELETE );
      assertFalse( Files.exists( deletedFilePath ), "The file should not exist after deleteModel() is called." );

      assertThrows( FileNotFoundException.class,
            () -> modelService.getModel( NAMESPACE.withName( TEST_MODEL_TO_DELETE ), TTL_PATH.toString() ),
            "Expected FileNotFoundException when accessing a deleted model." );
   }

   @Test
   void testGetModel() throws ModelResolutionException {
      final AspectModelResult result = modelService.getModel( NAMESPACE.withName( TEST_MODEL_FOR_SERVICE ), TEST_FILEPATH );
      assertTrue( result.content().contains( "@prefix samm: <urn:samm:org.eclipse.esmf.samm:meta-model:2.2.0#> ." ) );
      assertTrue( result.content().contains( "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> ." ) );
      assertTrue( result.content().contains( "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> ." ) );
      assertTrue( result.content().contains( "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> ." ) );
      assertTrue( result.content().contains( "@prefix : <urn:samm:org.eclipse.esmf.example:1.0.0#> ." ) );
      assertTrue( result.content().contains( ":Movement a samm:Aspect ;" ) );
      assertTrue( result.filename().get().contains( "Movement.ttl" ) );
   }

   @Test()
   void testGetModelThrowsNotFoundException() {
      assertThrows( FileNotFoundException.class, () ->
            modelService.getModel( NAMESPACE.withName( TEST_MODEL_NOT_FOUND ), TEST_FILEPATH ) );
   }

   @Test
   void testValidateModel() throws IOException {
      final Path storagePath = Path.of( TEST_NAMESPACE_PATH.toString(), TEST_MODEL_FOR_SERVICE + FILE_EXTENSION );
      final byte[] testModelForService = Files.readAllBytes( storagePath );
      final CompletedFileUpload mockedZipFile = new MockFileUpload( "TestArchive.ttl", testModelForService,
            MediaType.of( MediaType.MULTIPART_FORM_DATA ) );

      final ViolationReport validateReport = modelService.validateModel( URI.create( "blob:///" + toUriPath( storagePath ) ),
            mockedZipFile );

      assertTrue( validateReport.getViolationErrors().isEmpty() );
   }

   @Test
   void testSaveModel() {
      assertDoesNotThrow( () -> {
         final Path fileToReplace = Path.of( TEST_NAMESPACE_PATH.toString(), TEST_MODEL_FOR_SERVICE + FILE_EXTENSION );
         final String turtleData = Files.readString( fileToReplace, StandardCharsets.UTF_8 );

         modelService.createOrSaveModel( turtleData, NAMESPACE.withName( TEST_MODEL_FOR_SERVICE ),
               TEST_MODEL_FOR_SERVICE + FILE_EXTENSION, RESOURCE_PATH.toAbsolutePath() );
      } );
   }

   @Test
   void testMigrateModel() throws IOException {
      final Path storagePath = Path.of( TEST_NAMESPACE_PATH.toString(), "OldAspectModel.ttl" );
      final byte[] testModelForService = Files.readAllBytes( storagePath );
      final CompletedFileUpload mockedZipFile = new MockFileUpload( "TestArchive.ttl", testModelForService,
            MediaType.of( MediaType.MULTIPART_FORM_DATA ) );

      final String migratedModel = modelService.migrateModel( URI.create( "blob:///" + toUriPath( storagePath ) ), mockedZipFile );

      checkMigratedModel( migratedModel );
   }

   public void checkMigratedModel( final String migratedModel ) {
      assertTrue( migratedModel.contains( "@prefix samm: <urn:samm:org.eclipse.esmf.samm:meta-model:2.2.0#>" ) );
   }

   public String toUriPath( final Path path ) {
      String uriPath = path.toString();
      if ( System.getProperty( "os.name" ).toLowerCase().contains( "win" ) ) {
         uriPath = uriPath.replace( "\\", "/" );
      }
      return uriPath;
   }
}

@MicronautTest
@Property( name = "test.config", value = "special" )
class ModelServiceSpecialTest {
   @Inject
   private ModelService modelService;

   @Factory
   @Requires( property = "test.config", value = "special" )
   static class TestConfigOverride {
      @Bean
      @Singleton
      @Replaces( bean = AspectModelLoader.class )
      public AspectModelLoader aspectModelLoader() {
         return new AspectModelLoader( new FileSystemStrategy( modelPath() ) );
      }

      @Bean
      @Singleton
      @Replaces( bean = Path.class )
      public Path modelPath() {
         return Path.of( "src", "test", "resources", "services", "workspace-to-migrate" ).toAbsolutePath();
      }
   }

   private static final String VERSION = "1.0.0";
   private static final String NEW_VERSION = "2.0.0";

   private static final Path RESOURCE_PATH = Path.of( "src", "test", "resources", "services" );

   private static final Path MIGRATION_WORKSPACE_PATH = Path.of( RESOURCE_PATH.toString(), "workspace-to-migrate" );
   private static final Path MIGRATE_WORKSPACE_ONE = Path.of( MIGRATION_WORKSPACE_PATH.toString(), "io.migrate-workspace-one", VERSION );
   private static final Path MIGRATE_WORKSPACE_TWO = Path.of( MIGRATION_WORKSPACE_PATH.toString(), "io.migrate-workspace-two", VERSION );

   private static final Path MIGRATE_WORKSPACE_ONE_NEW_VERSION = Path.of( MIGRATION_WORKSPACE_PATH.toString(), "io.migrate-workspace-one",
         NEW_VERSION );
   private static final Path MIGRATE_WORKSPACE_TWO_NEW_VERSION = Path.of( MIGRATION_WORKSPACE_PATH.toString(), "io.migrate-workspace-two",
         NEW_VERSION );

   @Test
   void testMigrateWorkspaceWithoutVersionUpgrade() throws IOException {
      final MigrationResult migrationResult = modelService.migrateWorkspace( false, MIGRATION_WORKSPACE_PATH );

      assertTrue( migrationResult.success() );

      final String migratedModelOne = Files.readString( new File( MIGRATE_WORKSPACE_ONE + File.separator + "ToMigrateOne.ttl" ).toPath()
                  .toAbsolutePath(),
            StandardCharsets.UTF_8 );
      final String migratedModelTwo = Files.readString( new File( MIGRATE_WORKSPACE_ONE + File.separator + "ToMigrateTwo.ttl" ).toPath(),
            StandardCharsets.UTF_8 );
      final String migratedModelThree = Files.readString(
            new File( MIGRATE_WORKSPACE_TWO + File.separator + "ToMigrateOne.ttl" ).toPath(), StandardCharsets.UTF_8 );
      final String migratedModelFour = Files.readString(
            new File( MIGRATE_WORKSPACE_TWO + File.separator + "ToMigrateTwo.ttl" ).toPath(), StandardCharsets.UTF_8 );

      checkMigratedModel( migratedModelOne );
      checkMigratedModel( migratedModelTwo );
      checkMigratedModel( migratedModelThree );
      checkMigratedModel( migratedModelFour );
   }

   @Test
   void testMigrateWorkspaceWithVersionUpgrade() throws IOException {
      final MigrationResult migrationResult = modelService.migrateWorkspace( true, MIGRATION_WORKSPACE_PATH );

      assertTrue( migrationResult.success() );

      final String migratedModelOne = Files.readString(
            new File( MIGRATE_WORKSPACE_ONE_NEW_VERSION + File.separator + "ToMigrateOne.ttl" ).toPath()
                  .toAbsolutePath(),
            StandardCharsets.UTF_8 );
      final String migratedModelTwo = Files.readString(
            new File( MIGRATE_WORKSPACE_ONE_NEW_VERSION + File.separator + "ToMigrateTwo.ttl" ).toPath(),
            StandardCharsets.UTF_8 );
      final String migratedModelThree = Files.readString(
            new File( MIGRATE_WORKSPACE_TWO_NEW_VERSION + File.separator + "ToMigrateOne.ttl" ).toPath(), StandardCharsets.UTF_8 );
      final String migratedModelFour = Files.readString(
            new File( MIGRATE_WORKSPACE_TWO_NEW_VERSION + File.separator + "ToMigrateTwo.ttl" ).toPath(), StandardCharsets.UTF_8 );

      checkMigratedModel( migratedModelOne );
      assertTrue( migratedModelOne.contains( "@prefix : <urn:samm:io.migrate-workspace-one:2.0.0#>" ) );
      checkMigratedModel( migratedModelTwo );
      assertTrue( migratedModelTwo.contains( "@prefix : <urn:samm:io.migrate-workspace-one:2.0.0#>" ) );
      checkMigratedModel( migratedModelThree );
      assertTrue( migratedModelThree.contains( "@prefix : <urn:samm:io.migrate-workspace-two:2.0.0#>" ) );
      checkMigratedModel( migratedModelFour );
      assertTrue( migratedModelFour.contains( "@prefix : <urn:samm:io.migrate-workspace-two:2.0.0#>" ) );
   }

   public void checkMigratedModel( final String migratedModel ) {
      assertTrue( migratedModel.contains( "@prefix samm: <urn:samm:org.eclipse.esmf.samm:meta-model:2.2.0#>" ) );
   }
}

