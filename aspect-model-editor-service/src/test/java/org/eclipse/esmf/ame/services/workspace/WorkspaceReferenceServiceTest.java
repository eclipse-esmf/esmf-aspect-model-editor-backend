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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.eclipse.esmf.ame.services.workspace.WorkspaceTestFiles.consumerB;
import static org.eclipse.esmf.ame.services.workspace.WorkspaceTestFiles.providerA;
import static org.eclipse.esmf.ame.services.workspace.WorkspaceTestFiles.write;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.eclipse.esmf.ame.model.ModelReference;
import org.eclipse.esmf.ame.model.ReferenceReport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WorkspaceReferenceServiceTest {
   @TempDir
   Path workspace;

   private WorkspaceReferenceService service;

   @BeforeEach
   void setUp() {
      service = new WorkspaceReferenceService( workspace );
   }

   @Test
   void namespaceUsedByAnotherNamespaceIsNotDeletable() {
      providerA( workspace );
      consumerB( workspace );

      final ReferenceReport report = service.findNamespaceReferences( "org.example.a", "1.0.0" );

      assertFalse( report.deletable() );
      assertEquals( List.of(
            new ModelReference( "org.example.b", "1.0.0", "Consumer.ttl", List.of( "urn:samm:org.example.a:1.0.0#sharedProperty" ) ) ),
            report.references() );
      assertTrue( report.unreadableFiles().isEmpty() );
   }

   @Test
   void everyReferencingFileIsListed() {
      final Path provider = providerA( workspace );
      consumerB( workspace );
      write( workspace, "org.example.b", "1.0.0", "SecondConsumer.ttl", """
            @prefix : <urn:samm:org.example.b:1.0.0#> .
            @prefix a: <urn:samm:org.example.a:1.0.0#> .
            :SecondConsumer a samm:Aspect ; samm:properties ( a:sharedProperty ) ; samm:operations ( ) .
            """ );
      write( workspace, "org.example.c", "2.0.0", "ThirdConsumer.ttl", """
            @prefix : <urn:samm:org.example.c:2.0.0#> .
            @prefix a: <urn:samm:org.example.a:1.0.0#> .
            :ThirdConsumer a samm:Aspect ; samm:properties ( a:sharedProperty ) ; samm:operations ( ) .
            """ );

      final ReferenceReport fileReport = service.findFileReferences( provider );
      final ReferenceReport namespaceReport = service.findNamespaceReferences( "org.example.a", "1.0.0" );

      for ( final ReferenceReport report : List.of( fileReport, namespaceReport ) ) {
         assertFalse( report.deletable() );
         assertEquals( List.of( "org.example.b:Consumer.ttl", "org.example.b:SecondConsumer.ttl", "org.example.c:ThirdConsumer.ttl" ),
               report.references().stream().map( reference -> reference.namespace() + ":" + reference.fileName() ).sorted().toList() );
      }
   }

   @Test
   void outgoingReferencesDoNotBlock() {
      providerA( workspace );
      consumerB( workspace );

      final ReferenceReport report = service.findNamespaceReferences( "org.example.b", "1.0.0" );

      assertTrue( report.deletable() );
      assertTrue( report.references().isEmpty() );
   }

   @Test
   void referencesInsideTheNamespaceAreIgnoredForNamespaces() {
      providerA( workspace );
      write( workspace, "org.example.a", "1.0.0", "SameNamespace.ttl", """
            @prefix : <urn:samm:org.example.a:1.0.0#> .
            :Other a samm:Aspect ; samm:properties ( :sharedProperty ) ; samm:operations ( ) .
            """ );

      assertTrue( service.findNamespaceReferences( "org.example.a", "1.0.0" ).deletable() );
   }

   @Test
   void anotherVersionOfTheSameNamespaceCounts() {
      providerA( workspace );
      write( workspace, "org.example.a", "2.0.0", "Next.ttl", """
            @prefix : <urn:samm:org.example.a:2.0.0#> .
            @prefix old: <urn:samm:org.example.a:1.0.0#> .
            :Next a samm:Aspect ; samm:properties ( old:sharedProperty ) ; samm:operations ( ) .
            """ );

      final ReferenceReport report = service.findNamespaceReferences( "org.example.a", "1.0.0" );

      assertFalse( report.deletable() );
      assertEquals( List.of( "2.0.0" ), report.references().stream().map( ModelReference::version ).toList() );
   }

   @Test
   void fileUsedByAFileOfTheSameNamespaceIsNotDeletable() {
      final Path provider = providerA( workspace );
      write( workspace, "org.example.a", "1.0.0", "SameNamespace.ttl", """
            @prefix : <urn:samm:org.example.a:1.0.0#> .
            :Other a samm:Aspect ; samm:properties ( :sharedProperty ) ; samm:operations ( ) .
            """ );

      final ReferenceReport report = service.findFileReferences( provider );

      assertFalse( report.deletable() );
      assertEquals( List.of( "SameNamespace.ttl" ), report.references().stream().map( ModelReference::fileName ).toList() );
   }

   @Test
   void fileWithOnlyOutgoingReferencesIsDeletable() {
      providerA( workspace );
      final Path consumer = consumerB( workspace );

      assertTrue( service.findFileReferences( consumer ).deletable() );
   }

   @Test
   void elementsStillDefinedByAnotherFileAreNoReference() {
      final Path provider = providerA( workspace );
      write( workspace, "org.example.a", "1.0.0", "Duplicate.ttl", """
            @prefix : <urn:samm:org.example.a:1.0.0#> .
            :sharedProperty a samm:Property ; samm:characteristic :SharedCharacteristic .
            :SharedCharacteristic a samm:Characteristic ; samm:dataType xsd:string .
            """ );
      consumerB( workspace );

      assertTrue( service.findFileReferences( provider ).deletable() );
   }

   @Test
   void elementsDefinedOnlyInTheFileToDeleteStillCount() {
      final Path provider = providerA( workspace );
      write( workspace, "org.example.a", "1.0.0", "Duplicate.ttl", """
            @prefix : <urn:samm:org.example.a:1.0.0#> .
            :SharedCharacteristic a samm:Characteristic ; samm:dataType xsd:string .
            """ );
      consumerB( workspace );

      final ReferenceReport report = service.findFileReferences( provider );

      assertFalse( report.deletable() );
      assertEquals( List.of( "urn:samm:org.example.a:1.0.0#sharedProperty" ), report.references().getFirst().referencedElements() );
   }

   @Test
   void unreadableFilesBlockDeletion() {
      providerA( workspace );
      write( workspace, "org.example.c", "1.0.0", "Broken.ttl", "this is not turtle" );

      final ReferenceReport report = service.findNamespaceReferences( "org.example.a", "1.0.0" );

      assertFalse( report.deletable() );
      assertTrue( report.references().isEmpty() );
      assertEquals( 1, report.unreadableFiles().size() );
      assertEquals( "Broken.ttl", report.unreadableFiles().getFirst().fileName() );
   }

   @Test
   void unreadableFileToDeleteBlocksNothingElse() {
      final Path broken = write( workspace, "org.example.c", "1.0.0", "Broken.ttl", "this is not turtle" );
      consumerB( workspace );

      assertTrue( service.findFileReferences( broken ).deletable() );
   }

   @Test
   void onlyFilesAtTheStandardLocationAreListed() throws IOException {
      providerA( workspace );
      Files.writeString( workspace.resolve( "Root.ttl" ), "garbage" );
      Files.writeString( workspace.resolve( "backup-2026.zip" ), "zip" );
      write( workspace, "org.example.a", "1.0.0", "notes.txt", "" );
      Files.createDirectories( workspace.resolve( "org.example.a/1.0.0/deeper" ) );
      Files.writeString( workspace.resolve( "org.example.a/1.0.0/deeper/Deep.ttl" ), "garbage" );

      assertEquals( List.of( "Provider.ttl" ),
            service.listWorkspaceFiles().stream().map( WorkspaceReferenceService.WorkspaceFile::fileName ).toList() );
   }

   @Test
   void emptyOrMissingWorkspaceHasNoFiles() {
      assertTrue( new WorkspaceReferenceService( workspace.resolve( "missing" ) ).listWorkspaceFiles().isEmpty() );
      assertTrue( service.findNamespaceReferences( "org.example.a", "1.0.0" ).deletable() );
   }

   @Test
   void definingFileIsFoundEvenIfItsReferencesAreMissing() {
      write( workspace, "org.example.b", "1.0.0", "Other.ttl", """
            @prefix : <urn:samm:org.example.b:1.0.0#> .
            :otherProperty a samm:Property ; samm:characteristic samm-c:Text .
            """ );
      final Path consumer = consumerB( workspace );

      assertEquals( Optional.of( consumer ), service.findDefiningFile( "org.example.b", "1.0.0", "urn:samm:org.example.b:1.0.0#Consumer" ) );
   }

   @Test
   void definingFileIsOnlySearchedInTheNamespaceVersion() {
      providerA( workspace );
      write( workspace, "org.example.a", "2.0.0", "Uses.ttl", """
            @prefix : <urn:samm:org.example.a:2.0.0#> .
            @prefix old: <urn:samm:org.example.a:1.0.0#> .
            :Uses a samm:Aspect ; samm:properties ( old:sharedProperty ) ; samm:operations ( ) .
            """ );

      assertTrue( service.findDefiningFile( "org.example.a", "2.0.0", "urn:samm:org.example.a:1.0.0#sharedProperty" ).isEmpty() );
      assertTrue( service.findDefiningFile( "org.example.a", "1.0.0", "urn:samm:org.example.a:1.0.0#unknown" ).isEmpty() );
      assertTrue( service.findDefiningFile( "org.example.x", "1.0.0", "urn:samm:org.example.x:1.0.0#sharedProperty" ).isEmpty() );
   }

   @Test
   void usedButNotDefinedElementHasNoDefiningFile() {
      consumerB( workspace );

      assertTrue( service.findDefiningFile( "org.example.a", "1.0.0", "urn:samm:org.example.a:1.0.0#sharedProperty" ).isEmpty() );
   }

   @Test
   void unreadableFileFailsTheLookupOnlyIfNoOtherFileDefinesTheElement() {
      final Path provider = providerA( workspace );
      write( workspace, "org.example.a", "1.0.0", "Broken.ttl", ":broken a samm:Property ;" );

      assertEquals( Optional.of( provider ),
            service.findDefiningFile( "org.example.a", "1.0.0", "urn:samm:org.example.a:1.0.0#sharedProperty" ) );
      assertThrows( RuntimeException.class,
            () -> service.findDefiningFile( "org.example.a", "1.0.0", "urn:samm:org.example.a:1.0.0#broken" ) );
   }
}
