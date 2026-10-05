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

package org.eclipse.esmf.ame.services.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import org.eclipse.esmf.aspectmodel.ValueParsingException;
import org.eclipse.esmf.aspectmodel.resolver.ModelResolutionViolation;
import org.eclipse.esmf.aspectmodel.resolver.exceptions.ModelResolutionException;
import org.eclipse.esmf.aspectmodel.urn.AspectModelUrn;

import org.apache.jena.rdf.model.ResourceFactory;
import org.junit.jupiter.api.Test;

class UnresolvedReferencesTest {
   private static final String FIRST = "urn:samm:org.eclipse.esmf.example:1.0.0#first";
   private static final String SECOND = "urn:samm:org.eclipse.esmf.example:1.0.0#second";

   private static ModelResolutionViolation missing( final String urn, final Optional<Throwable> cause ) {
      return new ModelResolutionViolation( Optional.of( AspectModelUrn.fromUrn( urn ) ), URI.create( "file:///x.ttl" ),
            "File does not exist", cause );
   }

   @Test
   void findsTheMissingElementsSortedAndWithoutDuplicates() {
      final ModelResolutionException mre = new ModelResolutionException(
            List.of( missing( SECOND, Optional.empty() ), missing( FIRST, Optional.empty() ), missing( SECOND, Optional.empty() ) ) );

      assertEquals( List.of( FIRST, SECOND ), UnresolvedReferences.find( new RuntimeException( "wrapped", mre ) ) );
   }

   @Test
   void findsNothingForOtherErrors() {
      assertTrue( UnresolvedReferences.find( new IllegalStateException( "boom" ) ).isEmpty() );
      assertTrue( UnresolvedReferences.find( new ModelResolutionException( List.of(
            new ModelResolutionViolation( Optional.empty(), URI.create( "file:///x.ttl" ), "General", Optional.empty() ) ) ) ).isEmpty() );
   }

   @Test
   void findsNothingIfAReferencedFileHasAValueError() {
      final ValueParsingException vpe = new ValueParsingException(
            ResourceFactory.createResource( "http://www.w3.org/2001/XMLSchema#int" ), "x", new NumberFormatException( "x" ) );

      assertTrue( UnresolvedReferences.find( new ModelResolutionException( List.of( missing( FIRST, Optional.of( vpe ) ) ) ) ).isEmpty() );
   }

   @Test
   void describesEveryMissingElement() {
      assertEquals( "Element '" + FIRST + "' does not exist in a file. Element '" + SECOND + "' does not exist in a file.",
            UnresolvedReferences.message( List.of( FIRST, SECOND ) ) );
   }
}
