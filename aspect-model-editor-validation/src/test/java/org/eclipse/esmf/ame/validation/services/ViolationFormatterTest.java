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

package org.eclipse.esmf.ame.validation.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.eclipse.esmf.ame.exceptions.UrnNotFoundException;
import org.eclipse.esmf.ame.validation.model.ViolationError;
import org.eclipse.esmf.aspectmodel.DocumentViolation;
import org.eclipse.esmf.aspectmodel.UnsupportedVersionException;
import org.eclipse.esmf.aspectmodel.Violation;
import org.eclipse.esmf.aspectmodel.ViolationCode;
import org.eclipse.esmf.aspectmodel.ViolationReport;
import org.eclipse.esmf.aspectmodel.resolver.exceptions.ParserException;
import org.eclipse.esmf.aspectmodel.shacl.fix.Fix;
import org.eclipse.esmf.aspectmodel.shacl.violation.EvaluationContext;
import org.eclipse.esmf.aspectmodel.shacl.violation.ShaclViolation;
import org.eclipse.esmf.aspectmodel.shacl.violation.SparqlConstraintViolation;
import org.eclipse.esmf.aspectmodel.urn.AspectModelUrn;
import org.eclipse.esmf.aspectmodel.validation.ProcessingViolation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ViolationFormatterTest {

   private ViolationFormatter formatter;

   @BeforeEach
   void setUp() {
      formatter = new ViolationFormatter();
   }

   @Test
   void testApplyEmptyReport() {
      final ViolationReport report = new ViolationReport( List.of() );
      final List<ViolationError> errors = formatter.apply( report );

      assertTrue( errors.isEmpty() );
   }

   @Test
   void testVisitProcessingViolationWithUrnNotFoundException() {
      final AspectModelUrn urn = AspectModelUrn.fromUrn( "urn:samm:org.eclipse.esmf.example:1.0.0#MissingProp" );
      final UrnNotFoundException cause = new UrnNotFoundException( "URN not found", urn );
      final ProcessingViolation violation = new ProcessingViolation( "Failed to resolve URN", cause );

      final ViolationError error = formatter.visitProcessingViolation( violation );

      assertNotNull( error );
      assertEquals( "Failed to resolve URN", error.getMessage() );
      assertEquals( urn, error.getFocusNode() );
      assertFalse( error.getFix().isEmpty() );
      assertTrue( error.getFix().getFirst().contains( "Ensure the referred element is available" ) );
   }

   @Test
   void testVisitProcessingViolationWithUnsupportedVersionException() {
      final UnsupportedVersionException cause = new UnsupportedVersionException( "Version 1.0.0 not supported" );
      final ProcessingViolation violation = new ProcessingViolation( "Unsupported version in model", cause );

      final ViolationError error = formatter.visitProcessingViolation( violation );

      assertNotNull( error );
      assertFalse( error.getFix().isEmpty() );
      assertTrue( error.getFix().getFirst().contains( "unsupported SAMM version" ) );
   }

   @Test
   void testVisitProcessingViolationWithParserException() {
      final org.apache.jena.riot.RiotException riotEx = new org.apache.jena.riot.RiotException( "Syntax error" );
      final ParserException cause = new ParserException( riotEx, "Syntax error", java.net.URI.create( "file:///model.ttl" ) );
      final ProcessingViolation violation = new ProcessingViolation( "Parsing failed", cause );

      final ViolationError error = formatter.visitProcessingViolation( violation );

      assertNotNull( error );
      assertFalse( error.getFix().isEmpty() );
      assertTrue( error.getFix().getFirst().contains( "Turtle syntax contains errors" ) );
   }

   @Test
   void testVisitSparqlConstraintViolationWithBlankNodeAndErrWrongDatatype() {
      final EvaluationContext context = mock( EvaluationContext.class );
      final Resource anonResource = ResourceFactory.createResource(); // Blank node: isAnon() == true, getURI() == null
      when( context.element() ).thenReturn( anonResource );

      final String highlightUrn = "urn:samm:org.eclipse.esmf.samm:characteristic:2.2.0#UnitReference";
      final Map<String, RDFNode> bindings = Map.of(
            "highlight", ResourceFactory.createResource( highlightUrn ),
            "code", ResourceFactory.createPlainLiteral( "ERR_WRONG_DATATYPE" ),
            "this", anonResource,
            "value", ResourceFactory.createResource( highlightUrn )
      );

      final SparqlConstraintViolation violation = mock( SparqlConstraintViolation.class );
      when( violation.accept( any() ) ).thenCallRealMethod();
      when( violation.errorCode() ).thenReturn( "ERR_WRONG_DATATYPE" );
      when( violation.code() ).thenReturn( new ViolationCode( "ERR_WRONG_DATATYPE", "https://esmf.eclipse.org" ) );
      when( violation.message() ).thenReturn(
            "The dataType '" + highlightUrn + "' used on Characteristic '_:3c3d685655be8180cf552b2349684f53' is neither an allowed xsd or rdf type" );
      when( violation.context() ).thenReturn( context );
      when( violation.bindings() ).thenReturn( bindings );
      when( violation.fixes() ).thenReturn( List.of() );
      when( violation.sourceLocation() ).thenReturn( Optional.of( java.net.URI.create( "file:///workspace/models/TestModel.ttl" ) ) );

      final ViolationReport report = new ViolationReport( List.of( violation ) );
      final List<ViolationError> errors = formatter.apply( report );

      assertEquals( 1, errors.size() );
      final ViolationError error = errors.getFirst();
      assertEquals( "ERR_WRONG_DATATYPE", error.getErrorCode() );
      assertNull( error.getFocusNode() );
      assertTrue( error.getMessage().startsWith( "In '/workspace/models/TestModel.ttl':" ) );
      assertTrue( error.getMessage().contains( "(anonymous element)" ) );
      assertFalse( error.getMessage().contains( "_:3c3d685655be8180cf552b2349684f53" ) );
   }

   @Test
   void testVisitShaclViolationWithFixes() {
      final Fix fix = mock( Fix.class );
      when( fix.description() ).thenReturn( "Use valid type" );

      final ShaclViolation violation = mock( ShaclViolation.class );
      when( violation.message() ).thenReturn( "Constraint violation" );
      when( violation.code() ).thenReturn( new ViolationCode( "ERR_TEST", "https://esmf.eclipse.org" ) );
      when( violation.fixes() ).thenReturn( List.of( fix ) );
      when( violation.sourceLocation() ).thenReturn( Optional.empty() );

      final ViolationError error = formatter.visit( violation );

      assertNotNull( error );
      assertFalse( error.getFix().isEmpty() );
      assertTrue( error.getFix().getFirst().contains( "Use valid type" ) );
   }

   @Test
   void testSourceLocationIncludedInMessage() {
      final SparqlConstraintViolation violation = mock( SparqlConstraintViolation.class );
      when( violation.accept( any() ) ).thenCallRealMethod();
      when( violation.message() ).thenReturn( "Something is wrong" );
      when( violation.sourceLocation() ).thenReturn( Optional.of( java.net.URI.create( "file:///workspace/models/TestModel.ttl" ) ) );
      when( violation.context() ).thenReturn( null );
      when( violation.fixes() ).thenReturn( List.of() );

      final ViolationError error = formatter.visitSparqlConstraintViolation( violation );

      assertNotNull( error );
      assertTrue( error.getMessage().startsWith( "In '/workspace/models/TestModel.ttl': Something is wrong" ) );
   }

   @Test
   void testDocumentViolationWhenSourceDocumentThrows() {
      final DocumentViolation violation = mock( DocumentViolation.class );
      when( violation.message() ).thenReturn( "Violation message" );
      when( violation.sourceDocument() ).thenThrow( new RuntimeException( "Could not determine source document" ) );

      final ViolationError error = formatter.visit( violation );

      assertNotNull( error );
      assertEquals( "Violation message", error.getMessage() );
   }
}


