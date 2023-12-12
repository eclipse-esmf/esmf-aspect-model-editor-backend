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

package org.eclipse.esmf.ame.utils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.Model;
import org.eclipse.esmf.ame.config.ApplicationSettings;
import org.eclipse.esmf.ame.exceptions.FileReadException;
import org.eclipse.esmf.aspectmodel.resolver.AspectModelResolver;
import org.eclipse.esmf.aspectmodel.resolver.services.TurtleLoader;
import org.eclipse.esmf.aspectmodel.shacl.violation.Violation;
import org.eclipse.esmf.aspectmodel.urn.AspectModelUrn;
import org.eclipse.esmf.aspectmodel.validation.services.AspectModelValidator;
import org.eclipse.esmf.aspectmodel.validation.services.ViolationFormatter;
import org.eclipse.esmf.metamodel.AspectContext;

import io.vavr.control.Try;

public class ModelUtils {

   private ModelUtils() {
   }

   public static final String TTL = "ttl";
   public static final String TTL_EXTENSION = "." + TTL;

   /**
    * Retrieves the AspectContext from the provided Try<AspectContext>, handling exceptions if necessary.
    *
    * @param context The Try<AspectContext> representing the context to retrieve the AspectContext from.
    * @return The retrieved AspectContext.
    *
    * @throws FileReadException If there are failures in the generation process due to violations in the model.
    */
   public static AspectContext getAspectContext( Try<AspectContext> context ) {
      return context.recover( throwable -> {
         // Another exception, e.g. syntax error. Let the validator handle this
         final List<Violation> violations = new AspectModelValidator().validateModel(
               context.map( AspectContext::rdfModel ) );

         throw new FileReadException(
               String.format( "The generation process encountered failures due to the following violations: %s",
                     new ViolationFormatter().apply( violations ) ) );
      } ).get();
   }

   /**
    * Returns the {@link Model} that corresponds to the given model URN
    *
    * @param aspectModelUrn The model URN
    * @return The file that defines the supplied aspectModelUrn.
    */
   public static String getAspectModelFile( final String modelsRootPath, final AspectModelUrn aspectModelUrn ) {
      if ( aspectModelUrn == null ) {
         return StringUtils.EMPTY;
      }

      final Path directory = Path.of( modelsRootPath ).resolve( aspectModelUrn.getNamespace() )
                                 .resolve( aspectModelUrn.getVersion() );

      final String fileInformation = Arrays.stream(
                                                 Optional.ofNullable( directory.toFile().listFiles() ).orElse( new File[] {} ) ).filter( File::isFile )
                                           .filter( file -> file.getName().endsWith( ".ttl" ) ).map( File::toURI )
                                           .sorted().filter(
                  uri -> AspectModelResolver.containsDefinition( loadFromUri( uri ).get(), aspectModelUrn ) )
                                           .map( URI::getPath ).findFirst().orElse( "NO CORRESPONDING FILE FOUND" );

      final File filePath = new File( fileInformation );

      if ( !filePath.exists() ) {
         return fileInformation;
      }

      return filePath.getPath().replace( ApplicationSettings.getMetaModelStoragePath() + File.separator, "" );
   }

   /**
    * Loads an Aspect model from a resolvable URI
    *
    * @param uri The URI
    * @return The model
    */
   private static Try<Model> loadFromUri( final URI uri ) {
      try {
         return loadFromUrl( uri.toURL() );
      } catch ( final MalformedURLException exception ) {
         return Try.failure( exception );
      }
   }

   /**
    * Loads an Aspect model from a resolvable URL
    *
    * @param url The URL
    * @return The model
    */
   private static Try<Model> loadFromUrl( final URL url ) {
      return Try.ofSupplier( () -> TurtleLoader.openUrl( url ) ).flatMap( TurtleLoader::loadTurtle );
   }
}