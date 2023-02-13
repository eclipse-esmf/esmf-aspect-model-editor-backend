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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.junit.Test;
import org.junit.jupiter.api.Order;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;

import io.openmanufacturing.ame.config.ApplicationSettings;
import io.openmanufacturing.ame.model.packaging.ProcessPackage;

@RunWith( SpringRunner.class )
@SpringBootTest
public class PackageServiceTest {

   @Autowired
   private PackageService packageService;

   private static final Path resourcesPath = Path.of( "src", "test", "resources" );
   private static final Path workspaceToBackupPath = Path.of( resourcesPath.toString(), "workspace-to-backup" );
   private static final String nameSpaceOne = "io.openmanufacturing.test:1.0.0:TestFileOne.ttl";
   private static final String nameSpaceTwo = "io.openmanufacturing.test:1.0.0:TestFileTwo.ttl";
   private static final String nameSpaceThree = "io.openmanufacturing.test:1.0.0:TestFileThree.ttl";

   @Test
   @Order( 1 )
   public void testValidateImportAspectModelPackage() throws IOException {
      final Path storagePath = Paths.get( resourcesPath.toString(), "test-packages", "io.openmanufacturing.1.0.0" );
      final Path zipFilePath = Paths.get( resourcesPath.toString(), "TestArchive.zip" );

      final MockMultipartFile mockedZipFile = new MockMultipartFile( "TestArchive.zip",
            Files.readAllBytes( zipFilePath ) );

      final ProcessPackage importPackage = packageService.validateImportAspectModelPackage( mockedZipFile,
            storagePath.toFile().getAbsolutePath() );

      assertEquals( importPackage.getValidFiles().size(), 2 );
      assertEquals( importPackage.getInvalidFiles().size(), 1 );
   }

   @Test
   @Order( 2 )
   public void testValidateAspectModels() throws IOException {
      try ( final MockedStatic<ApplicationSettings> utilities = Mockito.mockStatic( ApplicationSettings.class ) ) {
         utilities.when( ApplicationSettings::getMetaModelStoragePath )
                  .thenReturn( resourcesPath.toFile().getAbsolutePath() );

         final Path exportedStoragePath = Paths.get( resourcesPath.toString(), "test-packages" );
         final List<String> aspectModelFiles = List.of( nameSpaceOne, nameSpaceTwo );
         final ProcessPackage processedExportedPackage = packageService.validateAspectModelsForExport( aspectModelFiles,
               exportedStoragePath.toFile().getAbsolutePath() );

         assertEquals( 2, processedExportedPackage.getValidFiles().size() );
         assertEquals( 1, processedExportedPackage.getMissingElements().size() );

         final String[] nameSpaceOneArray = nameSpaceOne.split( ":" );
         final String[] nameSpaceThreeArray = nameSpaceThree.split( ":" );

         assertTrue( processedExportedPackage.getMissingElements().get( 0 ).getAnalysedFileName()
                                             .contains( nameSpaceOneArray[2] ) );

         assertTrue( processedExportedPackage.getMissingElements().get( 0 ).getMissingFileName()
                                             .contains( nameSpaceThreeArray[2] ) );
      }
   }

   @Test
   @Order( 3 )
   public void testExportAspectModelPackage() throws IOException {
      try ( final MockedStatic<ApplicationSettings> utilities = Mockito.mockStatic( ApplicationSettings.class ) ) {
         utilities.when( ApplicationSettings::getMetaModelStoragePath )
                  .thenReturn( resourcesPath.toFile().getAbsolutePath() );

         final Path exportedStoragePath = Paths.get( resourcesPath.toString(), "test-packages" );
         final List<String> aspectModelFiles = List.of( nameSpaceOne, nameSpaceTwo, nameSpaceThree );

         packageService.validateAspectModelsForExport( aspectModelFiles,
               exportedStoragePath.toFile().getAbsolutePath() );

         final byte[] bytes = packageService.exportAspectModelPackage( "TestExportArchive.zip",
               exportedStoragePath.toFile().getAbsolutePath() );

         assertTrue( bytes.length > 0 );
      }
   }

   @Test
   @Order( 4 )
   public void testBackupWorkspace() {
      packageService.backupWorkspace( workspaceToBackupPath.toAbsolutePath().toString(),
            resourcesPath.toAbsolutePath().toString() );

      assertTrue( Arrays.stream( Objects.requireNonNull( resourcesPath.toFile().list() ) )
                        .anyMatch( file -> file.contains( "backup" ) ) );
   }
}
