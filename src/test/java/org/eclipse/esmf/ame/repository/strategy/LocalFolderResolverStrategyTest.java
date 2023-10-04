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

package org.eclipse.esmf.ame.repository.strategy;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.apache.jena.riot.RiotException;
import org.eclipse.esmf.ame.config.ApplicationSettings;
import org.eclipse.esmf.ame.exceptions.FileNotFoundException;
import org.eclipse.esmf.aspectmodel.urn.AspectModelUrn;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import io.vavr.Tuple2;

@ExtendWith( SpringExtension.class )
@SpringBootTest
class LocalFolderResolverStrategyTest {
   @Autowired
   private ApplicationSettings applicationSettingsMock;

   @Autowired
   private FileSystem importFileSystemMock;

   private LocalFolderResolverStrategy localFolderResolverStrategy;

   private static final Path RESOURCE_PATH = Path.of( "src", "test", "resources", "strategy" );

   private static final String NAMESPACE_VERSION = "org.eclipse.esmf.example:1.0.0";

   private static final String MODEL = "AspectModelForStrategy.ttl";
   private static final String MODEL_WITH_EXT_REF = "AspectModelForStrategyWithExtRef.ttl";
   private static final String MODEL_NOT_EXIST = "AspectModelNotExist.ttl";

   private static final String ASPECT_MODEL_URN_WITH_EXT_REF_AS_STRING =
         "urn:samm:" + NAMESPACE_VERSION + "#" + MODEL_WITH_EXT_REF.replace( ".ttl", "" );

   private static final String TTL_FILE_CONTENT = "new result ttl file";
   private static final String TTL_FILE_WITH_EXT_REF =
         NAMESPACE_VERSION.replace( ":", File.separator ) + File.separator + MODEL_WITH_EXT_REF;

   @BeforeEach
   void setUp() {
      localFolderResolverStrategy = new LocalFolderResolverStrategy( applicationSettingsMock, importFileSystemMock,
            RESOURCE_PATH.toString() );
   }

   @Test
   void testCheckModelExists() {
      assertTrue( localFolderResolverStrategy.checkModelExist( NAMESPACE_VERSION, MODEL ) );
   }

   @Test
   void testCheckModelNotExists() {
      assertFalse( localFolderResolverStrategy.checkModelExist( NAMESPACE_VERSION, MODEL_NOT_EXIST ) );
   }

   @Test
   void testGetModelFileNotFound() {
      assertThrows( FileNotFoundException.class,
            () -> localFolderResolverStrategy.getModelAsString( NAMESPACE_VERSION, MODEL_NOT_EXIST ) );
   }

   @Test
   void testGetModel() {
      final String result = localFolderResolverStrategy.getModelAsString( NAMESPACE_VERSION, MODEL );

      assertTrue( result.contains( "<urn:samm:org.eclipse.esmf.samm:meta-model:2.1.0#>" ) );
   }

   @Test
   void testGetFilePathBasedOnTurtleData() throws IOException {
      final Path extRefAspectModel = Path.of( RESOURCE_PATH.toAbsolutePath().toString(), TTL_FILE_WITH_EXT_REF );
      final AspectModelUrn aspectModelUrn = localFolderResolverStrategy.getAspectModelUrn(
            Files.readString( extRefAspectModel ) );

      assertEquals( ASPECT_MODEL_URN_WITH_EXT_REF_AS_STRING, aspectModelUrn.toString() );
   }

   @Test()
   void testSaveModelCanNotWriteToFile() {
      assertThrows( RiotException.class,
            () -> localFolderResolverStrategy.saveModel( Optional.empty(), Optional.empty(), TTL_FILE_CONTENT ) );
   }

   @Test
   void convertFileToTuple() {
      final Path eclipseTestPath = RESOURCE_PATH.resolve( NAMESPACE_VERSION.replace( ":", File.separator ) );
      final File testFile = new File( eclipseTestPath + File.separator + MODEL );

      final Tuple2<String, String> fileInfo = localFolderResolverStrategy.convertFileToTuple( testFile );

      assertEquals( MODEL.replace( ".ttl", "" ), fileInfo._1 );
      assertEquals( NAMESPACE_VERSION, fileInfo._2 );
   }
}
