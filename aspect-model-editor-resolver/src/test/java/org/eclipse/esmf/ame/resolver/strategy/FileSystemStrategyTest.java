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

package org.eclipse.esmf.ame.resolver.strategy;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.jena.rdf.model.Model;
import org.eclipse.esmf.ame.model.StoragePath;
import org.eclipse.esmf.aspectmodel.urn.AspectModelUrn;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import io.vavr.NotImplementedError;
import io.vavr.control.Try;

@ExtendWith( SpringExtension.class )
class FileSystemStrategyTest {
   private static final Path resourcesPath = Path.of( "src", "test", "resources", "resolver" );
   private static final Path eclipseTestPath = Path.of( resourcesPath.toString(), "org.eclipse.esmf.example", "1.0.0" );

   private static final String aspectModelFile = "AspectModelForResolver.ttl";
   private static final String aspectModelurn = "urn:samm:org.eclipse.esmf.example:1.0.0#AspectModelForResolver";

   private static final String causeMessage = "AspectModelUrn is not set";

   @BeforeEach
   void setUp() {
      StoragePath StoragePaths = Mockito.mock( StoragePath.class );
      Mockito.when( StoragePaths.getPath() ).thenReturn( resourcesPath );
   }

   @Test
   void testApplySuccess() throws IOException {
      final String fileToTest = Files.readString( eclipseTestPath.resolve( aspectModelFile ),
            StandardCharsets.UTF_8 );

      final FileSystemStrategy fileSystemStrategy = new FileSystemStrategy( fileToTest );
      final Try<Model> apply = fileSystemStrategy.apply( AspectModelUrn.fromUrn( aspectModelurn ) );

      assertTrue( apply.isSuccess() );
   }

   @Test
   void testApplyFailureNullAndFailureAspectModelUrn() throws IOException {
      final String fileToTest = Files.readString( eclipseTestPath.resolve( aspectModelFile ),
            StandardCharsets.UTF_8 );

      final FileSystemStrategy fileSystemStrategy = new FileSystemStrategy( fileToTest );
      final Try<Model> result = fileSystemStrategy.apply( null );

      assertTrue( result.isFailure() );
      assertInstanceOf( NotImplementedError.class, result.getCause() );
      assertEquals( causeMessage, result.getCause().getMessage() );
   }
}
