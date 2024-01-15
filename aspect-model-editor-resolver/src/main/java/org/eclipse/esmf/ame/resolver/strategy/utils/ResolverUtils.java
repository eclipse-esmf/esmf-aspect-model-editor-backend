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

package org.eclipse.esmf.ame.resolver.strategy.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.jena.rdf.model.Model;
import org.eclipse.esmf.ame.exceptions.InvalidAspectModelException;
import org.eclipse.esmf.ame.resolver.strategy.FileSystemStrategy;
import org.eclipse.esmf.ame.resolver.strategy.InMemoryStrategy;
import org.eclipse.esmf.ame.utils.ModelUtils;
import org.eclipse.esmf.aspectmodel.resolver.AspectModelResolver;
import org.eclipse.esmf.aspectmodel.resolver.services.SammAspectMetaModelResourceResolver;
import org.eclipse.esmf.aspectmodel.resolver.services.TurtleLoader;
import org.eclipse.esmf.aspectmodel.resolver.services.VersionedModel;
import org.eclipse.esmf.aspectmodel.serializer.PrettyPrinter;
import org.eclipse.esmf.aspectmodel.urn.AspectModelUrn;
import org.eclipse.esmf.metamodel.Aspect;
import org.eclipse.esmf.metamodel.AspectContext;
import org.eclipse.esmf.metamodel.loader.AspectModelLoader;

import io.vavr.control.Try;

public class ResolverUtils {
   /**
    * Reads the content of a file located at the specified path using the provided character encoding.
    *
    * @param path The path to the file to be read.
    * @param charset The character encoding to be used for decoding the file content.
    * @return The content of the file as a string decoded with the specified character encoding.
    *
    * @throws IOException If an I/O error occurs while reading the file.
    */
   public static String readString( Path path, Charset charset ) throws IOException {
      try ( InputStream inputStream = Files.newInputStream( path ) ) {
         byte[] bytes = inputStream.readAllBytes();
         return new String( bytes, charset );
      }
   }

   /**
    * Method to resolve a given AspectModelUrn using a suitable ResolutionStrategy.
    *
    * @param fileSystemStrategy strategy of the backend.
    * @return The resolved model on success.
    */
   public static Try<VersionedModel> fetchVersionModel( final FileSystemStrategy fileSystemStrategy ) {
      return new AspectModelResolver().resolveAspectModel( fileSystemStrategy, fileSystemStrategy.getAspectModelUrn() );
   }

   public static Try<VersionedModel> fetchVersionModel( final InMemoryStrategy inMemoryStrategy ) {
      return new AspectModelResolver().resolveAspectModel( inMemoryStrategy, inMemoryStrategy.getAspectModelUrn() );
   }

   /**
    * Load Aspect Model from storage path.
    *
    * @param fileSystemStrategy for the given storage path.
    * @return the resulting {@link VersionedModel} that corresponds to the input Aspect model.
    */
   public static VersionedModel loadModelFromStoragePath( final FileSystemStrategy fileSystemStrategy ) {
      return resolveModel( fileSystemStrategy.getCurrentAspectModel() ).getOrElseThrow(
            e -> new InvalidAspectModelException( "Cannot resolve Aspect Model.", e ) );
   }

   /**
    * Loading the Aspect Model from input file.
    *
    * @param file Aspect Model as a file.
    * @return the resulting {@link VersionedModel} that corresponds to the input Aspect model.
    */
   public static Try<VersionedModel> loadModelFromFile( final File file ) {
      try ( final InputStream inputStream = new FileInputStream( file ) ) {
         return TurtleLoader.loadTurtle( inputStream ).flatMap( ResolverUtils::resolveModel );
      } catch ( final IOException exception ) {
         return Try.failure( exception );
      }
   }

   private static Try<VersionedModel> resolveModel( final Model model ) {
      final SammAspectMetaModelResourceResolver resourceResolver = new SammAspectMetaModelResourceResolver();

      return resourceResolver.getMetaModelVersion( model ).flatMap(
            metaModelVersion -> resourceResolver.mergeMetaModelIntoRawModel( model, metaModelVersion ) );
   }

   /**
    * Creates a pretty-printed string representation of the provided versioned model.
    * This method formats the versioned model into a more readable and structured text format,
    * making it easier to understand and interpret.
    *
    * @param versionedModel The versioned model to be pretty-printed.
    * @param urn The URI of the aspect related to the model.
    * @return A string that represents the pretty-printed version of the versioned aspect model.
    */
   public static String getPrettyPrintedVersionedModel( final VersionedModel versionedModel, final URI urn ) {
      final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
      final PrintWriter writer = new PrintWriter( buffer );
      new PrettyPrinter( versionedModel, AspectModelUrn.fromUrn( urn ), writer ).print();
      writer.flush();
      return buffer.toString();
   }

   /**
    * Generates a pretty-printed string representation of the aspect model.
    * This method first resolves the versioned model from the given aspect model string
    * using a FileSystemStrategy and then pretty-prints it.
    *
    * @param aspectModel The string representation of the aspect model.
    * @return A pretty-printed string of the resolved and formatted versioned aspect model.
    */
   public static String getPrettyPrintedModel( final String aspectModel ) {
      final FileSystemStrategy fileSystemStrategy = new FileSystemStrategy( aspectModel );
      final VersionedModel versionedModel = ResolverUtils.loadModelFromStoragePath( fileSystemStrategy );

      return getPrettyPrintedVersionedModel( versionedModel, fileSystemStrategy.getAspectModelUrn().getUrn() );
   }

   /**
    * Creates an Aspect instance from an Aspect Model.
    *
    * @param aspectModel as a string.
    * @return the Aspect as an object.
    */
   public static Aspect resolveAspectFromModel( final String aspectModel ) throws InvalidAspectModelException {
      final FileSystemStrategy fileSystemStrategy = new FileSystemStrategy( aspectModel );
      final Try<VersionedModel> versionedModels = ResolverUtils.fetchVersionModel( fileSystemStrategy );

      final Try<AspectContext> context = versionedModels.flatMap(
            model -> resolveSingleAspect( fileSystemStrategy, model ) );

      return ModelUtils.getAspectContext( context ).aspect();
   }

   /**
    * Retrieves a single AspectContext based on the given FileSystemStrategy and VersionedModel.
    *
    * @param fileSystemStrategy The file system strategy to retrieve the AspectModel URN.
    * @param model The versioned model to search for the aspect.
    * @return A Try containing the AspectContext if found, otherwise a failure.
    */
   public static Try<AspectContext> resolveSingleAspect( final FileSystemStrategy fileSystemStrategy,
         final VersionedModel model ) {
      return AspectModelLoader.getSingleAspect( model,
                                    aspect -> aspect.getName().equals( fileSystemStrategy.getAspectModelUrn().getName() ) )
                              .map( aspect -> new AspectContext( model, aspect ) );
   }
}
