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

package io.openmanufacturing.ame.services.utils;

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
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RiotException;

import io.openmanufacturing.ame.config.ApplicationSettings;
import io.openmanufacturing.ame.exceptions.FileNotFoundException;
import io.openmanufacturing.ame.exceptions.InvalidAspectModelException;
import io.openmanufacturing.ame.model.ValidationProcess;
import io.openmanufacturing.ame.model.validation.ViolationReport;
import io.openmanufacturing.ame.repository.strategy.utils.LocalFolderResolverUtils;
import io.openmanufacturing.ame.resolver.inmemory.InMemoryStrategy;
import io.openmanufacturing.ame.validation.ViolationFormatter;
import io.openmanufacturing.sds.aspectmodel.resolver.AspectModelResolver;
import io.openmanufacturing.sds.aspectmodel.resolver.services.SdsAspectMetaModelResourceResolver;
import io.openmanufacturing.sds.aspectmodel.resolver.services.TurtleLoader;
import io.openmanufacturing.sds.aspectmodel.resolver.services.VersionedModel;
import io.openmanufacturing.sds.aspectmodel.serializer.PrettyPrinter;
import io.openmanufacturing.sds.aspectmodel.shacl.violation.InvalidSyntaxViolation;
import io.openmanufacturing.sds.aspectmodel.shacl.violation.ProcessingViolation;
import io.openmanufacturing.sds.aspectmodel.shacl.violation.Violation;
import io.openmanufacturing.sds.aspectmodel.urn.AspectModelUrn;
import io.openmanufacturing.sds.aspectmodel.validation.services.AspectModelValidator;
import io.openmanufacturing.sds.aspectmodel.versionupdate.MigratorService;
import io.openmanufacturing.sds.metamodel.Aspect;
import io.openmanufacturing.sds.metamodel.loader.AspectModelLoader;
import io.vavr.control.Try;

public class ModelUtils {

   private ModelUtils() {
   }

   public static final String TTL = "ttl";
   public static final String TTL_EXTENSION = "." + TTL;

   /**
    * /**
    * This Method is used to create an in memory strategy for the given Aspect Model.
    *
    * @param aspectModel as a string
    * @return in memory for the given storage path.
    */
   public static InMemoryStrategy inMemoryStrategy( final String aspectModel,
         final ValidationProcess validationProcess ) throws RiotException {
      return new InMemoryStrategy( aspectModel, validationProcess );
   }

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

   /**
    * Method to resolve a given AspectModelUrn using a suitable ResolutionStrategy.
    *
    * @param inMemoryStrategy strategy of the backend.
    * @return The resolved model on success.
    */
   public static Try<VersionedModel> fetchVersionModel( final InMemoryStrategy inMemoryStrategy ) {
      return new AspectModelResolver().resolveAspectModel( inMemoryStrategy, inMemoryStrategy.getAspectModelUrn() );
   }

   /**
    * Migrates a model to its latest version.
    *
    * @param aspectModel as a string.
    * @return migrated Aspect Model as a string.
    */
   public static String migrateModel( final String aspectModel, final ValidationProcess validationProcess )
         throws InvalidAspectModelException {
      final InMemoryStrategy inMemoryStrategy = ModelUtils.inMemoryStrategy( aspectModel, validationProcess );
      final Try<VersionedModel> migratedFile = loadModelFromStoragePath( inMemoryStrategy ).flatMap(
            new MigratorService()::updateMetaModelVersion );

      final VersionedModel versionedModel = migratedFile.getOrElseThrow(
            error -> new InvalidAspectModelException( "Aspect Model cannot be migrated.", error ) );

      return getPrettyPrintedVersionedModel( versionedModel, inMemoryStrategy.getAspectModelUrn().getUrn() );
   }

   /**
    * Creates an Aspect instance from an Aspect Model.
    *
    * @param aspectModel as a string.
    * @return the Aspect as an object.
    */
   public static Aspect resolveAspectFromModel( final String aspectModel, final ValidationProcess validationProcess )
         throws InvalidAspectModelException {
      final InMemoryStrategy inMemoryStrategy = ModelUtils.inMemoryStrategy( aspectModel,
            validationProcess );

      final VersionedModel versionedModel = ModelUtils.loadModelFromStoragePath( inMemoryStrategy ).getOrElseThrow(
            e -> new InvalidAspectModelException( "Cannot resolve Aspect Model.", e ) );

      return AspectModelLoader.getSingleAspectUnchecked( versionedModel );
   }

   /**
    * Load Aspect Model from storage path.
    *
    * @param inMemoryStrategy for the given storage path.
    * @return the resulting {@link VersionedModel} that corresponds to the input Aspect model.
    */
   public static Try<VersionedModel> loadModelFromStoragePath( final InMemoryStrategy inMemoryStrategy ) {
      return resolveModel( inMemoryStrategy.aspectModel );
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
      final SdsAspectMetaModelResourceResolver metaModelResourceResolver = new SdsAspectMetaModelResourceResolver();

      return metaModelResourceResolver.getBammVersion( model ).flatMap(
            metaModelVersion -> metaModelResourceResolver.mergeMetaModelIntoRawModel( model, metaModelVersion ) );
   }

   /**
    * Validates an Aspect Model that is provided as a Try of a VersionedModel that can contain either a syntactically
    * valid (but semantically invalid) Aspect model, or a RiotException if a parser error occured.
    *
    * @param aspectModel as a string.
    * @param aspectModelValidator Aspect Model Validator from sds-sdk
    * @param validationProcess Validation Process
    * @return Either a ValidationReport.ValidReport if the model is syntactically correct and conforms to the Aspect
    *       Meta Model semantics or a ValidationReport.InvalidReport that provides a number of ValidationErrors that
    *       describe all validation violations.
    */
   public static ViolationReport validateModel( final String aspectModel,
         final AspectModelValidator aspectModelValidator, final ValidationProcess validationProcess ) {
      final ViolationReport violationReport = new ViolationReport();

      try {
         final InMemoryStrategy inMemoryStrategy = inMemoryStrategy( aspectModel, validationProcess );
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

   public static List<String> copyAspectModelToDirectory( final List<String> aspectModelFiles,
         final String sourceStorage, final String destStorage ) {
      return aspectModelFiles.stream().map( file -> {
         final LocalFolderResolverUtils.FolderStructure folderStructure = LocalFolderResolverUtils.extractFilePath(
               file );
         final String absoluteAspectModelPath = sourceStorage + File.separator + folderStructure.toString();
         final File aspectModelStoragePath = Paths.get( destStorage, folderStructure.getFileRootPath(),
               folderStructure.getVersion() ).toFile();

         try {
            FileUtils.copyFileToDirectory( new File( absoluteAspectModelPath ), aspectModelStoragePath );
            return folderStructure.toString();
         } catch ( final IOException e ) {
            throw new FileNotFoundException(
                  String.format( "Cannot copy file %s to %s", folderStructure.getFileName(), aspectModelStoragePath ) );
         }
      } ).toList();
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
                                                 Optional.ofNullable( directory.toFile().listFiles() ).orElse( new File[] {} ) )
                                           .filter( File::isFile )
                                           .filter( file -> file.getName().endsWith( ".ttl" ) )
                                           .map( File::toURI )
                                           .sorted()
                                           .filter( uri -> AspectModelResolver.containsDefinition(
                                                 loadFromUri( uri ).get(), aspectModelUrn ) )
                                           .map( URI::getPath )
                                           .findFirst()
                                           .orElse( "NO CORRESPONDING FILE FOUND" );

      final File filePath = new File( fileInformation );

      if ( !filePath.exists() ) {
         return fileInformation;
      }

      return filePath.getPath().replace(
            ApplicationSettings.getMetaModelStoragePath() + File.separator, "" );
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
