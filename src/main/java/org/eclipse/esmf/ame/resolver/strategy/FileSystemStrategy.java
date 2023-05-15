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
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RiotException;
import org.eclipse.esmf.ame.model.ProcessPath;
import org.eclipse.esmf.aspectmodel.resolver.AspectModelResolver;
import org.eclipse.esmf.aspectmodel.urn.AspectModelUrn;

import io.vavr.control.Try;

public class FileSystemStrategy extends resolutionStrategy {
   public FileSystemStrategy( final String aspectModel ) throws RiotException {
      super(aspectModel, ProcessPath.MODELS.getPath());
   }

   protected Try<Model> getModelFromFileSystem( final AspectModelUrn aspectModelUrn, final Path rootPath ) {
      final Path directory = rootPath.resolve( aspectModelUrn.getNamespace() ).resolve( aspectModelUrn.getVersion() );

      final File namedResourceFile = directory.resolve( aspectModelUrn.getName() + ".ttl" ).toFile();
      if ( namedResourceFile.exists() ) {
         return loadTurtleFromFile( new File( namedResourceFile.toURI() ) );
      }

      LOG.warn( "Looking for {}, but no {}.ttl was found. Inspecting files in {}", aspectModelUrn.getName(),
            aspectModelUrn.getName(), directory );

      return Arrays.stream(Optional.ofNullable(directory.toFile().listFiles()).orElse(new File[]{}))
              .filter(file -> file.isFile() && file.getName().endsWith(".ttl"))
              .map(File::toURI)
              .sorted()
              .map(this::loadFromUri)
              .filter(tryModel -> tryModel.map(model -> AspectModelResolver.containsDefinition(model, aspectModelUrn)).getOrElse(false))
              .findFirst()
              .orElse(Try.failure(new FileNotFoundException("No model file containing " + aspectModelUrn + " could be found in directory: " + directory)));
   }
}
