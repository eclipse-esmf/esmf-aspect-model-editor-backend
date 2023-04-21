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
import static org.mockito.ArgumentMatchers.any;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.apache.jena.riot.RiotException;
import org.eclipse.esmf.ame.config.ApplicationSettings;
import org.eclipse.esmf.ame.exceptions.FileNotFoundException;
import org.eclipse.esmf.ame.model.ValidationProcess;
import org.eclipse.esmf.aspectmodel.urn.AspectModelUrn;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import io.vavr.Tuple2;

@ExtendWith( SpringExtension.class )
@SpringBootTest
class LocalFolderResolverStrategyTest {
   @Autowired
   private ApplicationSettings applicationSettingsMock;

   private LocalFolderResolverStrategy localFolderResolverStrategy;
   private static final String VERSION = "1.0.0";
   private static final String NAMESPACE_VERSION = "org.eclipse.esmf.example:" + VERSION;
   private static final String MODEL = "AspectModel.ttl";
   private static final String MODEL_WITH_EXT_REF = "AspectModelWithExternalRef";
   private static final String MODEL_NOT_EXIST = "AspectModelNotExist.ttl";
   private static final String ASPECT_MODEL_URN_WITH_EXT_REF_AS_STRING =
         "urn:samm:" + NAMESPACE_VERSION + "#" + MODEL_WITH_EXT_REF;
   private static final Path RESOURCE_PATH = Path.of( "src", "test", "resources" );
   private static final String TTL_FILE_CONTENT = "new result ttl file";
   private static final String TTL_FILE_EXTENSION = ".ttl";
   private static final String TTL_FILE_WITH_EXT_REF =
         "org.eclipse.esmf.example" + File.separator + "1.0.0" + File.separator + "AspectModelWithExternalRef"
               + TTL_FILE_EXTENSION;

   @BeforeEach
   void setUp() {
      localFolderResolverStrategy = new LocalFolderResolverStrategy( applicationSettingsMock );
   }

   @Test
   void testCheckModelExists() {
      assertTrue( localFolderResolverStrategy.checkModelExist( NAMESPACE_VERSION, MODEL, RESOURCE_PATH.toString() ) );
   }

   @Test
   void testCheckModelNotExists() {
      assertFalse(
            localFolderResolverStrategy.checkModelExist( NAMESPACE_VERSION, MODEL_NOT_EXIST,
                  RESOURCE_PATH.toString() ) );
   }

   @Test
   void testGetModelFileNotFound() {
      assertThrows( FileNotFoundException.class,
            () -> localFolderResolverStrategy.getModelAsString( NAMESPACE_VERSION, MODEL_NOT_EXIST,
                  RESOURCE_PATH.toString() ) );
   }

   @Test
   void testGetModel() {
      final String result = localFolderResolverStrategy.getModelAsString( NAMESPACE_VERSION, MODEL,
            RESOURCE_PATH.toString() );

      assertTrue( result.contains( "<urn:samm:org.eclipse.esmf.samm:meta-model:2.0.0#>" ) );
   }

   @Test
   void testGetFilePathBasedOnTurtleData() throws IOException {
      try ( final MockedStatic<ValidationProcess> utilities = Mockito.mockStatic( ValidationProcess.class ) ) {
         final Path extRefAspectModel = Path.of( RESOURCE_PATH.toAbsolutePath().toString(), TTL_FILE_WITH_EXT_REF );

         final ValidationProcess validationProcess = Mockito.mock( ValidationProcess.class );
         Mockito.when( validationProcess.getPath() ).thenReturn( extRefAspectModel );

         utilities.when( () -> ValidationProcess.getEnum( any( String.class ) ) ).thenReturn( validationProcess );

         final AspectModelUrn aspectModelUrn = localFolderResolverStrategy.getAspectModelUrn(
               Files.readString( extRefAspectModel ), RESOURCE_PATH.toString() );

         assertEquals( ASPECT_MODEL_URN_WITH_EXT_REF_AS_STRING, aspectModelUrn.toString() );
      }
   }

   @Test()
   void testSaveModelCanNotWriteToFile() {
      try ( final MockedStatic<ValidationProcess> utilities = Mockito.mockStatic( ValidationProcess.class ) ) {
         final ValidationProcess validationProcess = Mockito.mock( ValidationProcess.class );
         Mockito.when( validationProcess.getPath() ).thenReturn( RESOURCE_PATH );

         utilities.when( () -> ValidationProcess.getEnum( any() ) ).thenReturn( validationProcess );

         assertThrows( RiotException.class,
               () -> localFolderResolverStrategy.saveModel( Optional.empty(), Optional.empty(), TTL_FILE_CONTENT,
                     RESOURCE_PATH.toString() ) );
      }
   }

   @Test
   void convertFileToTuple() {
      final Path eclipseTestPath = RESOURCE_PATH.resolve( Path.of( "org.eclipse.esmf.example", "1.0.0" ) );
      final String expectedResultNamespace = "org.eclipse.esmf.example:1.0.0";
      final String expectedResultAspectModel = "AspectModel";
      final File testFile = new File( eclipseTestPath + File.separator + "AspectModel.ttl" );

      final Tuple2<String, String> fileInfo = localFolderResolverStrategy.convertFileToTuple( testFile );

      assertEquals( expectedResultAspectModel, fileInfo._1 );
      assertEquals( expectedResultNamespace, fileInfo._2 );
   }
}
