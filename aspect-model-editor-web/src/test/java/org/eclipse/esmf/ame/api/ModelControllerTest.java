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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.esmf.ame.api.model.response.AspectModelResponse;
import org.eclipse.esmf.ame.exceptions.FileNotFoundException;
import org.eclipse.esmf.ame.exceptions.InvalidAspectModelException;
import org.eclipse.esmf.ame.exceptions.UriNotDefinedException;
import org.eclipse.esmf.ame.model.MockFileUpload;
import org.eclipse.esmf.ame.security.FileNameSanitizer;
import org.eclipse.esmf.ame.services.AspectModelMigrator;
import org.eclipse.esmf.ame.services.AspectModelReader;
import org.eclipse.esmf.ame.services.AspectModelValidationService;
import org.eclipse.esmf.ame.services.AspectModelWriter;
import org.eclipse.esmf.ame.services.ModelService;
import org.eclipse.esmf.ame.services.models.AspectModelResult;
import org.eclipse.esmf.ame.services.models.FileEntry;
import org.eclipse.esmf.ame.services.models.FileInformation;
import org.eclipse.esmf.ame.services.models.MigrationResult;
import org.eclipse.esmf.ame.services.models.Version;
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

      controller = new ModelController(
            modelService,
            aspectModelReader,
            aspectModelWriter,
            validationService,
            aspectModelMigrator,
            fileNameSanitizer
      );
   }

   @Test
   void testGetModelSuccess() {
      final AspectModelResult result = new AspectModelResult( Optional.of( "Movement.ttl" ), "turtle-content", Optional.empty() );
      when( aspectModelReader.getModel( eq( AspectModelUrn.fromUrn( VALID_URN ) ), any() ) ).thenReturn( result );

      final HttpResponse<AspectModelResponse> response = controller.getModel( Optional.of( VALID_URN ) );

      assertEquals( HttpStatus.OK, response.getStatus() );
      assertNotNull( response.body() );
      assertEquals( "turtle-content", response.body().content() );
   }

   @Test
   void testGetModelMissingUrnThrowsFileNotFoundException() {
      final FileNotFoundException ex = assertThrows( FileNotFoundException.class, () -> controller.getModel( Optional.empty() ) );
      assertEquals( "Please specify an aspect model urn", ex.getMessage() );
      assertEquals( 404, ex.getHttpStatusCode() );
   }

   @Test
   void testGetModelInvalidUrnThrowsInvalidAspectModelException() {
      final InvalidAspectModelException ex = assertThrows( InvalidAspectModelException.class,
            () -> controller.getModel( Optional.of( "urn:invalid:format" ) ) );
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
      when( modelService.getModels( entries ) ).thenReturn( fileInfoList );

      final HttpResponse<List<FileInformation>> response = controller.getModels( entries );

      assertEquals( HttpStatus.OK, response.getStatus() );
      assertEquals( 1, response.body().size() );
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
}

