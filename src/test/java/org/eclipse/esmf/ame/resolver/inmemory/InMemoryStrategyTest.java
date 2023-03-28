/*
 * Copyright (c) 2023 Robert Bosch Manufacturing Solutions GmbH
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

package org.eclipse.esmf.ame.resolver.inmemory;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.jena.rdf.model.Model;
import org.eclipse.esmf.ame.model.ValidationProcess;
import org.eclipse.esmf.aspectmodel.urn.AspectModelUrn;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import io.vavr.control.Try;

@ExtendWith( SpringExtension.class )
class InMemoryStrategyTest {
   private static final Path resourcesPath = Path.of( "src", "test", "resources" );
   private static final Path eclipseTestPath = Path.of( resourcesPath.toString(), "org.eclipse.esmf.example",
         "1.0.0" );
   private static final String aspectModelFile = "AspectModel.ttl";
   private static final String aspectModelFileWithRef = "AspectModelWithExternalRef.ttl";
   private ValidationProcess validationProcess;

   @BeforeEach
   void setUp() {
      validationProcess = Mockito.mock( ValidationProcess.class );
      Mockito.when( validationProcess.getPath() ).thenReturn( resourcesPath );
   }

   @Test
   void testApplySuccess() throws IOException {
      final String fileToTest = Files.readString( eclipseTestPath.resolve( aspectModelFile ),
            StandardCharsets.UTF_8 );

      final InMemoryStrategy inMemoryStrategy = new InMemoryStrategy( fileToTest, validationProcess );

      final Try<Model> apply = inMemoryStrategy.apply(
            AspectModelUrn.fromUrn( "urn:samm:org.eclipse.esmf.example:1.0.0#AspectModel" ) );

      assertTrue( apply.isSuccess() );
   }

   @Test
   void testApplyFailureNullAspectModelUrn() throws IOException {
      final String fileToTest = Files.readString( eclipseTestPath.resolve( aspectModelFile ),
            StandardCharsets.UTF_8 );

      final InMemoryStrategy inMemoryStrategy = new InMemoryStrategy( fileToTest, validationProcess );
      final Try<Model> result = inMemoryStrategy.apply( null );

      assertTrue( result.isFailure() );
   }

   @Test
   void testApplyFailure() throws IOException {
      final String fileToTest = Files.readString( eclipseTestPath.resolve( aspectModelFileWithRef ),
            StandardCharsets.UTF_8 );

      final InMemoryStrategy inMemoryStrategy = new InMemoryStrategy( fileToTest, validationProcess );
      final Try<Model> result = inMemoryStrategy.apply( null );

      assertTrue( result.isFailure() );
   }
}
