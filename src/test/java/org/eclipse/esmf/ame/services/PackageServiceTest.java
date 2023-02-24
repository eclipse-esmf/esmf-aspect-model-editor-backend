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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.eclipse.esmf.ame.model.ValidationProcess;
import org.eclipse.esmf.ame.model.packaging.ProcessPackage;
import org.eclipse.esmf.ame.model.resolver.FolderStructure;
import org.eclipse.esmf.ame.repository.strategy.utils.LocalFolderResolverUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith( SpringExtension.class )
@SpringBootTest
class PackageServiceTest {

   @Autowired
   private PackageService packageService;

   @Autowired
   private ModelResolverRepository modelResolverRepository;

   private static final Path resourcesPath = Path.of( "src", "test", "resources" );
   private static final Path workspaceToBackupPath = Path.of( resourcesPath.toString(), "workspace-to-backup" );
   private static final String nameSpaceOne = "io.openmanufacturing.test:1.0.0:TestFileOne.ttl";
   private static final String nameSpaceTwo = "io.openmanufacturing.test:1.0.0:TestFileTwo.ttl";
   private static final String nameSpaceThree = "io.openmanufacturing.test:1.0.0:TestFileThree.ttl";

   @Test
   void testValidateImportAspectModelPackage() throws IOException {
      final Path storagePath = Paths.get( resourcesPath.toString(), "test-packages", "io.openmanufacturing.1.0.0" );
      final Path zipFilePath = Paths.get( resourcesPath.toString(), "TestArchive.zip" );

      final MockMultipartFile mockedZipFile = new MockMultipartFile( "TestArchive.zip",
            Files.readAllBytes( zipFilePath ) );

      final ValidationProcess validationProcess = Mockito.mock( ValidationProcess.class );
      when( validationProcess.getPath() ).thenReturn( storagePath );

      final ProcessPackage importPackage = packageService.validateImportAspectModelPackage( mockedZipFile,
            validationProcess, resourcesPath );

      assertEquals( importPackage.getValidFiles().size(), 2 );
      assertEquals( importPackage.getInvalidFiles().size(), 1 );
   }

   @Test
   void testValidateAspectModels() {
      final Path exportedStoragePath = Paths.get( resourcesPath.toString(), "test-packages" );
      final List<String> aspectModelFiles = List.of( nameSpaceOne, nameSpaceTwo );

      final ValidationProcess validationProcess = Mockito.mock( ValidationProcess.class );
      Mockito.when( validationProcess.getPath() ).thenReturn( exportedStoragePath );

      final ProcessPackage processedExportedPackage = packageService.validateAspectModelsForExport( aspectModelFiles,
            validationProcess, resourcesPath );

      assertEquals( 2, processedExportedPackage.getValidFiles().size() );
      assertEquals( 1, processedExportedPackage.getMissingElements().size() );

      final String[] nameSpaceOneArray = nameSpaceOne.split( ":" );
      final String[] nameSpaceThreeArray = nameSpaceThree.split( ":" );

      assertTrue( processedExportedPackage.getMissingElements().get( 0 ).getAnalysedFileName()
                                          .contains( nameSpaceOneArray[2] ) );

      assertTrue( processedExportedPackage.getMissingElements().get( 0 ).getMissingFileName()
                                          .contains( nameSpaceThreeArray[2] ) );
   }

   @Test
   void testExportAspectModelPackage() {
      try ( final MockedStatic<LocalFolderResolverUtils> utilities = Mockito.mockStatic(
            LocalFolderResolverUtils.class ) ) {

         final FolderStructure one = new FolderStructure( "io.openmanufacturing.test", "1.0.0",
               "TestFileOne.ttl" );
         final FolderStructure two = new FolderStructure( "io.openmanufacturing.test", "1.0.0",
               "TestFileTwo.ttl" );
         final FolderStructure three = new FolderStructure( "io.openmanufacturing.test", "1.0.0",
               "TestFileThree.ttl" );

         final AspectModelUrn one_urn = AspectModelUrn.from( "urn:bamm:io.openmanufacturing.test:1.0.0#TestFileOne" )
                                                      .get();
         final AspectModelUrn two_urn = AspectModelUrn.from( "urn:bamm:io.openmanufacturing.test:1.0.0#TestFileTwo" )
                                                      .get();
         final AspectModelUrn three_urn = AspectModelUrn.from(
                                                              "urn:bamm:io.openmanufacturing.test:1.0.0#TestFileThree" )
                                                        .get();

         utilities.when( () -> LocalFolderResolverUtils.deleteDirectory( any( File.class ) ) )
                  .thenAnswer( (Answer<Void>) invocation -> null );

         utilities.when( () -> LocalFolderResolverUtils.extractFilePath( any( String.class ) ) )
                  .thenReturn( one, two, three );

         utilities.when( () -> LocalFolderResolverUtils.convertToAspectModelUrn( any( String.class ) ) )
                  .thenReturn( one_urn, two_urn, three_urn );

         final Path exportedStoragePath = Paths.get( resourcesPath.toString(), "test-packages" );
         final List<String> aspectModelFiles = List.of( nameSpaceOne, nameSpaceTwo, nameSpaceThree );

         final ValidationProcess validationProcess = Mockito.mock( ValidationProcess.class );
         when( validationProcess.getPath() ).thenReturn( exportedStoragePath );

         packageService.validateAspectModelsForExport( aspectModelFiles, validationProcess, resourcesPath );

         final byte[] bytes = packageService.exportAspectModelPackage( "TestExportArchive.zip",
               validationProcess );

         assertTrue( bytes.length > 0 );
      }
   }

   @Test
   void testBackupWorkspace() {
      packageService.backupWorkspace( workspaceToBackupPath.toAbsolutePath(), resourcesPath.toAbsolutePath() );

      assertTrue( Arrays.stream( Objects.requireNonNull( resourcesPath.toFile().list() ) )
                        .anyMatch( file -> file.contains( "backup" ) ) );
   }
}
