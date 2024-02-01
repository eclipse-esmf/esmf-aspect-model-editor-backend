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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.eclipse.esmf.ame.config.TestConfig;
import org.eclipse.esmf.ame.services.model.FileValidationReport;
import org.eclipse.esmf.ame.services.model.NamespaceFileCollection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith( SpringExtension.class )
@SpringBootTest( classes = PackageService.class )
@Import( TestConfig.class )
@ActiveProfiles( "test" )
class PackageServiceTest {
   @Autowired
   private PackageService packageService;

   private static final Path RESOURCE_PATH = Path.of( "src", "test", "resources", "services" );
   private static final String VERSION = "1.0.0";
   private static final String NAMESPACE = "org.eclipse.esmf.export";
   private static final String NAMESPACE_VERSION = NAMESPACE + ":" + VERSION;
   private static final String FILE_ONE = "TestFileOne.ttl";
   private static final String FILE_TWO = "TestFileTwo.ttl";
   private static final String FILE_THREE = "TestFileThree.ttl";

   @Test
   void testValidateImportAspectModelPackage() throws IOException {
      final Path zipFilePath = Paths.get( RESOURCE_PATH.toString(), "TestArchive.zip" );
      final byte[] testPackage = Files.readAllBytes( zipFilePath );

      final MockMultipartFile mockedZipFile = new MockMultipartFile( "TestArchive.zip", testPackage );

      final FileValidationReport importPackage = packageService.validateImportAspectModelPackage( mockedZipFile );

      assertEquals( importPackage.getNamespaceFileReports().size(), 2 );
   }

   @Test
   void testExportValidateAspectModels() {
      final List<NamespaceFileCollection> aspectModelFiles = List.of(
            new NamespaceFileCollection( NAMESPACE_VERSION, List.of( FILE_ONE, FILE_TWO ) ) );

      final FileValidationReport file = packageService.validateAspectModelsForExport( aspectModelFiles );

      assertEquals( 2, file.getNamespaceFileReports().size() );
      assertEquals( 1, file.getElementMissingReports().size() );

      assertTrue( file.getElementMissingReports().get( 0 ).getAnalysedFileName().contains( FILE_ONE ) );

      assertTrue( file.getElementMissingReports().get( 0 ).getMissingFileName().contains( FILE_THREE ) );
   }

   @Test
   void testExportAspectModelPackage() {
      final List<NamespaceFileCollection> aspectModelFiles = List.of(
            new NamespaceFileCollection( NAMESPACE_VERSION, List.of( FILE_ONE, FILE_TWO, FILE_THREE ) ) );

      packageService.validateAspectModelsForExport( aspectModelFiles );

      assertTrue( packageService.exportAspectModelPackage( "TestExportArchive.zip" ).length > 0 );
   }

   @Test
   void testBackupWorkspace() {
      packageService.backupWorkspace( RESOURCE_PATH.toString() );

      assertTrue( Arrays.stream( Objects.requireNonNull( RESOURCE_PATH.toFile().list() ) )
                        .anyMatch( file -> file.contains( "backup-" ) ) );
   }
}
