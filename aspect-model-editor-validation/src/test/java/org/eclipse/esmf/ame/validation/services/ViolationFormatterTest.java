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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.eclipse.esmf.ame.exceptions.UrnNotFoundException;
import org.eclipse.esmf.ame.validation.model.ViolationError;
import org.eclipse.esmf.aspectmodel.UnsupportedVersionException;
import org.eclipse.esmf.aspectmodel.Violation;
import org.eclipse.esmf.aspectmodel.ViolationCode;
import org.eclipse.esmf.aspectmodel.ViolationReport;
import org.eclipse.esmf.aspectmodel.resolver.exceptions.ParserException;
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
}


