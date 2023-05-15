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

import org.eclipse.esmf.ame.model.ProcessPath;
import org.eclipse.esmf.ame.model.packaging.AspectModelFiles;
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

   private static final Path RESOURCE_PATH = Path.of( "src", "test", "resources" );
   private static final Path WORKSPACE_TO_BACKUP_PATH = Path.of( RESOURCE_PATH.toString(), "workspace-to-backup" );
   private static final String VERSION = "1.0.0";
   private static final String NAMESPACE = "org.eclipse.esmf.test";
   private static final String NAMESPACE_VERSION = NAMESPACE + ":" + VERSION;
   private static final String FILE_ONE = "TestFileOne.ttl";
   private static final String FILE_TWO = "TestFileTwo.ttl";
   private static final String FILE_THREE = "TestFileThree.ttl";

   @Test
   void testValidateImportAspectModelPackage() throws IOException {
      final Path storagePath = Paths.get( RESOURCE_PATH.toString(), "test-packages-import" );
      final Path zipFilePath = Paths.get( RESOURCE_PATH.toString(), "TestArchive.zip" );

      final MockMultipartFile mockedZipFile = new MockMultipartFile( "TestArchive.zip",
            Files.readAllBytes( zipFilePath ) );

      final ProcessPath processPath = Mockito.mock( ProcessPath.class );
      when( processPath.getPath() ).thenReturn( storagePath );

    /*  final ProcessPackage importPackage = packageService.validateImportAspectModelPackage( mockedZipFile,
            validationProcess, RESOURCE_PATH );

      assertEquals( importPackage.getValidFiles().size(), 2 );
      assertEquals( importPackage.getInvalidFiles().size(), 1 );*/
   }

   @Test
   void testValidateAspectModels() {
      final List<AspectModelFiles> aspectModelFiles = List.of(
            new AspectModelFiles( NAMESPACE_VERSION, List.of( FILE_ONE, FILE_TWO ) ) );

      //final ProcessPackage processedExportedPackage = packageService.validateAspectModelsForExport( aspectModelFiles, RESOURCE_PATH );
//
//      assertEquals( 2, processedExportedPackage.getValidFiles().size() );
//      assertEquals( 1, processedExportedPackage.getMissingElements().size() );
//
//      assertTrue( processedExportedPackage.getMissingElements().get( 0 ).getAnalysedFileName().contains( FILE_ONE ) );
//
//      assertTrue( processedExportedPackage.getMissingElements().get( 0 ).getMissingFileName().contains( FILE_THREE ) );
   }

   @Test
   void testExportAspectModelPackage() {
      try ( final MockedStatic<LocalFolderResolverUtils> utilities = Mockito.mockStatic(
            LocalFolderResolverUtils.class ) ) {

         final FolderStructure one = new FolderStructure( NAMESPACE, VERSION, FILE_ONE );
         final FolderStructure two = new FolderStructure( NAMESPACE, VERSION, FILE_TWO );
         final FolderStructure three = new FolderStructure( NAMESPACE, VERSION, FILE_THREE );
         
         utilities.when( () -> LocalFolderResolverUtils.deleteDirectory( any( File.class ) ) )
                  .thenAnswer( (Answer<Void>) invocation -> null );

         utilities.when( () -> LocalFolderResolverUtils.extractFilePath( any( String.class ) ) )
                  .thenReturn( one, two, three );

         utilities.when( () -> LocalFolderResolverUtils.buildFilePath( any( String.class ), any( String.class ) ) )
                  .thenReturn( one.toString(), two.toString(), three.toString() );

         final List<AspectModelFiles> aspectModelFiles = List.of(
               new AspectModelFiles( NAMESPACE_VERSION, List.of( FILE_ONE, FILE_TWO, FILE_THREE ) ) );


         //packageService.validateAspectModelsForExport( aspectModelFiles, RESOURCE_PATH );

         final byte[] bytes = packageService.exportAspectModelPackage( "TestExportArchive.zip" );

         assertTrue( bytes.length > 0 );
      }
   }

   @Test
   void testBackupWorkspace() {
      packageService.backupWorkspace();

      assertTrue( Arrays.stream( Objects.requireNonNull( RESOURCE_PATH.toFile().list() ) )
                        .anyMatch( file -> file.contains( "backup" ) ) );
   }
}
