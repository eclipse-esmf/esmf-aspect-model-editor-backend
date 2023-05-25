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

package org.eclipse.esmf.ame.services.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RiotException;
import org.eclipse.esmf.ame.config.ApplicationSettings;
import org.eclipse.esmf.ame.exceptions.FileReadException;
import org.eclipse.esmf.ame.exceptions.InvalidAspectModelException;
import org.eclipse.esmf.ame.model.validation.ViolationReport;
import org.eclipse.esmf.ame.resolver.strategy.FileSystemStrategy;
import org.eclipse.esmf.ame.resolver.strategy.InMemoryStrategy;
import org.eclipse.esmf.ame.validation.ViolationFormatter;
import org.eclipse.esmf.aspectmodel.resolver.AspectModelResolver;
import org.eclipse.esmf.aspectmodel.resolver.services.SammAspectMetaModelResourceResolver;
import org.eclipse.esmf.aspectmodel.resolver.services.TurtleLoader;
import org.eclipse.esmf.aspectmodel.resolver.services.VersionedModel;
import org.eclipse.esmf.aspectmodel.serializer.PrettyPrinter;
import org.eclipse.esmf.aspectmodel.shacl.violation.InvalidSyntaxViolation;
import org.eclipse.esmf.aspectmodel.shacl.violation.ProcessingViolation;
import org.eclipse.esmf.aspectmodel.shacl.violation.Violation;
import org.eclipse.esmf.aspectmodel.urn.AspectModelUrn;
import org.eclipse.esmf.aspectmodel.validation.services.AspectModelValidator;
import org.eclipse.esmf.aspectmodel.versionupdate.MigratorService;
import org.eclipse.esmf.metamodel.Aspect;
import org.eclipse.esmf.metamodel.AspectContext;
import org.eclipse.esmf.metamodel.loader.AspectModelLoader;

import io.vavr.control.Try;

public class ModelUtils {

   private ModelUtils() {
   }

   public static final String TTL = "ttl";
   public static final String TTL_EXTENSION = "." + TTL;

   /**
    * This Method is used to create a pretty printed string of the versioned model
    *
    * @param versionedModel The Versioned Model
    * @param urn The urn of the Aspect
    * @return Pretty printed string of the Versioned Aspect Model.
    */
   public static String getPrettyPrintedVersionedModel( final VersionedModel versionedModel, final URI urn ) {
      final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
      final PrintWriter writer = new PrintWriter( buffer );
      new PrettyPrinter( versionedModel, AspectModelUrn.fromUrn( urn ), writer ).print();
      writer.flush();
      return buffer.toString();
   }

   public static String getPrettyPrintedModel( final String aspectModel ) {
      final FileSystemStrategy fileSystemStrategy = new FileSystemStrategy( aspectModel );
      final VersionedModel versionedModel = ModelUtils.loadModelFromStoragePath( fileSystemStrategy );

      return getPrettyPrintedVersionedModel( versionedModel, fileSystemStrategy.getAspectModelUrn().getUrn() );
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
    * Migrates a model to its latest version.
    *
    * @param aspectModel as a string.
    * @return migrated Aspect Model as a string.
    */
   public static String migrateModel( final String aspectModel ) throws InvalidAspectModelException {
      final FileSystemStrategy fileSystemStrategy = new FileSystemStrategy( aspectModel );

      final Try<VersionedModel> migratedFile = new MigratorService().updateMetaModelVersion(
            loadModelFromStoragePath( fileSystemStrategy ) );

      final VersionedModel versionedModel = migratedFile.getOrElseThrow(
            error -> new InvalidAspectModelException( "Aspect Model cannot be migrated.", error ) );

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
      final Try<VersionedModel> versionedModels = ModelUtils.fetchVersionModel( fileSystemStrategy );

      final Try<AspectContext> context = versionedModels.flatMap(
            model -> getSingleAspect( fileSystemStrategy, model ) );

      return getAspectContext( context ).aspect();
   }

   /**
    * Retrieves a single AspectContext based on the given FileSystemStrategy and VersionedModel.
    *
    * @param fileSystemStrategy The file system strategy to retrieve the AspectModel URN.
    * @param model The versioned model to search for the aspect.
    * @return A Try containing the AspectContext if found, otherwise a failure.
    */
   public static Try<AspectContext> getSingleAspect( final FileSystemStrategy fileSystemStrategy,
         final VersionedModel model ) {
      return AspectModelLoader.getSingleAspect( model,
                                    aspect -> aspect.getName().equals( fileSystemStrategy.getAspectModelUrn().getName() ) )
                              .map( aspect -> new AspectContext( model, aspect ) );
   }

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
    * Load Aspect Model from storage path.
    *
    * @param fileSystemStrategy for the given storage path.
    * @return the resulting {@link VersionedModel} that corresponds to the input Aspect model.
    */
   public static VersionedModel loadModelFromStoragePath( final FileSystemStrategy fileSystemStrategy ) {
      return resolveModel( fileSystemStrategy.aspectModel ).getOrElseThrow(
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
         return TurtleLoader.loadTurtle( inputStream ).flatMap( ModelUtils::resolveModel );
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
    * Validates an Aspect Model that is provided as a Try of a VersionedModel that can contain either a syntactically
    * valid (but semantically invalid) Aspect model, or a RiotException if a parser error occured.
    *
    * @param aspectModel as a string.
    * @param aspectModelValidator Aspect Model Validator from esmf-sdk
    * @return Either a ValidationReport.ValidReport if the model is syntactically correct and conforms to the Aspect
    *       Meta Model semantics or a ValidationReport.InvalidReport that provides a number of ValidationErrors that
    *       describe all validation violations.
    */
   public static ViolationReport validateModel( final String aspectModel,
         final AspectModelValidator aspectModelValidator ) {
      final ViolationReport violationReport = new ViolationReport();

      try {
         final FileSystemStrategy fileSystemStrategy = new FileSystemStrategy( aspectModel );
         final Try<VersionedModel> versionedModel = ModelUtils.fetchVersionModel( fileSystemStrategy );
         final List<Violation> violations = aspectModelValidator.validateModel( versionedModel );

         violationReport.setViolationErrors( new ViolationFormatter().apply( violations ) );

         return violationReport;
      } catch ( final RiotException riotException ) {
         violationReport.addViolation(
               new ViolationFormatter().visitInvalidSyntaxViolation( riotException.getMessage() ) );

         return violationReport;
      }
   }

   public static ViolationReport validateModelInMemoryFiles( final String aspectModel,
         final AspectModelValidator aspectModelValidator, final java.nio.file.FileSystem fileSystem ) {
      final Path root = fileSystem.getRootDirectories().iterator().next();
      final ViolationReport violationReport = new ViolationReport();

      try {
         final InMemoryStrategy inMemoryStrategy = new InMemoryStrategy( aspectModel, root, fileSystem );
         final Try<VersionedModel> versionedModel = ModelUtils.fetchVersionModel( inMemoryStrategy );
         final List<Violation> violations = aspectModelValidator.validateModel( versionedModel );

         violationReport.setViolationErrors( new ViolationFormatter().apply( violations ) );

         return violationReport;
      } catch ( final RiotException riotException ) {
         violationReport.addViolation(
               new ViolationFormatter().visitInvalidSyntaxViolation( riotException.getMessage() ) );

         return violationReport;
      }
   }

   public static Predicate<Violation> isInvalidSyntaxViolation() {
      return violation -> violation.errorCode() != null && violation.errorCode()
                                                                    .equals( InvalidSyntaxViolation.ERROR_CODE );
   }

   public static Predicate<Violation> isProcessingViolation() {
      return violation -> violation.errorCode() != null && violation.errorCode()
                                                                    .equals( ProcessingViolation.ERROR_CODE );
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
