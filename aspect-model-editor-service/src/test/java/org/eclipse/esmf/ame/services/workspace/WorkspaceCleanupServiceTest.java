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
package org.eclipse.esmf.ame.services.workspace;

import static org.eclipse.esmf.ame.services.workspace.WorkspaceTestFiles.consumerB;
import static org.eclipse.esmf.ame.services.workspace.WorkspaceTestFiles.providerA;
import static org.eclipse.esmf.ame.services.workspace.WorkspaceTestFiles.write;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.eclipse.esmf.ame.exceptions.FileNotFoundException;
import org.eclipse.esmf.ame.exceptions.ModelReferencedException;
import org.eclipse.esmf.ame.services.PackageService;
import org.eclipse.esmf.ame.services.file.FileOperations;
import org.eclipse.esmf.ame.services.file.FilePathResolver;
import org.eclipse.esmf.ame.services.models.ClearWorkspaceResult;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WorkspaceCleanupServiceTest {
   @TempDir
   Path workspace;

   private PackageService packageService;
   private WorkspaceCleanupService service;

   @BeforeEach
   void setUp() {
      packageService = mock( PackageService.class );
      service = new WorkspaceCleanupService( workspace, new WorkspaceReferenceService( workspace ), new FileOperations(),
            new FilePathResolver(), packageService );
   }

   @Test
   void deletesAnUnusedNamespaceVersionAndItsEmptyDirectories() throws IOException {
      providerA( workspace );
      write( workspace, "org.example.a", "1.0.0", "Second.ttl", "" );
      Files.writeString( workspace.resolve( "org.example.a/1.0.0/.DS_Store" ), "" );

      assertEquals( 2, service.deleteNamespace( "org.example.a", "1.0.0" ) );

      assertFalse( Files.exists( workspace.resolve( "org.example.a" ) ) );
   }

   @Test
   void keepsOtherVersionsOfTheNamespace() {
      providerA( workspace );
      write( workspace, "org.example.a", "2.0.0", "Next.ttl", "" );

      service.deleteNamespace( "org.example.a", "1.0.0" );

      assertFalse( Files.exists( workspace.resolve( "org.example.a/1.0.0" ) ) );
      assertTrue( Files.exists( workspace.resolve( "org.example.a/2.0.0/Next.ttl" ) ) );
   }

   @Test
   void keepsUnknownFilesInsideTheNamespace() throws IOException {
      providerA( workspace );
      Files.writeString( workspace.resolve( "org.example.a/1.0.0/notes.txt" ), "keep me" );

      service.deleteNamespace( "org.example.a", "1.0.0" );

      assertFalse( Files.exists( workspace.resolve( "org.example.a/1.0.0/Provider.ttl" ) ) );
      assertTrue( Files.exists( workspace.resolve( "org.example.a/1.0.0/notes.txt" ) ) );
   }

   @Test
   void refusesToDeleteAUsedNamespace() {
      final Path provider = providerA( workspace );
      consumerB( workspace );

      final ModelReferencedException exception = assertThrows( ModelReferencedException.class,
            () -> service.deleteNamespace( "org.example.a", "1.0.0" ) );

      assertEquals( 409, exception.getHttpStatusCode() );
      assertEquals( "Consumer.ttl", exception.getReport().references().getFirst().fileName() );
      assertTrue( Files.exists( provider ) );
   }

   @Test
   void deletesANamespaceThatOnlyUsesOthers() {
      providerA( workspace );
      final Path consumer = consumerB( workspace );

      service.deleteNamespace( "org.example.b", "1.0.0" );

      assertFalse( Files.exists( consumer ) );
   }

   @Test
   void missingNamespaceIsNotFound() {
      assertThrows( FileNotFoundException.class, () -> service.deleteNamespace( "org.example.missing", "1.0.0" ) );
   }

   @Test
   void rejectsInvalidNamespacesAndVersions() {
      final List<List<String>> invalid = List.of(
            List.of( "..", "1.0.0" ),
            List.of( "../outside", "1.0.0" ),
            List.of( "org.example.a", "../.." ),
            List.of( "org.example.a", "1.0" ),
            List.of( "/etc", "1.0.0" ),
            List.of( "org/example", "1.0.0" ) );
      for ( final List<String> input : invalid ) {
         assertThrows( IllegalArgumentException.class, () -> service.deleteNamespace( input.get( 0 ), input.get( 1 ) ), input::toString );
         assertThrows( IllegalArgumentException.class, () -> service.checkNamespace( input.get( 0 ), input.get( 1 ) ), input::toString );
      }
   }

   @Test
   void rejectsInvalidFileNames() {
      providerA( workspace );
      for ( final String fileName : List.of( "../Provider.ttl", "Provider.txt", ".hidden.ttl", "sub/Provider.ttl" ) ) {
         assertThrows( IllegalArgumentException.class, () -> service.checkFile( "org.example.a", "1.0.0", fileName ), fileName );
      }
   }

   @Test
   void checksSingleFiles() {
      providerA( workspace );
      consumerB( workspace );

      assertFalse( service.checkFile( "org.example.a", "1.0.0", "Provider.ttl" ).deletable() );
      assertTrue( service.checkFile( "org.example.b", "1.0.0", "Consumer.ttl" ).deletable() );
      assertThrows( FileNotFoundException.class, () -> service.checkFile( "org.example.a", "1.0.0", "Missing.ttl" ) );
   }

   @Test
   void clearsTheWorkspaceButKeepsBackupsAndUnknownFiles() throws IOException {
      providerA( workspace );
      consumerB( workspace );
      Files.writeString( workspace.resolve( "backup-20260101.zip" ), "zip" );
      Files.writeString( workspace.resolve( "Root.ttl" ), "not at the standard location" );

      final ClearWorkspaceResult result = service.clearWorkspace( true );

      assertEquals( new ClearWorkspaceResult( 2, true ), result );
      verify( packageService ).backupWorkspace();
      assertFalse( Files.exists( workspace.resolve( "org.example.a" ) ) );
      assertFalse( Files.exists( workspace.resolve( "org.example.b" ) ) );
      assertTrue( Files.exists( workspace.resolve( "backup-20260101.zip" ) ) );
      assertTrue( Files.exists( workspace.resolve( "Root.ttl" ) ) );
      assertTrue( Files.isDirectory( workspace ) );
   }

   @Test
   void clearingRemovesEmptyNamespaceFoldersButKeepsFoldersWithOtherContent() throws IOException {
      providerA( workspace );
      Files.createDirectories( workspace.resolve( "org.example.empty" ).resolve( "1.0.0" ) );
      Files.writeString( Files.createDirectories( workspace.resolve( "org.example.notes" ).resolve( "1.0.0" ) ).resolve( "notes.txt" ), "keep" );

      service.clearWorkspace( false );

      assertFalse( Files.exists( workspace.resolve( "org.example.a" ) ) );
      assertFalse( Files.exists( workspace.resolve( "org.example.empty" ) ) );
      assertTrue( Files.exists( workspace.resolve( "org.example.notes" ).resolve( "1.0.0" ).resolve( "notes.txt" ) ) );
   }

   @Test
   void clearsWithoutBackup() {
      providerA( workspace );

      assertEquals( new ClearWorkspaceResult( 1, false ), service.clearWorkspace( false ) );
      verify( packageService, never() ).backupWorkspace();
   }

   @Test
   void clearingAnEmptyWorkspaceCreatesNoBackup() {
      assertEquals( new ClearWorkspaceResult( 0, false ), service.clearWorkspace( true ) );
      verify( packageService, never() ).backupWorkspace();
   }
}
