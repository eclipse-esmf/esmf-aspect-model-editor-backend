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

package org.eclipse.esmf.ame.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.esmf.ame.api.model.response.AspectModelResponse;
import org.eclipse.esmf.ame.api.model.response.StoragePathResponse;
import org.eclipse.esmf.ame.exceptions.FileNotFoundException;
import org.eclipse.esmf.ame.exceptions.InvalidAspectModelException;
import org.eclipse.esmf.ame.exceptions.ModelReferencedException;
import org.eclipse.esmf.ame.exceptions.UriNotDefinedException;
import org.eclipse.esmf.ame.model.MockFileUpload;
import org.eclipse.esmf.ame.model.ModelReference;
import org.eclipse.esmf.ame.model.ReferenceReport;
import org.eclipse.esmf.ame.security.FileNameSanitizer;
import org.eclipse.esmf.ame.services.AspectModelMigrator;
import org.eclipse.esmf.ame.services.AspectModelReader;
import org.eclipse.esmf.ame.services.AspectModelValidationService;
import org.eclipse.esmf.ame.services.AspectModelWriter;
import org.eclipse.esmf.ame.services.ModelService;
import org.eclipse.esmf.ame.services.models.AspectModelResult;
import org.eclipse.esmf.ame.services.models.ClearWorkspaceResult;
import org.eclipse.esmf.ame.services.models.FileEntry;
import org.eclipse.esmf.ame.services.models.FileInformation;
import org.eclipse.esmf.ame.services.models.MigrationResult;
import org.eclipse.esmf.ame.services.models.Version;
import org.eclipse.esmf.ame.services.workspace.WorkspaceCleanupService;
import org.eclipse.esmf.ame.validation.model.ViolationReport;
import org.eclipse.esmf.aspectmodel.urn.AspectModelUrn;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.multipart.CompletedFileUpload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ModelControllerTest {

   private ModelService modelService;
   private AspectModelReader aspectModelReader;
   private AspectModelWriter aspectModelWriter;
   private AspectModelValidationService validationService;
   private AspectModelMigrator aspectModelMigrator;
   private FileNameSanitizer fileNameSanitizer;
   private WorkspaceCleanupService workspaceCleanupService;
   private ModelController controller;

   private static final String VALID_URN = "urn:samm:org.eclipse.esmf.example:1.0.0#Movement";

   @BeforeEach
   void setUp() {
      modelService = mock( ModelService.class );
      aspectModelReader = mock( AspectModelReader.class );
      aspectModelWriter = mock( AspectModelWriter.class );
      validationService = mock( AspectModelValidationService.class );
      aspectModelMigrator = mock( AspectModelMigrator.class );
      fileNameSanitizer = new FileNameSanitizer();
      workspaceCleanupService = mock( WorkspaceCleanupService.class );

      controller = new ModelController(
            modelService,
            aspectModelReader,
            aspectModelWriter,
            validationService,
            aspectModelMigrator,
            fileNameSanitizer,
            workspaceCleanupService
      );
   }

   @Test
   void testGetModelSuccess() {
      final AspectModelResult result = new AspectModelResult( Optional.of( "Movement.ttl" ), "turtle-content", Optional.empty() );
      when( aspectModelReader.getModel( eq( AspectModelUrn.fromUrn( VALID_URN ) ), any() ) ).thenReturn( result );

      final HttpResponse<AspectModelResponse> response = controller.getModel( Optional.of( VALID_URN ), false );

      assertEquals( HttpStatus.OK, response.getStatus() );
      assertNotNull( response.body() );
      assertEquals( "turtle-content", response.body().content() );
   }

   @Test
   void testGetModelWithIgnoreMissingReturnsTheUnresolvedFile() {
      final AspectModelUrn urn = AspectModelUrn.fromUrn( VALID_URN );
      when( aspectModelReader.getModel( eq( urn ), any() ) ).thenThrow( new FileNotFoundException( "resolution failed" ) );
      when( modelService.getUnresolvedModel( urn ) ).thenReturn(
            Optional.of( new AspectModelResult( Optional.of( "Movement.ttl" ), "raw-content", Optional.empty() ) ) );

      final HttpResponse<AspectModelResponse> response = controller.getModel( Optional.of( VALID_URN ), true );

      assertEquals( HttpStatus.OK, response.getStatus() );
      assertEquals( "raw-content", response.body().content() );
   }

   @Test
   void testGetModelWithIgnoreMissingFailsIfNoFileDefinesTheElement() {
      final AspectModelUrn urn = AspectModelUrn.fromUrn( VALID_URN );
      final FileNotFoundException notFound = new FileNotFoundException( "resolution failed" );
      when( aspectModelReader.getModel( eq( urn ), any() ) ).thenThrow( notFound );
      when( modelService.getUnresolvedModel( urn ) ).thenReturn( Optional.empty() );

      assertSame( notFound, assertThrows( FileNotFoundException.class, () -> controller.getModel( Optional.of( VALID_URN ), true ) ) );
   }

   @Test
   void testGetModelWithoutIgnoreMissingDoesNotLookForTheUnresolvedFile() {
      final AspectModelUrn urn = AspectModelUrn.fromUrn( VALID_URN );
      when( aspectModelReader.getModel( eq( urn ), any() ) ).thenThrow( new FileNotFoundException( "resolution failed" ) );

      assertThrows( FileNotFoundException.class, () -> controller.getModel( Optional.of( VALID_URN ), false ) );
      verify( modelService, never() ).getUnresolvedModel( any() );
   }

   @Test
   void testGetModelMissingUrnThrowsFileNotFoundException() {
      final FileNotFoundException ex = assertThrows( FileNotFoundException.class, () -> controller.getModel( Optional.empty(), false ) );
      assertEquals( "Please specify an aspect model urn", ex.getMessage() );
      assertEquals( 404, ex.getHttpStatusCode() );
   }

   @Test
   void testGetModelInvalidUrnThrowsInvalidAspectModelException() {
      final InvalidAspectModelException ex = assertThrows( InvalidAspectModelException.class,
            () -> controller.getModel( Optional.of( "urn:invalid:format" ), false ) );
      assertEquals( 409, ex.getHttpStatusCode() );
      assertTrue( ex.getMessage().contains( "Invalid Aspect Model URN format" ) );
   }

   @Test
   void testCheckElementExistsSuccess() {
      when( aspectModelReader.checkElementExists( eq( AspectModelUrn.fromUrn( VALID_URN ) ), eq( "Movement.ttl" ) ) ).thenReturn( true );

      final HttpResponse<Boolean> response = controller.checkElementExists( Optional.of( VALID_URN ), "Movement.ttl" );

      assertEquals( HttpStatus.OK, response.getStatus() );
      assertEquals( Boolean.TRUE, response.body() );
   }

   @Test
   void testGetModelsBatchSuccess() {
      final List<FileEntry> entries = List.of( new FileEntry( "key", "file.ttl", VALID_URN, "2.2.0" ) );
      final List<FileInformation> fileInfoList = List.of( new FileInformation( "key", VALID_URN, "2.2.0", "content", "file.ttl" ) );
      when( modelService.getModels( entries, false ) ).thenReturn( fileInfoList );

      final HttpResponse<List<FileInformation>> response = controller.getModels( entries, false );

      assertEquals( HttpStatus.OK, response.getStatus() );
      assertEquals( 1, response.body().size() );
   }

   @Test
   void testGetModelsBatchPassesIgnoreMissing() {
      final List<FileEntry> entries = List.of( new FileEntry( null, null, VALID_URN, null ) );
      when( modelService.getModels( entries, true ) ).thenReturn( List.of() );

      final HttpResponse<List<FileInformation>> response = controller.getModels( entries, true );

      assertEquals( HttpStatus.OK, response.getStatus() );
      assertTrue( response.body().isEmpty() );
      verify( modelService ).getModels( entries, true );
   }

   @Test
   void testCreateOrSaveModelSuccess() {
      final HttpResponse<String> response = controller.createOrSaveModel( "content", Optional.of( VALID_URN ), Optional.of( "Movement.ttl" ) );

      assertEquals( HttpStatus.CREATED, response.getStatus() );
      verify( aspectModelWriter ).saveModel( eq( "content" ), eq( AspectModelUrn.fromUrn( VALID_URN ) ), eq( "Movement.ttl" ), any() );
   }

   @Test
   void testDeleteModelSuccess() {
      controller.deleteModel( Optional.of( VALID_URN ) );
      verify( aspectModelWriter ).deleteModel( eq( AspectModelUrn.fromUrn( VALID_URN ) ) );
   }

   @Test
   void testDeleteModelStillReferencedPropagatesException() {
      final ModelReferencedException exception = new ModelReferencedException( "used", blockedReport() );
      doThrow( exception ).when( aspectModelWriter ).deleteModel( any() );

      assertEquals( exception, assertThrows( ModelReferencedException.class, () -> controller.deleteModel( Optional.of( VALID_URN ) ) ) );
   }

   @Test
   void testGetReferencesOfNamespace() {
      final ReferenceReport report = blockedReport();
      when( workspaceCleanupService.checkNamespace( "org.example", "1.0.0" ) ).thenReturn( report );

      final HttpResponse<ReferenceReport> response = controller.getReferences( "org.example", "1.0.0", Optional.empty() );

      assertEquals( HttpStatus.OK, response.getStatus() );
      assertEquals( report, response.body() );
   }

   @Test
   void testGetReferencesOfFile() {
      final ReferenceReport report = ReferenceReport.empty();
      when( workspaceCleanupService.checkFile( "org.example", "1.0.0", "A.ttl" ) ).thenReturn( report );

      final HttpResponse<ReferenceReport> response = controller.getReferences( "org.example", "1.0.0", Optional.of( "A.ttl" ) );

      assertTrue( response.body().deletable() );
   }

   @Test
   void testDeleteNamespaceSuccess() {
      final HttpResponse<ReferenceReport> response = controller.deleteNamespace( "org.example", "1.0.0" );

      assertEquals( HttpStatus.OK, response.getStatus() );
      assertEquals( ReferenceReport.empty(), response.body() );
      verify( workspaceCleanupService ).deleteNamespace( "org.example", "1.0.0" );
   }

   @Test
   void testDeleteNamespaceStillReferencedPropagatesException() {
      when( workspaceCleanupService.deleteNamespace( "org.example", "1.0.0" ) )
            .thenThrow( new ModelReferencedException( "used", blockedReport() ) );

      assertThrows( ModelReferencedException.class, () -> controller.deleteNamespace( "org.example", "1.0.0" ) );
   }

   @Test
   void testClearWorkspace() {
      when( workspaceCleanupService.clearWorkspace( true ) ).thenReturn( new ClearWorkspaceResult( 3, true ) );

      final HttpResponse<ClearWorkspaceResult> response = controller.clearWorkspace( true );

      assertEquals( HttpStatus.OK, response.getStatus() );
      assertEquals( 3, response.body().deletedFiles() );
   }

   private static ReferenceReport blockedReport() {
      return ReferenceReport.of(
            List.of( new ModelReference( "org.other", "1.0.0", "B.ttl", List.of( "urn:samm:org.example:1.0.0#Prop" ) ) ), List.of() );
   }

   @Test
   void testValidateModelSuccess() {
      final CompletedFileUpload upload = MockFileUpload.create( "test.ttl" );
      final ViolationReport report = new ViolationReport( Collections.emptyList() );
      when( validationService.validate( any( URI.class ), eq( upload ) ) ).thenReturn( report );

      final HttpResponse<ViolationReport> response = controller.validateModel( Optional.of( "file:///test.ttl" ), upload );

      assertEquals( HttpStatus.OK, response.getStatus() );
      assertNotNull( response.body() );
   }

   @Test
   void testValidateModelMissingUriThrowsUriNotDefinedException() {
      final CompletedFileUpload upload = MockFileUpload.create( "test.ttl" );

      final UriNotDefinedException ex = assertThrows( UriNotDefinedException.class,
            () -> controller.validateModel( Optional.empty(), upload ) );
      assertEquals( 422, ex.getHttpStatusCode() );
   }

   @Test
   void testValidateModelInvalidUriThrowsUriNotDefinedException() {
      final CompletedFileUpload upload = MockFileUpload.create( "test.ttl" );

      final UriNotDefinedException ex = assertThrows( UriNotDefinedException.class,
            () -> controller.validateModel( Optional.of( "ht tp://invalid uri" ), upload ) );
      assertEquals( 422, ex.getHttpStatusCode() );
      assertTrue( ex.getMessage().contains( "Invalid URI format" ) );
   }

   @Test
   void testMigrateModelSuccess() {
      final CompletedFileUpload upload = MockFileUpload.create( "test.ttl" );
      when( aspectModelMigrator.migrate( any( URI.class ), eq( upload ) ) ).thenReturn( "migrated-turtle" );

      final HttpResponse<String> response = controller.migrateModel( Optional.of( "file:///test.ttl" ), upload );

      assertEquals( HttpStatus.OK, response.getStatus() );
      assertEquals( "migrated-turtle", response.body() );
   }

   @Test
   void testFormatModelSuccess() {
      final CompletedFileUpload upload = MockFileUpload.create( "test.ttl" );
      when( aspectModelMigrator.format( any( URI.class ), eq( upload ) ) ).thenReturn( "formatted-turtle" );

      final HttpResponse<String> response = controller.getFormattedModel( Optional.of( "file:///test.ttl" ), upload );

      assertEquals( HttpStatus.OK, response.getStatus() );
      assertEquals( "formatted-turtle", response.body() );
   }

   @Test
   void testGetAllNamespacesSuccess() {
      final Map<String, List<Version>> namespaces = Map.of( "org.eclipse.esmf.example", Collections.emptyList() );
      when( modelService.getAllNamespaces() ).thenReturn( namespaces );

      final HttpResponse<Map<String, List<Version>>> response = controller.getAllNamespaces();

      assertEquals( HttpStatus.OK, response.getStatus() );
      assertNotNull( response.body() );
      assertTrue( response.body().containsKey( "org.eclipse.esmf.example" ) );
   }

   @Test
   void testMigrateWorkspaceSuccess() {
      final MigrationResult result = new MigrationResult( true, Collections.emptyList() );
      when( aspectModelMigrator.migrateWorkspace( any(), eq( false ), any() ) ).thenReturn( result );

      final HttpResponse<MigrationResult> response = controller.migrateWorkspace( false );

      assertEquals( HttpStatus.OK, response.getStatus() );
      assertNotNull( response.body() );
      assertTrue( response.body().success() );
   }

   @Test
   void testGetStoragePathSuccess() {
      final Path testPath = Path.of( "/tmp/test-models" );
      when( modelService.getModelPath() ).thenReturn( testPath );

      final HttpResponse<StoragePathResponse> response = controller.getStoragePath();

      assertEquals( HttpStatus.OK, response.getStatus() );
      assertNotNull( response.body() );
      assertEquals( testPath.toAbsolutePath().toString(), response.body().path() );
      assertEquals( testPath.toAbsolutePath().toString(), response.body().storagePath() );
   }

   @Test
   void testGetPathSuccess() {
      final Path testPath = Path.of( "/tmp/test-models" );
      when( modelService.getModelPath() ).thenReturn( testPath );

      final HttpResponse<StoragePathResponse> response = controller.getPath();

      assertEquals( HttpStatus.OK, response.getStatus() );
      assertNotNull( response.body() );
      assertEquals( testPath.toAbsolutePath().toString(), response.body().path() );
   }

   @Test
   void testGetModelsBatchFailure_ThrowsBatchLoadException() {
      final List<FileEntry> entries = List.of( new FileEntry( "ns:1.0.0:Model.ttl", "Model.ttl", "urn:samm:ns:1.0.0#Model", "" ) );
      final org.eclipse.esmf.ame.exceptions.AspectModelBatchLoadException batchException =
            new org.eclipse.esmf.ame.exceptions.AspectModelBatchLoadException(
                  "Failed to load", List.of() );
      when( modelService.getModels( entries, false ) ).thenThrow( batchException );

      final org.eclipse.esmf.ame.exceptions.AspectModelBatchLoadException thrown =
            assertThrows( org.eclipse.esmf.ame.exceptions.AspectModelBatchLoadException.class, () -> controller.getModels( entries, false ) );

      assertEquals( 422, thrown.getHttpStatusCode() );
   }
}

