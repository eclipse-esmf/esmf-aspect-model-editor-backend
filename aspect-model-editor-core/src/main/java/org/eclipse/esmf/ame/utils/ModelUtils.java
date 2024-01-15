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
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.Model;
import org.eclipse.esmf.ame.config.ApplicationSettings;
import org.eclipse.esmf.ame.exceptions.FileHandlingException;
import org.eclipse.esmf.ame.exceptions.FileReadException;
import org.eclipse.esmf.aspectmodel.resolver.AspectModelResolver;
import org.eclipse.esmf.aspectmodel.resolver.services.TurtleLoader;
import org.eclipse.esmf.aspectmodel.urn.AspectModelUrn;
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
         throw new FileReadException( throwable.getMessage() );
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

   /**
    * Sanitizes the file name to remove any path information and retain only the base file name.
    * This method is used to ensure that the file name does not contain any directory path components,
    * which helps prevent path traversal attacks. It extracts only the file name portion from a given
    * string that may represent a path.
    *
    * @param fileInformation The file name string potentially including path information.
    * @return The sanitized base file name without any path components.
    *
    * @throws FileHandlingException If the file contains path information´s.
    */
   public static String sanitizeFileInformation( String fileInformation ) {
      if ( fileInformation.contains( File.separator ) || fileInformation.contains( ".." ) ) {
         throw new FileHandlingException(
               "Invalid file information: The provided string must not contain directory separators or relative path components." );
      }

      return new File( fileInformation ).getName();
   }
}
