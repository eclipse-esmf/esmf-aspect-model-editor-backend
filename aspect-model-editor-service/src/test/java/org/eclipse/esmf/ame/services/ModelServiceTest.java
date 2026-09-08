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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import org.eclipse.esmf.ame.exceptions.FileNotFoundException;
import org.eclipse.esmf.ame.services.models.FileEntry;
import org.eclipse.esmf.ame.services.models.FileInformation;
import org.eclipse.esmf.aspectmodel.urn.AspectModelUrn;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
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
   private static final String TEST_MODEL_FOR_BATCH = "BatchTestAspect";
   private static final String TEST_MODEL_OLD_ASPECT = "OldAspectModel";
   private static final String TEST_MODEL_NOT_FOUND = "NOTFOUND";
   private static final String TEST_FILEPATH = Path.of( EXAMPLE_NAMESPACE, VERSION, TEST_MODEL_FOR_SERVICE + FILE_EXTENSION ).toString();

   @Test
   void testGetModels_Success() {
      final List<FileEntry> fileEntriesWithTwoFiles = List.of(
            new FileEntry( EXAMPLE_NAMESPACE + ":" + VERSION + ":" + TEST_MODEL_FOR_SERVICE + FILE_EXTENSION,
                  TEST_MODEL_FOR_SERVICE + FILE_EXTENSION, "urn:samm:" + EXAMPLE_NAMESPACE + ":" + VERSION + "#" + TEST_MODEL_FOR_SERVICE,
                  "" ), new FileEntry( EXAMPLE_NAMESPACE + ":" + VERSION + ":" + TEST_MODEL_OLD_ASPECT + FILE_EXTENSION,
                  TEST_MODEL_OLD_ASPECT + FILE_EXTENSION, "urn:samm:" + EXAMPLE_NAMESPACE + ":" + VERSION + "#" + TEST_MODEL_OLD_ASPECT,
                  "" ) );

      final List<FileInformation> resultOne = modelService.getModels( fileEntriesWithTwoFiles );

      assertFalse( resultOne.isEmpty(), "Results should not be empty" );
      assertTrue( resultOne.stream().anyMatch( fi -> fi.fileName().contains( TEST_MODEL_FOR_SERVICE ) ),
            "Results should contain Movement model" );

      final List<FileEntry> fileEntriesWithOneFile = List.of(
            new FileEntry( EXAMPLE_NAMESPACE + ":" + VERSION + ":" + TEST_MODEL_FOR_SERVICE + FILE_EXTENSION,
                  TEST_MODEL_FOR_SERVICE + FILE_EXTENSION, "urn:samm:" + EXAMPLE_NAMESPACE + ":" + VERSION + "#" + TEST_MODEL_FOR_SERVICE,
                  "" ) );

      final List<FileInformation> resultTwo = modelService.getModels( fileEntriesWithOneFile );

      assertFalse( resultTwo.isEmpty(), "Results should not be empty" );
      assertTrue( resultTwo.stream().anyMatch( fi -> fi.fileName().contains( TEST_MODEL_FOR_SERVICE ) ),
            "Results should contain the requested model" );
   }

   @Test
   void testGetModels_EmptyList() {
      final var emptyList = List.<FileEntry> of();
      final var results = modelService.getModels( emptyList );

      assertTrue( results.isEmpty(), "Results should be empty for empty input" );
   }

   @Test
   void testGetModels_FileNotFound() {
      final List<FileEntry> fileEntries = List.of(
            new FileEntry( EXAMPLE_NAMESPACE + ":" + VERSION + ":" + TEST_MODEL_NOT_FOUND + FILE_EXTENSION,
                  TEST_MODEL_NOT_FOUND + FILE_EXTENSION, "urn:samm:" + EXAMPLE_NAMESPACE + ":" + VERSION + "#" + TEST_MODEL_NOT_FOUND,
                  "" ) );

      assertThrows( FileNotFoundException.class, () -> modelService.getModels( fileEntries ),
            "Should throw FileNotFoundException when file does not exist" );
   }

   @Test
   void testGetModels_InvalidNamespace() {
      final List<FileEntry> fileEntries = List.of(
            new FileEntry( null, "InvalidModel.ttl", "invalid.namespace:1.0.0:InvalidModel.ttl", "" ) );

      final IllegalArgumentException exception = assertThrows( IllegalArgumentException.class, () -> modelService.getModels( fileEntries ),
            "Should throw IllegalArgumentException for invalid URN" );

      assertTrue( exception.getMessage().contains( "Invalid aspect model URN" ), "Exception message should indicate invalid URN" );
      assertTrue( exception.getMessage().contains( "invalid.namespace:1.0.0:InvalidModel.ttl" ),
            "Exception message should contain the invalid URN" );
   }

   @Test
   void testGetModels_InvalidUrnFormat() {
      final List<FileEntry> invalidEntries = List.of( new FileEntry( null, "InvalidModel.ttl", "not-a-valid-urn", "" ),
            new FileEntry( null, "EmptyUrn.ttl", "", "" ), new FileEntry( null, "Malformed.ttl", "::::", "" ) );

      for ( final FileEntry entry : invalidEntries ) {
         final IllegalArgumentException exception = assertThrows( IllegalArgumentException.class,
               () -> modelService.getModels( List.of( entry ) ),
               "Should throw IllegalArgumentException for malformed URN: " + entry.aspectModelUrn() );

         assertTrue( exception.getMessage().contains( "Invalid aspect model URN" ), "Exception message should indicate invalid URN" );
      }
   }

   @Test
   void testGetModels_MissingElement() {
      final List<FileEntry> fileEntries = List.of(
            new FileEntry( EXAMPLE_NAMESPACE + ":" + VERSION + ":" + TEST_MODEL_FOR_SERVICE + FILE_EXTENSION,
                  TEST_MODEL_FOR_SERVICE + FILE_EXTENSION, "urn:samm:" + EXAMPLE_NAMESPACE + ":" + VERSION + "#MissingElement",
                  "" ) );

      final FileNotFoundException fileNotFoundException = assertThrows( FileNotFoundException.class,
            () -> modelService.getModels( fileEntries ), "Should throw FileNotFoundException when element is missing" );

      assertTrue( fileNotFoundException.getMessage().contains( "Aspect Model not found" ) );
      assertTrue( fileNotFoundException.getMessage().contains( "MissingElement" ) );
   }

   @Test
   void testGetModels_PreservesOriginalSammVersion() {
      final List<FileEntry> fileEntries = List.of(
            new FileEntry( EXAMPLE_NAMESPACE + ":" + VERSION + ":" + TEST_MODEL_OLD_ASPECT + FILE_EXTENSION,
                  TEST_MODEL_OLD_ASPECT + FILE_EXTENSION, "urn:samm:" + EXAMPLE_NAMESPACE + ":" + VERSION + "#" + TEST_MODEL_OLD_ASPECT,
                  "" ) );

      final List<FileInformation> results = modelService.getModels( fileEntries );

      assertFalse( results.isEmpty() );
      final FileInformation fileInfo = results.getFirst();
      assertEquals( "2.1.0", fileInfo.modelVersion(), "Should preserve original SAMM 2.1.0 version without auto-migrating" );
      assertTrue( fileInfo.aspectModel().contains( "meta-model:2.1.0#" ), "Turtle content should retain original 2.1.0 prefix" );
   }

   @Test
   void testGetModels_MultipleValidFiles() {
      final List<FileEntry> fileEntries = List.of(
            new FileEntry( EXAMPLE_NAMESPACE + ":" + VERSION + ":" + TEST_MODEL_FOR_SERVICE + FILE_EXTENSION,
                  TEST_MODEL_FOR_SERVICE + FILE_EXTENSION, "urn:samm:" + EXAMPLE_NAMESPACE + ":" + VERSION + "#" + TEST_MODEL_FOR_SERVICE,
                  "" ), new FileEntry( EXAMPLE_NAMESPACE + ":" + VERSION + ":" + TEST_MODEL_OLD_ASPECT + FILE_EXTENSION,
                  TEST_MODEL_OLD_ASPECT + FILE_EXTENSION, "urn:samm:" + EXAMPLE_NAMESPACE + ":" + VERSION + "#" + TEST_MODEL_OLD_ASPECT,
                  "" ) );

      final List<FileInformation> results = modelService.getModels( fileEntries );

      assertFalse( results.isEmpty(), "Results should not be empty" );
      assertTrue( results.size() >= 2, "Should load multiple models" );

      results.forEach( fileInfo -> {
         assertFalse( fileInfo.fileName().isEmpty(), "File name should not be empty" );
         assertFalse( fileInfo.aspectModelUrn().isEmpty(), "URN should not be empty" );
         assertFalse( fileInfo.aspectModel().isEmpty(), "Content should not be empty" );
         assertFalse( fileInfo.modelVersion().isEmpty(), "Model version should not be empty" );
      } );
   }

   @Test
   void testGetModels_VerifyFileInformation() {
      final List<FileEntry> fileEntries = List.of(
            new FileEntry( EXAMPLE_NAMESPACE + ":" + VERSION + ":" + TEST_MODEL_FOR_SERVICE + FILE_EXTENSION,
                  TEST_MODEL_FOR_SERVICE + FILE_EXTENSION, "urn:samm:" + EXAMPLE_NAMESPACE + ":" + VERSION + "#" + TEST_MODEL_FOR_SERVICE,
                  "" ) );

      final List<FileInformation> results = modelService.getModels( fileEntries );

      assertFalse( results.isEmpty(), "Results should not be empty" );

      final FileInformation fileInfo = results.getFirst();
      assertTrue( fileInfo.fileName().contains( ".ttl" ), "File name should have .ttl extension" );
      assertTrue( fileInfo.aspectModelUrn().contains( "urn:samm:" ), "URN should start with urn:samm:" );
      assertTrue( fileInfo.aspectModel().contains( "@prefix samm:" ), "Content should contain SAMM prefix" );
      assertTrue( fileInfo.aspectModel().contains( ":Movement" ), "Content should contain Movement aspect" );
      assertFalse( fileInfo.modelVersion().isEmpty(), "Model version should not be empty" );
   }

   @Test
   void testGetModels_ExceptionMessageContainsFileName() {
      final String nonExistentFile = "NonExistent.ttl";
      final List<FileEntry> fileEntries = List.of(
            new FileEntry( EXAMPLE_NAMESPACE + ":" + VERSION + ":" + nonExistentFile, nonExistentFile,
                  "urn:samm:" + EXAMPLE_NAMESPACE + ":" + VERSION + "#NonExistent", "" ) );

      final FileNotFoundException fileNotFoundException = assertThrows( FileNotFoundException.class,
            () -> modelService.getModels( fileEntries ), "Should throw FileNotFoundException" );

      assertTrue( fileNotFoundException.getMessage().contains( nonExistentFile ) || fileNotFoundException.getMessage()
            .contains( EXAMPLE_NAMESPACE ), "Exception message should contain file name or namespace information" );
   }

   @Test
   void testGetModels_DifferentNamespaces() {
      final List<FileEntry> fileEntries = List.of(
            new FileEntry( EXAMPLE_NAMESPACE + ":" + VERSION + ":" + TEST_MODEL_FOR_SERVICE + FILE_EXTENSION,
                  TEST_MODEL_FOR_SERVICE + FILE_EXTENSION, "urn:samm:" + EXAMPLE_NAMESPACE + ":" + VERSION + "#" + TEST_MODEL_FOR_SERVICE,
                  "" ) );

      final List<FileInformation> results = modelService.getModels( fileEntries );

      assertFalse( results.isEmpty(), "Results should not be empty" );

      results.forEach( fileInfo -> {
         assertTrue( fileInfo.absoluteName().contains( EXAMPLE_NAMESPACE ), "Absolute name should contain namespace" );
         assertTrue( fileInfo.absoluteName().contains( VERSION ), "Absolute name should contain version" );
      } );
   }

   @Test
   void testGetModels_ByUrnOnly() {
      final List<FileEntry> fileEntries = List.of(
            new FileEntry( null, null, "urn:samm:" + EXAMPLE_NAMESPACE + ":" + VERSION + "#" + TEST_MODEL_FOR_SERVICE, "" ) );

      final List<FileInformation> results = modelService.getModels( fileEntries );

      assertFalse( results.isEmpty(), "Results should not be empty" );
      final FileInformation fileInfo = results.getFirst();
      assertEquals( TEST_MODEL_FOR_SERVICE + FILE_EXTENSION, fileInfo.fileName() );
      assertTrue( fileInfo.aspectModel().contains( ":Movement" ) );
   }
}
