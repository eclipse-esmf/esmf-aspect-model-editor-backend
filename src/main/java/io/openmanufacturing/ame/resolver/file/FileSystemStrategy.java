/*
 * Copyright (c) 2022 Robert Bosch Manufacturing Solutions GmbH
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

package io.openmanufacturing.ame.resolver.file;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;

import org.apache.jena.rdf.model.Model;

import io.openmanufacturing.sds.aspectmodel.resolver.AspectModelResolver;
import io.openmanufacturing.sds.aspectmodel.resolver.services.TurtleLoader;
import io.openmanufacturing.sds.aspectmodel.urn.AspectModelUrn;
import io.vavr.control.Try;

public class FileSystemStrategy {
   private final Path modelsRoot;

   public FileSystemStrategy( final Path modelsRoot ) {
      this.modelsRoot = modelsRoot;
   }

   /**
    * Returns the {@link Model} that corresponds to the given model URN
    *
    * @param aspectModelUrn The model URN
    * @return The file that defines the supplied aspectModelUrn.
    */
   public String getAspectModelFile( final AspectModelUrn aspectModelUrn ) {
      final Path directory = modelsRoot.resolve( aspectModelUrn.getNamespace() ).resolve( aspectModelUrn.getVersion() );

      return Arrays.stream( Optional.ofNullable( directory.toFile().listFiles() ).orElse( new File[] {} ) )
                   .filter( File::isFile )
                   .filter( file -> file.getName().endsWith( ".ttl" ) )
                   .map( File::toURI )
                   .sorted()
                   .filter( uri -> AspectModelResolver.containsDefinition( loadFromUri( uri ).get(), aspectModelUrn ) )
                   .map( URI::getPath )
                   .findFirst()
                   .orElse( String.format(
                         "File with the content of URN: %s could not be found.",
                         aspectModelUrn ) );
   }

   /**
    * Loads an Aspect model from a resolveable URI
    *
    * @param uri The URI
    * @return The model
    */
   protected Try<Model> loadFromUri( final URI uri ) {
      try {
         return loadFromUrl( uri.toURL() );
      } catch ( final MalformedURLException exception ) {
         return Try.failure( exception );
      }
   }

   /**
    * Loads an Aspect model from a resolveable URL
    *
    * @param url The URL
    * @return The model
    */
   protected Try<Model> loadFromUrl( final URL url ) {
      return Try.ofSupplier( () -> TurtleLoader.openUrl( url ) ).flatMap( TurtleLoader::loadTurtle );
   }
}
