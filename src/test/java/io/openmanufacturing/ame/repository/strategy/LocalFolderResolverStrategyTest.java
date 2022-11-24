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

package io.openmanufacturing.ame.repository.strategy;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import io.openmanufacturing.ame.config.ApplicationSettings;
import io.openmanufacturing.ame.exceptions.FileNotFoundException;
import io.openmanufacturing.ame.exceptions.FileReadException;
import io.openmanufacturing.ame.exceptions.FileWriteException;
import io.openmanufacturing.ame.exceptions.InvalidAspectModelException;
import io.openmanufacturing.sds.aspectmodel.urn.AspectModelUrn;

@RunWith( MockitoJUnitRunner.class )
public class LocalFolderResolverStrategyTest {

   private LocalFolderResolverStrategy localFolderResolverStrategy;

   @Mock
   private ApplicationSettings applicationSettingsMock;

   @Mock
   private File fileMock;

   @Mock
   private File parentFileMock;

   public static final String MODELS = "workspace-to-migrate";

   private static final Path resourcesPath = Path.of( "src", "test", "resources" );
   private static final String STORAGE_PATH =
         System.getProperty( "user.home" ) + File.separator + "aspect-model-editor" + File.separator + MODELS;
   private static final String NAMESPACE = "com.test.example:1.0.0:AspectDefault.ttl";
   private static final String TTL_FILE_CONTENT = "new result ttl file";
   private static final String TTL_FILE_EXTENSION = ".ttl";
   private static final String TTL_FILE_WITH_EXT_REF =
         "io.openmanufacturing" + File.separator + "1.0.0" + File.separator
               + "AspectModelWithExternalRef" + TTL_FILE_EXTENSION;
   private static final String COM_TEST_EXAMPLE_1_2_0 =
         "com" + File.separator + "test" + File.separator + "example" + File.separator + "1.2.0";
   private static final String COM_TEST_EXAMPLE_1_0_0_ASPECT_DEFAULT =
         "com" + File.separator + "test" + File.separator + "example" + File.separator + "1.0.0" + File.separator
               + "AspectDefault";

   @Before
   public void setUp() {
      localFolderResolverStrategy = spy( new LocalFolderResolverStrategy( applicationSettingsMock ) );
      doReturn( fileMock ).when( localFolderResolverStrategy ).getFileInstance( any() );
      when( applicationSettingsMock.getEndFilePath() ).thenReturn( MODELS );
   }

   @Test
   public void testCheckModelExists() {
      final boolean result = localFolderResolverStrategy.checkModelExist( NAMESPACE,
            STORAGE_PATH + File.separator + NAMESPACE );

      assertFalse( result );
   }

   @Test
   public void testGetModel() {
      doReturn( true ).when( fileMock ).exists();
      doReturn( TTL_FILE_CONTENT ).when( localFolderResolverStrategy ).getFileContent( any() );

      final String result = localFolderResolverStrategy.getModelAsString( NAMESPACE,
            STORAGE_PATH + File.separator + NAMESPACE );

      assertEquals( TTL_FILE_CONTENT, result );
   }

   @Test( expected = FileNotFoundException.class )
   public void testGetModelModelNotFound() {
      doReturn( false ).when( fileMock ).exists();

      localFolderResolverStrategy.getModelAsString( NAMESPACE, STORAGE_PATH + File.separator + NAMESPACE );
   }

   @Test
   public void testDeleteModelWithTtlNamespace() {
      try ( final MockedStatic<FileUtils> utilities = Mockito.mockStatic( FileUtils.class ) ) {
         utilities.when( () -> FileUtils.forceDelete( any( File.class ) ) )
                  .thenAnswer( (Answer<Void>) invocation -> null );

         doReturn( true ).when( fileMock ).exists();
         doReturn( "test" ).when( fileMock ).getName();
         doReturn( parentFileMock ).when( fileMock ).getParentFile();
         doReturn( new File[1] ).when( parentFileMock ).listFiles();

         localFolderResolverStrategy.deleteModel( NAMESPACE, STORAGE_PATH + File.separator + NAMESPACE );

         verify( localFolderResolverStrategy ).deleteModel( NAMESPACE, STORAGE_PATH + File.separator + NAMESPACE );
      }
   }

   @Test
   public void testDeleteModelWithEmptyFolder() {
      try ( final MockedStatic<FileUtils> utilities = Mockito.mockStatic( FileUtils.class ) ) {
         utilities.when( () -> FileUtils.forceDelete( any( File.class ) ) )
                  .thenAnswer( (Answer<Void>) invocation -> null );

         final String namespaceWithoutTll = "com.test.example:1.0.0";
         doReturn( true ).when( fileMock ).exists();
         doReturn( "test" ).when( fileMock ).getName();
         doReturn( parentFileMock ).when( fileMock ).getParentFile();
         doReturn( new File[1] ).when( parentFileMock ).listFiles();

         localFolderResolverStrategy.deleteModel( namespaceWithoutTll, STORAGE_PATH + File.separator + NAMESPACE );

         verify( localFolderResolverStrategy ).deleteModel( namespaceWithoutTll,
               STORAGE_PATH + File.separator + NAMESPACE );
      }
   }

   @Test
   public void testDeleteModelWithEmptyFolderWithoutVersion() {
      try ( final MockedStatic<FileUtils> utilities = Mockito.mockStatic( FileUtils.class ) ) {
         utilities.when( () -> FileUtils.forceDelete( any( File.class ) ) )
                  .thenAnswer( (Answer<Void>) invocation -> null );

         final String namespaceWithoutTllAndVersion = "com.test.example";
         doReturn( true ).when( fileMock ).exists();
         doReturn( "test" ).when( fileMock ).getName();
         doReturn( parentFileMock ).when( fileMock ).getParentFile();
         doReturn( new File[1] ).when( parentFileMock ).listFiles();

         localFolderResolverStrategy.deleteModel( namespaceWithoutTllAndVersion,
               STORAGE_PATH + File.separator + NAMESPACE );

         verify( localFolderResolverStrategy ).deleteModel( namespaceWithoutTllAndVersion,
               STORAGE_PATH + File.separator + NAMESPACE );
      }
   }

   @Test( expected = FileNotFoundException.class )
   public void testDeleteModelDoesNotExist() {
      doReturn( false ).when( fileMock ).exists();

      localFolderResolverStrategy.deleteModel( NAMESPACE, STORAGE_PATH + File.separator + NAMESPACE );

      verify( localFolderResolverStrategy ).deleteModel( NAMESPACE, STORAGE_PATH + File.separator + NAMESPACE );
   }

   @Test
   public void testSaveModel() throws Exception {
      doReturn( COM_TEST_EXAMPLE_1_0_0_ASPECT_DEFAULT ).when( localFolderResolverStrategy )
                                                       .getFilePathBasedOnTurtleData( TTL_FILE_CONTENT,
                                                             STORAGE_PATH + File.separator + NAMESPACE );
      when( applicationSettingsMock.getFileType() ).thenReturn( TTL_FILE_EXTENSION );
      doNothing().when( localFolderResolverStrategy ).writeToFile( any(), any() );

      final String result = localFolderResolverStrategy.saveModel( Optional.empty(), TTL_FILE_CONTENT,
            STORAGE_PATH + File.separator + NAMESPACE );

      assertEquals( COM_TEST_EXAMPLE_1_0_0_ASPECT_DEFAULT + TTL_FILE_EXTENSION, result );
   }

   @Test
   public void testGetFilePathBasedOnTurtleData() throws Exception {
      final Path extRefAspectModel = Path.of( resourcesPath.toAbsolutePath().toString(), TTL_FILE_WITH_EXT_REF );
      final AspectModelUrn aspectModelUrn = localFolderResolverStrategy.getAspectModelUrn(
            Files.readString( extRefAspectModel ),
            resourcesPath.toString() );

      assertEquals( "urn:bamm:io.openmanufacturing:1.0.0#AspectModelWithExternalRef", aspectModelUrn.toString() );
   }

   @Test( expected = FileWriteException.class )
   public void testSaveModelCanNotWriteToFile() throws Exception {
      doReturn( COM_TEST_EXAMPLE_1_0_0_ASPECT_DEFAULT ).when( localFolderResolverStrategy )
                                                       .getFilePathBasedOnTurtleData( TTL_FILE_CONTENT,
                                                             STORAGE_PATH + File.separator + NAMESPACE );
      when( applicationSettingsMock.getFileType() ).thenReturn( TTL_FILE_EXTENSION );
      doThrow( IOException.class ).when( localFolderResolverStrategy ).writeToFile( any(), any() );

      localFolderResolverStrategy.saveModel( Optional.empty(), TTL_FILE_CONTENT,
            STORAGE_PATH + File.separator + NAMESPACE );
   }

   @Test
   public void testGetAllNamespacesWithTtlFile() throws IOException {
      try ( final MockedStatic<LocalFolderResolverStrategy> utilities = Mockito.mockStatic(
            LocalFolderResolverStrategy.class ) ) {
         final Map<String, List<String>> nameSpace = Map.of( "com.test.example:1.0.0",
               Collections.singletonList( "AspectDefault.ttl" ) );

         final Path ttlEndFile = Paths.get(
               STORAGE_PATH + File.separator + COM_TEST_EXAMPLE_1_0_0_ASPECT_DEFAULT + TTL_FILE_EXTENSION );
         final Stream<Path> filePaths = Stream.of( ttlEndFile );

         when( fileMock.exists() ).thenReturn( true );
         doReturn( filePaths ).when( localFolderResolverStrategy ).getAllSubFilePaths( any() );

         utilities.when( () -> LocalFolderResolverStrategy.transformToValidModelDirectory( any() ) )
                  .thenReturn( "com.test.example:1.0.0:AspectDefault.ttl" );

         final Map<String, List<String>> result = localFolderResolverStrategy.getAllNamespaces( true,
               ApplicationSettings.getMetaModelStoragePath() );

         assertEquals( nameSpace, result );
      }
   }

   @Test
   public void testGetAllNamespacesWithEmptyFolder() throws IOException {
      try ( final MockedStatic<LocalFolderResolverStrategy> utilities = Mockito.mockStatic(
            LocalFolderResolverStrategy.class ) ) {

         final Map<String, List<String>> expectedResult = Map.of( "com.test.example:1.2.0", Collections.emptyList() );
         final Path emptyFolder = Paths.get( STORAGE_PATH + File.separator + COM_TEST_EXAMPLE_1_2_0 );
         final Stream<Path> filePaths = Stream.of( emptyFolder );
         when( fileMock.exists() ).thenReturn( true );
         doReturn( filePaths ).when( localFolderResolverStrategy ).getAllSubFilePaths( any() );
         doReturn( true ).when( localFolderResolverStrategy ).isPathRelevant( any() );

         utilities.when( () -> LocalFolderResolverStrategy.transformToValidModelDirectory( any() ) )
                  .thenReturn( "com.test.example:1.2.0" );

         final Map<String, List<String>> result = localFolderResolverStrategy.getAllNamespaces( true,
               ApplicationSettings.getMetaModelStoragePath() );

         assertEquals( expectedResult, result );
      }
   }

   @Test( expected = FileReadException.class )
   public void testGetAllNamespacesErrorWhenCallGetFolders() throws IOException {
      when( fileMock.exists() ).thenReturn( true );
      doThrow( IOException.class ).when( localFolderResolverStrategy ).getAllSubFilePaths( any() );

      localFolderResolverStrategy.getAllNamespaces( true, ApplicationSettings.getMetaModelStoragePath() );
   }

   @Test( expected = FileNotFoundException.class )
   public void testGetAllNamespacesErrorNoSharedFolder() {
      when( fileMock.exists() ).thenReturn( false );

      localFolderResolverStrategy.getAllNamespaces( true, ApplicationSettings.getMetaModelStoragePath() );
   }

   @Test
   public void testConvertFileToUrn() {
      final Path openManufacturingTestPath = Path.of( resourcesPath.toString(), "io.openmanufacturing", "1.0.0" );
      final String expectedResult = "urn:bamm:io.openmanufacturing:1.0.0#AspectModel";
      final File testFile = new File( openManufacturingTestPath + File.separator + "AspectModel.ttl" );

      final AspectModelUrn aspectModelUrn = localFolderResolverStrategy.convertFileToUrn( testFile );

      assertEquals( expectedResult, aspectModelUrn.getUrn().toString() );
   }

   @Test( expected = InvalidAspectModelException.class )
   public void testConvertFileToUrnErrorInvalidUrn() {
      final File testFile = new File( resourcesPath + File.separator + "NoDefinedFolderStructure.ttl" );

      localFolderResolverStrategy.convertFileToUrn( testFile );
   }
}
