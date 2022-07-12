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
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;

import io.openmanufacturing.ame.config.ApplicationSettings;
import io.openmanufacturing.ame.services.model.ProcessPackage;

@RunWith( SpringRunner.class )
@SpringBootTest
public class PackageServiceTest {

   @Autowired
   private PackageService packageService;

   private static final Path resourcesPath = Path.of( "src", "test", "resources" );

   private static final String nameSpaceOne = "io.openmanufacturing.test:1.0.0:TestFileOne.ttl";
   private static final String nameSpaceTwo = "io.openmanufacturing.test:1.0.0:TestFileTwo.ttl";
   private static final String nameSpaceThree = "io.openmanufacturing.test:1.0.0:TestFileThree.ttl";

   @Test
   public void testValidateImportAspectModelPackage() throws IOException {
      final Path storagePath = Paths.get( resourcesPath.toString(), "test-packages" );
      final Path zipFilePath = Paths.get( resourcesPath.toString(), "TestArchive.zip" );

      final MockMultipartFile mockedZipFile
            = new MockMultipartFile(
            "TestArchive.zip",
            Files.readAllBytes( zipFilePath )
      );

      final ProcessPackage importPackage = packageService.validateImportAspectModelPackage(
            mockedZipFile, storagePath.toFile().getAbsolutePath() );

      assertEquals( importPackage.getCorrectFiles().size(), 2 );
      assertEquals( importPackage.getIncorrectFiles().size(), 1 );
   }

   @Test
   public void testValidateAspectModels() throws IOException {
      try ( final MockedStatic<ApplicationSettings> utilities = Mockito.mockStatic( ApplicationSettings.class ) ) {
         utilities.when( ApplicationSettings::getMetaModelStoragePath )
                  .thenReturn( resourcesPath.toFile().getAbsolutePath() );

         final Path exportedStoragePath = Paths.get( resourcesPath.toString(), "test-packages" );
         final List<String> aspectModelFiles = List.of( nameSpaceOne, nameSpaceTwo );
         final ProcessPackage processedExportedPackage = packageService.validateAspectModels( aspectModelFiles,
               exportedStoragePath.toFile().getAbsolutePath() );

         assertEquals( 2, processedExportedPackage.getCorrectFiles().size() );
         assertEquals( 1, processedExportedPackage.getMissingFiles().size() );

         final String[] nameSpaceOneArray = nameSpaceOne.split( ":" );
         final String[] nameSpaceThreeArray = nameSpaceThree.split( ":" );

         assertTrue(
               processedExportedPackage.getMissingFiles().get( 0 ).getAnalysedFile().contains( nameSpaceOneArray[0] ) );
         assertTrue(
               processedExportedPackage.getMissingFiles().get( 0 ).getAnalysedFile().contains( nameSpaceOneArray[1] ) );
         assertTrue(
               processedExportedPackage.getMissingFiles().get( 0 ).getAnalysedFile().contains( nameSpaceOneArray[2] ) );

         assertTrue( processedExportedPackage.getMissingFiles().get( 0 ).getMissingFile()
                                             .contains( nameSpaceThreeArray[0] ) );
         assertTrue( processedExportedPackage.getMissingFiles().get( 0 ).getMissingFile()
                                             .contains( nameSpaceThreeArray[1] ) );
         assertTrue( processedExportedPackage.getMissingFiles().get( 0 ).getMissingFile()
                                             .contains( nameSpaceThreeArray[2] ) );

         FileUtils.deleteDirectory( exportedStoragePath.toFile() );
      }
   }

   @Test
   public void testExportAspectModelPackage() throws IOException {
      try ( final MockedStatic<ApplicationSettings> utilities = Mockito.mockStatic( ApplicationSettings.class ) ) {
         utilities.when( ApplicationSettings::getMetaModelStoragePath )
                  .thenReturn( resourcesPath.toFile().getAbsolutePath() );

         final Path exportedStoragePath = Paths.get( resourcesPath.toString(), "test-packages" );
         final List<String> aspectModelFiles = List.of( nameSpaceOne, nameSpaceTwo, nameSpaceThree );

         packageService.validateAspectModels( aspectModelFiles, exportedStoragePath.toFile().getAbsolutePath() );

         final byte[] bytes = packageService.exportAspectModelPackage( "TestExportArchive.zip",
               exportedStoragePath.toFile().getAbsolutePath() );

         assertTrue( bytes.length > 0 );

         FileUtils.deleteDirectory( exportedStoragePath.toFile() );
      }
   }
}
