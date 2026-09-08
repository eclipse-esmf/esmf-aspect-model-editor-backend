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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.esmf.ame.exceptions.FileNotFoundException;
import org.eclipse.esmf.aspectmodel.urn.AspectModelUrn;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@MicronautTest
class AspectModelWriterTest {
   @Inject
   private AspectModelWriter aspectModelWriter;

   @Inject
   private AspectModelReader aspectModelReader;

   private static final String FILE_EXTENSION = ".ttl";

   private static final Path RESOURCE_PATH = Path.of( "src", "test", "resources", "services" );

   private static final String EXAMPLE_NAMESPACE = "org.eclipse.esmf.example";
   private static final String VERSION = "1.0.0";

   private static final AspectModelUrn NAMESPACE = AspectModelUrn.fromUrn( "urn:samm:" + EXAMPLE_NAMESPACE + ":" + VERSION );

   private static final Path TEST_NAMESPACE_PATH = Path.of( RESOURCE_PATH.toString(), EXAMPLE_NAMESPACE, VERSION );

   private static final String TEST_MODEL_FOR_SERVICE = "Movement";
   private static final String TEST_MODEL_TO_DELETE = "FileToDelete";

   private String originalFileToDeleteContent;
   private String originalMovementContent;

   @BeforeEach
   void setUp() throws IOException {
      final Path fileToDelete = Path.of( TEST_NAMESPACE_PATH.toString(), TEST_MODEL_TO_DELETE + FILE_EXTENSION );
      if ( Files.exists( fileToDelete ) ) {
         originalFileToDeleteContent = Files.readString( fileToDelete, StandardCharsets.UTF_8 );
      }
      final Path movementFile = Path.of( TEST_NAMESPACE_PATH.toString(), TEST_MODEL_FOR_SERVICE + FILE_EXTENSION );
      if ( Files.exists( movementFile ) ) {
         originalMovementContent = Files.readString( movementFile, StandardCharsets.UTF_8 );
      }
   }

   @AfterEach
   void tearDown() throws IOException {
      if ( originalFileToDeleteContent != null ) {
         final Path fileToDelete = Path.of( TEST_NAMESPACE_PATH.toString(), TEST_MODEL_TO_DELETE + FILE_EXTENSION );
         Files.writeString( fileToDelete, originalFileToDeleteContent, StandardCharsets.UTF_8 );
      }
      if ( originalMovementContent != null ) {
         final Path movementFile = Path.of( TEST_NAMESPACE_PATH.toString(), TEST_MODEL_FOR_SERVICE + FILE_EXTENSION );
         Files.writeString( movementFile, originalMovementContent, StandardCharsets.UTF_8 );
      }
      final Path newFilePath = Path.of( TEST_NAMESPACE_PATH.toString(), "TestNewModel" + FILE_EXTENSION );
      Files.deleteIfExists( newFilePath );
   }

   @Test
   void testDeleteModel() {
      aspectModelWriter.deleteModel( NAMESPACE.withName( TEST_MODEL_TO_DELETE ) );

      final Path deletedFilePath = Path.of( TEST_NAMESPACE_PATH.toString(), TEST_MODEL_TO_DELETE + FILE_EXTENSION );
      assertFalse( Files.exists( deletedFilePath ), "The file should not exist after deleteModel() is called." );

      assertThrows( FileNotFoundException.class,
            () -> aspectModelReader.getModel( NAMESPACE.withName( TEST_MODEL_TO_DELETE ), TEST_MODEL_TO_DELETE + FILE_EXTENSION ),
            "Relative path must contain at least namespace, version, and filename: FileToDelete.ttl" );
   }

   @Test
   void testSaveModel() {
      assertDoesNotThrow( () -> {
         final Path fileToReplace = Path.of( TEST_NAMESPACE_PATH.toString(), TEST_MODEL_FOR_SERVICE + FILE_EXTENSION );
         final String turtleData = Files.readString( fileToReplace, StandardCharsets.UTF_8 );

         aspectModelWriter.saveModel( turtleData, NAMESPACE.withName( TEST_MODEL_FOR_SERVICE ), TEST_MODEL_FOR_SERVICE + FILE_EXTENSION,
               RESOURCE_PATH.toAbsolutePath() );
      } );
   }

   @Test
   void testCreateModel() throws IOException {
      final String testModelName = "TestNewModel";
      final Path newFilePath = Path.of( TEST_NAMESPACE_PATH.toString(), testModelName + FILE_EXTENSION );

      // Cleanup if exists
      Files.deleteIfExists( newFilePath );

      final String turtleContent = """
            @prefix samm: <urn:samm:org.eclipse.esmf.samm:meta-model:2.2.0#> .
            @prefix : <urn:samm:org.eclipse.esmf.example:1.0.0#> .
            
            :TestNewModel a samm:Aspect ;
               samm:properties ( ) ;
               samm:operations ( ) .
            """;

      assertDoesNotThrow( () -> {
         aspectModelWriter.createModel( turtleContent, NAMESPACE.withName( testModelName ), testModelName + FILE_EXTENSION,
               RESOURCE_PATH.toAbsolutePath() );
      } );

      // Verify file was created
      try {
         assertFalse( Files.notExists( newFilePath ), "The model file should be created" );
      } finally {
         // Cleanup
         Files.deleteIfExists( newFilePath );
      }
   }
}

