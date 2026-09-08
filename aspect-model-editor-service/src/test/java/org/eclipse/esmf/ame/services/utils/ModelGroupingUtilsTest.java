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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.eclipse.esmf.ame.exceptions.InvalidAspectModelException;
import org.eclipse.esmf.aspectmodel.AspectModelFile;
import org.eclipse.esmf.aspectmodel.loader.AspectModelLoader;
import org.eclipse.esmf.aspectmodel.urn.AspectModelUrn;
import org.eclipse.esmf.aspectmodel.validation.services.AspectModelValidator;
import org.eclipse.esmf.metamodel.ModelElement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ModelGroupingUtilsTest {

   private AspectModelLoader aspectModelLoader;
   private AspectModelValidator aspectModelValidator;
   private ModelGroupingUtils modelGroupingUtils;

   @BeforeEach
   void setUp() {
      aspectModelLoader = mock( AspectModelLoader.class );
      aspectModelValidator = mock( AspectModelValidator.class );
      modelGroupingUtils = new ModelGroupingUtils( aspectModelLoader, aspectModelValidator );
   }

   @Test
   void testGetUrnSuccess() {
      final ModelElement modelElement = mock( ModelElement.class );
      final AspectModelUrn expectedUrn = AspectModelUrn.fromUrn( "urn:samm:org.eclipse.esmf.example:1.0.0#Movement" );
      when( modelElement.urn() ).thenReturn( expectedUrn );

      final Optional<AspectModelUrn> result = modelGroupingUtils.getUrn( modelElement );

      assertTrue( result.isPresent() );
      assertEquals( expectedUrn, result.get() );
   }

   @Test
   void testGetUrnNullReturnsEmpty() {
      final Optional<AspectModelUrn> result = modelGroupingUtils.getUrn( null );
      assertTrue( result.isEmpty() );
   }

   @Test
   void testGetUrnFailureIncludesFileAndElementName() {
      final ModelElement modelElement = mock( ModelElement.class );
      final AspectModelFile sourceFile = mock( AspectModelFile.class );
      when( modelElement.getSourceFile() ).thenReturn( sourceFile );
      when( sourceFile.filename() ).thenReturn( Optional.of( "Movement.ttl" ) );
      when( modelElement.urn() ).thenThrow( new IllegalArgumentException( "Malformed URN syntax" ) );

      final InvalidAspectModelException ex = assertThrows( InvalidAspectModelException.class,
            () -> modelGroupingUtils.getUrn( modelElement ) );

      assertTrue( ex.getMessage().contains( "Movement.ttl" ) );
      assertTrue( ex.getMessage().contains( "Malformed URN syntax" ) );
   }

   @Test
   void testExtractSourceFileNameSuccess() {
      final ModelElement modelElement = mock( ModelElement.class );
      final AspectModelFile sourceFile = mock( AspectModelFile.class );
      when( modelElement.getSourceFile() ).thenReturn( sourceFile );
      when( sourceFile.filename() ).thenReturn( Optional.of( "Movement.ttl" ) );

      final String fileName = modelGroupingUtils.extractSourceFileName( modelElement );
      assertEquals( "Movement.ttl", fileName );
   }

   @Test
   void testExtractSourceFileNameNullReturnsUnknown() {
      final String fileName = modelGroupingUtils.extractSourceFileName( null );
      assertEquals( "unknown file", fileName );
   }
}

