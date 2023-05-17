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

import com.github.marschall.memoryfilesystem.MemoryFileSystemBuilder;

import io.vavr.NotImplementedError;
import io.vavr.control.Try;

import org.apache.jena.rdf.model.Model;
import org.eclipse.esmf.ame.model.ProcessPath;
import org.eclipse.esmf.aspectmodel.urn.AspectModelUrn;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith( SpringExtension.class )
class InMemoryStrategyTest {

   private static final Path resourcesPath = Path.of( "src", "test", "resources", "strategy" );
   private static final Path eclipseTestPath = Path.of( resourcesPath.toString(), "org.eclipse.esmf.example", "1.0.0" );

   private static final String aspectModelFile = "AspectModelForStrategy.ttl";
   private static final String aspectModelurn = "urn:samm:org.eclipse.esmf.example:1.0.0#AspectModelForStrategy";

   private static final String causeMessage = "AspectModelUrn is not set";

   private static Path rootPath;
   private static FileSystem fileSystem;
   ;

   @BeforeEach
   void setUp() throws IOException {
      fileSystem = MemoryFileSystemBuilder.newEmpty().build();
      rootPath = fileSystem.getRootDirectories().iterator().next();
   }

   @Test
   void testApplySuccess() throws IOException {
      final String fileToTest = Files.readString( eclipseTestPath.resolve( aspectModelFile ),
            StandardCharsets.UTF_8 );

      final InMemoryStrategy inMemoryStrategy = new InMemoryStrategy( fileToTest, rootPath, fileSystem );
      final Try<Model> apply = inMemoryStrategy.apply( AspectModelUrn.fromUrn( aspectModelurn ) );

      assertTrue( apply.isSuccess() );
   }

   @Test
   void testApplyFailureNullAndFailureAspectModelUrn() throws IOException {
      final String fileToTest = Files.readString( eclipseTestPath.resolve( aspectModelFile ),
            StandardCharsets.UTF_8 );

      final InMemoryStrategy inMemoryStrategy = new InMemoryStrategy( fileToTest, rootPath, fileSystem );
      final Try<Model> result = inMemoryStrategy.apply( null );

      assertTrue( result.isFailure() );
      assertTrue( result.getCause() instanceof NotImplementedError );
      assertTrue( result.getCause().getMessage().equals( causeMessage ) );
   }
}
