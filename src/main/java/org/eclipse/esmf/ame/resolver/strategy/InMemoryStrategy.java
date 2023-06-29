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

import static org.apache.jena.http.auth.AuthEnv.LOG;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RiotException;
import org.eclipse.esmf.aspectmodel.resolver.AspectModelResolver;
import org.eclipse.esmf.aspectmodel.urn.AspectModelUrn;

import io.vavr.control.Try;

public class InMemoryStrategy extends ResolutionStrategy {
   public final FileSystem fileSystem;

   public InMemoryStrategy( final String aspectModel, final Path processingRootPath, final FileSystem fileSystem )
         throws RiotException {
      super( aspectModel, processingRootPath );
      this.fileSystem = fileSystem;
   }

   protected Try<Model> getModelFromFileSystem( final AspectModelUrn aspectModelUrn, final Path rootPath ) {
      try ( Stream<Path> pathStream = Files.walk( rootPath ) ) {
         final String filePath =
               aspectModelUrn.getNamespace() + File.separator + aspectModelUrn.getVersion() + File.separator
                     + aspectModelUrn.getName() + ".ttl";

         final Path file = fileSystem.getPath( filePath );

         if ( Files.exists( file ) ) {
            return Try.of( () -> loadTurtleFromString( Files.readString( file ) ) );
         }

         LOG.warn( "Looking for {}, but no {}.ttl was found. Inspecting files in {}", aspectModelUrn.getName(),
               aspectModelUrn.getName(), filePath );

         Optional<Try<Model>> modelWithDefinition = pathStream
               .filter( Files::isRegularFile )
               .map( Path::toAbsolutePath )
               .map( Path::toString )
               .map( fileSystem::getPath )
               .map( aspectModelPath -> Try.of( () -> loadTurtleFromString( Files.readString( aspectModelPath ) ) ) )
               .filter( tryModel -> tryModel.map(
                                                  model -> AspectModelResolver.containsDefinition( model, aspectModelUrn ) )
                                            .getOrElse( false ) )
               .findFirst();

         return modelWithDefinition.orElse( Try.failure( new FileNotFoundException(
               "No model file containing " + aspectModelUrn + " could be found in directory" ) ) );
      } catch ( IOException exception ) {
         return Try.failure( exception );
      }
   }
}
