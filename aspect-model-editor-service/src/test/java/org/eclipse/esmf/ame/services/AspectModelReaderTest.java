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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.eclipse.esmf.ame.exceptions.FileNotFoundException;
import org.eclipse.esmf.ame.services.models.AspectModelResult;
import org.eclipse.esmf.aspectmodel.resolver.exceptions.ModelResolutionException;
import org.eclipse.esmf.aspectmodel.urn.AspectModelUrn;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@MicronautTest
class AspectModelReaderTest {
   @Inject
   private AspectModelReader aspectModelReader;

   private static final String FILE_EXTENSION = ".ttl";

   private static final String EXAMPLE_NAMESPACE = "org.eclipse.esmf.example";
   private static final String VERSION = "1.0.0";

   private static final AspectModelUrn NAMESPACE = AspectModelUrn.fromUrn( "urn:samm:" + EXAMPLE_NAMESPACE + ":" + VERSION );

   private static final String TEST_MODEL_FOR_SERVICE = "Movement";
   private static final String TEST_MODEL_NOT_FOUND = "NOTFOUND";
   private static final String TEST_FILEPATH = Path.of( EXAMPLE_NAMESPACE, VERSION, TEST_MODEL_FOR_SERVICE + FILE_EXTENSION ).toString();

   @Test
   void testGetModel() throws ModelResolutionException {
      final AspectModelResult result = aspectModelReader.getModel( NAMESPACE.withName( TEST_MODEL_FOR_SERVICE ), TEST_FILEPATH );
      assertTrue( result.content().contains( "@prefix samm: <urn:samm:org.eclipse.esmf.samm:meta-model:2.2.0#> ." ) );
      assertTrue( result.content().contains( "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> ." ) );
      assertTrue( result.content().contains( "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> ." ) );
      assertTrue( result.content().contains( "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> ." ) );
      assertTrue( result.content().contains( "@prefix : <urn:samm:org.eclipse.esmf.example:1.0.0#> ." ) );
      assertTrue( result.content().contains( ":Movement a samm:Aspect ;" ) );
      assertTrue( result.filename().get().contains( "Movement.ttl" ) );
   }

   @Test
   void testCheckElementExists() throws ModelResolutionException {
      assertTrue( aspectModelReader.checkElementExists( NAMESPACE.withName( TEST_MODEL_FOR_SERVICE ), "Example.ttl" ) );
      assertFalse( aspectModelReader.checkElementExists( NAMESPACE.withName( TEST_MODEL_FOR_SERVICE ), "Movement.ttl" ) );
   }

   @Test
   void testGetModelThrowsNotFoundException() {
      assertThrows( FileNotFoundException.class,
            () -> aspectModelReader.getModel( NAMESPACE.withName( TEST_MODEL_NOT_FOUND ), TEST_FILEPATH ) );
   }
}

