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

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.eclipse.esmf.aspectmodel.ValueParsingException;
import org.junit.jupiter.api.Test;

class TurtleElementResolverTest {

   private static final String SAMPLE_TURTLE = """
         @prefix : <urn:samm:org.eclipse.esmf.example:1.0.0#> .
         @prefix samm: <urn:samm:org.eclipse.esmf.samm:meta-model:2.2.0#> .
         @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
         
         :Movement a samm:Aspect ;
            samm:preferredName "Movement"@en ;
            samm:description "Movement description"@en ;
            samm:properties ( :speed ) ;
            samm:operations ( ) .
         
         :speed a samm:Property ;
            samm:preferredName "Speed"@en ;
            samm:characteristic :SpeedCharacteristic ;
            samm:exampleValue "fef"^^xsd:int .
         
         :SpeedCharacteristic a samm:Characteristic ;
            samm:dataType xsd:int .
         """;

   @Test
   void testResolveContext_FindsSubjectUrnAndPredicate() {
      // Line 14 is: samm:exampleValue "fef"^^xsd:int .
      final TurtleElementContext context = TurtleElementResolver.resolveContext( SAMPLE_TURTLE, 14, 21 );

      assertTrue( context.elementUrn().isPresent(), "Element URN should be resolved" );
      assertEquals( "urn:samm:org.eclipse.esmf.example:1.0.0#speed", context.elementUrn().get() );

      assertTrue( context.predicate().isPresent(), "Predicate should be resolved" );
      assertEquals( "samm:exampleValue", context.predicate().get() );
      assertEquals( 14, context.line() );
      assertEquals( 21, context.column() );
   }

   @Test
   void testResolveContext_TargetOnSubjectLine() {
      final String turtle = """
            @prefix : <urn:samm:org.eclipse.esmf.example:1.0.0#> .
            @prefix samm: <urn:samm:org.eclipse.esmf.samm:meta-model:2.2.0#> .
            @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
            
            :speed a samm:Property ; samm:exampleValue "fef"^^xsd:int .
            """;

      final TurtleElementContext context = TurtleElementResolver.resolveContext( turtle, 5, 45 );

      assertTrue( context.elementUrn().isPresent() );
      assertEquals( "urn:samm:org.eclipse.esmf.example:1.0.0#speed", context.elementUrn().get() );
   }

   @Test
   void testResolveContext_FullIriSubject() {
      final String turtle = """
            @prefix samm: <urn:samm:org.eclipse.esmf.samm:meta-model:2.2.0#> .
            @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
            
            <urn:samm:org.eclipse.esmf.example:1.0.0#speed> a samm:Property ;
               samm:exampleValue "fef"^^xsd:int .
            """;

      final TurtleElementContext context = TurtleElementResolver.resolveContext( turtle, 5, 23 );

      assertTrue( context.elementUrn().isPresent() );
      assertEquals( "urn:samm:org.eclipse.esmf.example:1.0.0#speed", context.elementUrn().get() );
      assertTrue( context.predicate().isPresent() );
      assertEquals( "samm:exampleValue", context.predicate().get() );
   }

   @Test
   void testResolveContext_CustomPrefix() {
      final String turtle = """
            @prefix custom: <urn:custom:model:1.0.0#> .
            @prefix samm: <urn:samm:org.eclipse.esmf.samm:meta-model:2.2.0#> .
            @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
            
            custom:myProperty a samm:Property ;
               samm:exampleValue "fef"^^xsd:int .
            """;

      final TurtleElementContext context = TurtleElementResolver.resolveContext( turtle, 5, 23 );

      assertTrue( context.elementUrn().isPresent() );
      assertEquals( "urn:custom:model:1.0.0#myProperty", context.elementUrn().get() );
   }

   @Test
   void testFormatValueParsingMessage_WithResolvedContext() {
      final Resource intType = ResourceFactory.createResource( "http://www.w3.org/2001/XMLSchema#int" );
      final NumberFormatException cause = new NumberFormatException( "For input string: \"fef\"" );
      final ValueParsingException vpe = new ValueParsingException( intType, "fef", cause );
      vpe.setLine( 14 );
      vpe.setColumn( 21 );
      vpe.setSourceDocument( SAMPLE_TURTLE );
      vpe.setSourceLocation( URI.create( "file:///Movement.ttl" ) );

      final String message = TurtleElementResolver.formatValueParsingException( vpe );

      assertTrue( message.contains( "Element 'urn:samm:org.eclipse.esmf.example:1.0.0#speed'" ) );
      assertTrue( message.contains( "(samm:exampleValue)" ) );
      assertTrue( message.contains( "Invalid value \"fef\" for type xsd:int at line 14, column 21" ) );
      assertTrue( message.contains( "For input string: \"fef\"" ) );
   }

   @Test
   void testFormatValueParsingMessage_WithoutSourceDocument() {
      final Resource intType = ResourceFactory.createResource( "http://www.w3.org/2001/XMLSchema#int" );
      final NumberFormatException cause = new NumberFormatException( "For input string: \"fef\"" );
      final ValueParsingException vpe = new ValueParsingException( intType, "fef", cause );
      vpe.setLine( 10 );
      vpe.setColumn( 5 );

      final String message = TurtleElementResolver.formatValueParsingException( vpe );

      assertTrue( message.contains( "Invalid value \"fef\" for type xsd:int at line 10, column 5" ) );
      assertTrue( message.contains( "For input string: \"fef\"" ) );
   }
}
