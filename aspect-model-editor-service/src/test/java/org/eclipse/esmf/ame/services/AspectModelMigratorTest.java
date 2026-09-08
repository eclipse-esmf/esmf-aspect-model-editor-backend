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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.esmf.ame.model.MockFileUpload;
import org.eclipse.esmf.ame.services.models.MigrationResult;
import org.eclipse.esmf.aspectmodel.loader.AspectModelLoader;
import org.eclipse.esmf.aspectmodel.resolver.FileSystemStrategy;

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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@MicronautTest
class AspectModelMigratorTest {
   @Inject
   private AspectModelMigrator aspectModelMigrator;

   private static final Path RESOURCE_PATH = Path.of( "src", "test", "resources", "services" );

   private static final String EXAMPLE_NAMESPACE = "org.eclipse.esmf.example";
   private static final String VERSION = "1.0.0";

   private static final Path TEST_NAMESPACE_PATH = Path.of( RESOURCE_PATH.toString(), EXAMPLE_NAMESPACE, VERSION );

   @Test
   void testMigrateModel() throws IOException {
      final Path storagePath = Path.of( TEST_NAMESPACE_PATH.toString(), "OldAspectModel.ttl" );
      final byte[] testModelForService = Files.readAllBytes( storagePath );
      final CompletedFileUpload mockedZipFile = MockFileUpload.create( "TestArchive.ttl", testModelForService,
            MediaType.of( MediaType.MULTIPART_FORM_DATA ) );

      final String migratedModel = aspectModelMigrator.migrate( URI.create( "blob:///" + toUriPath( storagePath ) ), mockedZipFile );

      assertTrue( migratedModel.contains( "@prefix samm: <urn:samm:org.eclipse.esmf.samm:meta-model:2.2.0#>" ) );
   }

   private String toUriPath( final Path path ) {
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

   @Inject
   private AspectModelMigrator aspectModelMigrator;

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

   private Map<Path, byte[]> originalFiles;
   private Set<Path> originalDirs;

   @BeforeEach
   void setUp() throws IOException {
      originalFiles = new HashMap<>();
      originalDirs = new HashSet<>();
      try ( final Stream<Path> stream = Files.walk( MIGRATION_WORKSPACE_PATH ) ) {
         stream.forEach( path -> {
            try {
               if ( Files.isRegularFile( path ) ) {
                  originalFiles.put( path, Files.readAllBytes( path ) );
               } else if ( Files.isDirectory( path ) ) {
                  originalDirs.add( path );
               }
            } catch ( final IOException e ) {
               throw new RuntimeException( e );
            }
         } );
      }
   }

   @AfterEach
   void tearDown() throws IOException {
      if ( originalFiles == null ) {
         return;
      }
      try ( final Stream<Path> stream = Files.walk( MIGRATION_WORKSPACE_PATH ) ) {
         final List<Path> currentPaths = stream.sorted( Comparator.reverseOrder() ).toList();
         for ( final Path path : currentPaths ) {
            if ( Files.isRegularFile( path ) && !originalFiles.containsKey( path ) ) {
               Files.deleteIfExists( path );
            } else if ( Files.isDirectory( path ) && !originalDirs.contains( path ) ) {
               Files.deleteIfExists( path );
            }
         }
      }
      for ( final Map.Entry<Path, byte[]> entry : originalFiles.entrySet() ) {
         Files.write( entry.getKey(), entry.getValue() );
      }
   }

   @Test
   void testMigrateWorkspaceWithoutVersionUpgrade() throws IOException {
      final MigrationResult migrationResult = aspectModelMigrator.migrateWorkspace( modelService.getAllNamespaces(), false,
            MIGRATION_WORKSPACE_PATH );

      assertTrue( migrationResult.success() );

      final String migratedModelOne = Files.readString(
            new File( MIGRATE_WORKSPACE_ONE + File.separator + "ToMigrateOne.ttl" ).toPath().toAbsolutePath(), StandardCharsets.UTF_8 );
      final String migratedModelTwo = Files.readString( new File( MIGRATE_WORKSPACE_ONE + File.separator + "ToMigrateTwo.ttl" ).toPath(),
            StandardCharsets.UTF_8 );
      final String migratedModelThree = Files.readString( new File( MIGRATE_WORKSPACE_TWO + File.separator + "ToMigrateOne.ttl" ).toPath(),
            StandardCharsets.UTF_8 );
      final String migratedModelFour = Files.readString( new File( MIGRATE_WORKSPACE_TWO + File.separator + "ToMigrateTwo.ttl" ).toPath(),
            StandardCharsets.UTF_8 );

      checkMigratedModel( migratedModelOne );
      checkMigratedModel( migratedModelTwo );
      checkMigratedModel( migratedModelThree );
      checkMigratedModel( migratedModelFour );
   }

   @Test
   void testMigrateWorkspaceWithVersionUpgrade() throws IOException {
      final MigrationResult migrationResult = aspectModelMigrator.migrateWorkspace( modelService.getAllNamespaces(), true,
            MIGRATION_WORKSPACE_PATH );

      assertTrue( migrationResult.success() );

      final String migratedModelOne = Files.readString(
            new File( MIGRATE_WORKSPACE_ONE_NEW_VERSION + File.separator + "ToMigrateOne.ttl" ).toPath().toAbsolutePath(),
            StandardCharsets.UTF_8 );
      final String migratedModelTwo = Files.readString(
            new File( MIGRATE_WORKSPACE_ONE_NEW_VERSION + File.separator + "ToMigrateTwo.ttl" ).toPath(), StandardCharsets.UTF_8 );
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


