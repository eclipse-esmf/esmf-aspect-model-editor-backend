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

package org.eclipse.esmf.ame.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import org.eclipse.esmf.ame.exceptions.AspectModelBatchLoadException;
import org.eclipse.esmf.ame.exceptions.FileNotFoundException;
import org.eclipse.esmf.ame.exceptions.FileReadException;
import org.eclipse.esmf.ame.model.FileLoadError;
import org.eclipse.esmf.aspectmodel.AspectLoadingException;
import org.eclipse.esmf.aspectmodel.resolver.ModelResolutionViolation;
import org.eclipse.esmf.aspectmodel.resolver.exceptions.ModelResolutionException;
import org.eclipse.esmf.aspectmodel.urn.AspectModelUrn;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FileLoadErrorHandlerTest {

   private FileLoadErrorHandler errorHandler;

   @BeforeEach
   void setUp() {
      errorHandler = new FileLoadErrorHandler();
   }

   @Test
   void testExtractErrors_FileNotFoundException() {
      final FileNotFoundException fnfe = new FileNotFoundException( "File not found" );
      final List<FileLoadError> errors = errorHandler.extractErrors( "models/A.ttl", fnfe );

      assertEquals( 1, errors.size() );
      final FileLoadError error = errors.getFirst();
      assertEquals( "models/A.ttl", error.fileIdentifier() );
      assertEquals( "models/A.ttl", error.sourceDocument() );
      assertEquals( "File not found", error.message() );
   }

   @Test
   void testExtractErrors_FileReadException() {
      final FileReadException fre = new FileReadException( "Read failed" );
      final List<FileLoadError> errors = errorHandler.extractErrors( "models/B.ttl", fre );

      assertEquals( 1, errors.size() );
      assertEquals( "Read failed", errors.getFirst().message() );
   }

   @Test
   void testExtractErrors_ModelResolutionExceptionWithViolations() {
      final URI docUri = URI.create( "file:/path/models/Movement.ttl" );
      final AspectModelUrn urn = AspectModelUrn.fromUrn( "urn:samm:org.eclipse.esmf.example:1.0.0#Movement" );
      final ModelResolutionViolation violation = new ModelResolutionViolation(
            Optional.of( urn ), docUri, "Resolution issue", Optional.empty() );

      final ModelResolutionException mre = new ModelResolutionException( List.of( violation ) );
      final List<FileLoadError> errors = errorHandler.extractErrors( "models/Movement.ttl", mre );

      assertEquals( 1, errors.size() );
      final FileLoadError error = errors.getFirst();
      assertEquals( "models/Movement.ttl", error.fileIdentifier() );
      assertEquals( docUri.toString(), error.sourceDocument() );
      assertTrue( error.message().contains( urn.toString() ) );
   }

   @Test
   void testExtractErrors_WrappedInAspectLoadingException() {
      final URI docUri = URI.create( "file:/path/models/Wrapped.ttl" );
      final ModelResolutionViolation violation = new ModelResolutionViolation(
            Optional.empty(), docUri, "Wrapped violation", Optional.empty() );
      final ModelResolutionException mre = new ModelResolutionException( List.of( violation ) );
      final AspectLoadingException wrapper = new AspectLoadingException( "Load failed", mre );

      final List<FileLoadError> errors = errorHandler.extractErrors( "models/Wrapped.ttl", wrapper );

      assertEquals( 1, errors.size() );
      assertEquals( docUri.toString(), errors.getFirst().sourceDocument() );
      assertEquals( "Wrapped violation", errors.getFirst().message() );
   }

   @Test
   void testFormatErrorMessage_SingleFile() {
      final List<FileLoadError> errors = List.of(
            new FileLoadError( "models/Single.ttl", "file:/path/Single.ttl", "Missing property" ) );

      final String message = errorHandler.formatErrorMessage( errors );

      assertTrue( message.startsWith( "Failed to load aspect model file:\n\n" ) );
      assertTrue( message.contains( "File: models/Single.ttl" ) );
      assertTrue( message.contains( "• Error: Missing property" ) );
   }

   @Test
   void testFormatErrorMessage_MultipleFilesSortedAlphabetically() {
      final List<FileLoadError> errors = List.of(
            new FileLoadError( "models/Zebra.ttl", "file:/path/Zebra.ttl", "Error in Zebra" ),
            new FileLoadError( "models/Ant.ttl", "file:/path/Ant.ttl", "Error in Ant" ) );

      final String message = errorHandler.formatErrorMessage( errors );

      assertTrue( message.startsWith( "Failed to load aspect model files:\n\n" ) );
      final int indexOfAnt = message.indexOf( "models/Ant.ttl" );
      final int indexOfZebra = message.indexOf( "models/Zebra.ttl" );
      assertTrue( indexOfAnt < indexOfZebra, "Ant should be listed before Zebra" );
   }

   @Test
   void testCreateBatchLoadException() {
      final List<FileLoadError> errors = List.of(
            new FileLoadError( "models/A.ttl", "file:/path/A.ttl", "Error A" ) );

      final AspectModelBatchLoadException exception = errorHandler.createBatchLoadException( errors );

      assertEquals( errors, exception.getErrors() );
      assertEquals( 422, exception.getHttpStatusCode() );
      assertFalse( exception.getMessage().isEmpty() );
   }

   @Test
   void testExtractErrors_SingleFileWithMultipleResolutionViolations_AllCaptured() {
      final URI docUri = URI.create( "file:/path/models/Movement.ttl" );
      final AspectModelUrn urn1 = AspectModelUrn.fromUrn( "urn:samm:org.eclipse.esmf.example:1.0.0#Prop1" );
      final AspectModelUrn urn2 = AspectModelUrn.fromUrn( "urn:samm:org.eclipse.esmf.example:1.0.0#Prop2" );

      final ModelResolutionViolation violation1 = new ModelResolutionViolation(
            Optional.of( urn1 ), docUri, "File does not exist", Optional.empty() );
      final ModelResolutionViolation violation2 = new ModelResolutionViolation(
            Optional.of( urn2 ), docUri, "File does not contain the element definition", Optional.empty() );

      final ModelResolutionException mre = new ModelResolutionException( List.of( violation1, violation2 ) );
      final List<FileLoadError> errors = errorHandler.extractErrors( "models/Movement.ttl", mre );

      assertEquals( 2, errors.size(), "Both violations should be extracted for the single file" );
      assertTrue( errors.get( 0 ).message().contains( "Prop1" ) );
      assertTrue( errors.get( 0 ).message().contains( "File does not exist" ) );
      assertTrue( errors.get( 1 ).message().contains( "Prop2" ) );
      assertTrue( errors.get( 1 ).message().contains( "File does not contain the element definition" ) );
   }

   @Test
   void testExtractErrors_ModelResolutionExceptionStringMessageWithMultipleErrors() {
      final ModelResolutionException mre = new ModelResolutionException(
            "Element 'urn:samm:org.eclipse.esmf:1.0.0#A' not found; Element 'urn:samm:org.eclipse.esmf:1.0.0#B' not found" );
      final List<FileLoadError> errors = errorHandler.extractErrors( "models/Multi.ttl", mre );

      assertEquals( 2, errors.size(), "Semicolon-separated errors should be split into individual errors" );
      assertEquals( "Element 'urn:samm:org.eclipse.esmf:1.0.0#A' not found", errors.get( 0 ).message() );
      assertEquals( "Element 'urn:samm:org.eclipse.esmf:1.0.0#B' not found", errors.get( 1 ).message() );
   }

   @Test
   void testExtractErrors_InvalidAspectModelException_MultipleSyntaxErrors() {
      final org.eclipse.esmf.ame.exceptions.InvalidAspectModelException iame =
            new org.eclipse.esmf.ame.exceptions.InvalidAspectModelException(
                  "Aspect Model has invalid syntax: Missing property description; Characteristic requires dataType" );

      final List<FileLoadError> errors = errorHandler.extractErrors( "models/Invalid.ttl", iame );

      assertEquals( 2, errors.size(), "All syntax errors should be extracted from InvalidAspectModelException" );
      assertEquals( "Missing property description", errors.get( 0 ).message() );
      assertEquals( "Characteristic requires dataType", errors.get( 1 ).message() );
   }

   @Test
   void testFormatErrorMessage_DeduplicatesIdenticalMessagesUnderSameFile() {
      final List<FileLoadError> errors = List.of(
            new FileLoadError( "models/A.ttl", "file:/path/A.ttl", "Same error" ),
            new FileLoadError( "models/A.ttl", "file:/path/A_other.ttl", "Same error" ),
            new FileLoadError( "models/A.ttl", "file:/path/A.ttl", "Different error" ) );

      final String message = errorHandler.formatErrorMessage( errors );

      final int firstIdx = message.indexOf( "• Error: Same error" );
      final int secondIdx = message.indexOf( "• Error: Same error", firstIdx + 1 );
      assertEquals( -1, secondIdx, "Identical error message should not be repeated under the same file" );
      assertTrue( message.contains( "• Error: Different error" ) );
   }

   @Test
   void testExtractErrors_ValueParsingException_ResolvesElementUrnAndProperty() {
      final String turtle = """
            @prefix : <urn:samm:org.eclipse.esmf.example:1.0.0#> .
            @prefix samm: <urn:samm:org.eclipse.esmf.samm:meta-model:2.2.0#> .
            @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
            
            :speed a samm:Property ;
               samm:exampleValue "fef"^^xsd:int .
            """;

      final org.apache.jena.rdf.model.Resource intType =
            org.apache.jena.rdf.model.ResourceFactory.createResource( "http://www.w3.org/2001/XMLSchema#int" );
      final org.eclipse.esmf.aspectmodel.ValueParsingException vpe =
            new org.eclipse.esmf.aspectmodel.ValueParsingException(
                  intType, "fef", new NumberFormatException( "For input string: \"fef\"" ) );
      vpe.setLine( 6 );
      vpe.setColumn( 21 );
      vpe.setSourceDocument( turtle );
      vpe.setSourceLocation( URI.create( "file:///models/Speed.ttl" ) );

      final List<FileLoadError> errors = errorHandler.extractErrors( "models/Speed.ttl", vpe );

      assertEquals( 1, errors.size() );
      final FileLoadError error = errors.getFirst();
      assertEquals( "models/Speed.ttl", error.fileIdentifier() );
      assertTrue( error.message().contains( "Element 'urn:samm:org.eclipse.esmf.example:1.0.0#speed'" ) );
      assertTrue( error.message().contains( "(samm:exampleValue)" ) );
      assertTrue( error.message().contains( "Invalid value \"fef\" for type xsd:int at line 6, column 21" ) );
      assertTrue( error.message().contains( "For input string: \"fef\"" ) );
   }
}
