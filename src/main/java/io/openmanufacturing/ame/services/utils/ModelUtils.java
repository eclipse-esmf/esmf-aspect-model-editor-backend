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
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RiotException;

import io.openmanufacturing.ame.config.ApplicationSettings;
import io.openmanufacturing.ame.exceptions.InvalidAspectModelException;
import io.openmanufacturing.ame.exceptions.UrnNotFoundException;
import io.openmanufacturing.ame.resolver.inmemory.InMemoryStrategy;
import io.openmanufacturing.sds.aspectmodel.resolver.AspectModelResolver;
import io.openmanufacturing.sds.aspectmodel.resolver.services.SdsAspectMetaModelResourceResolver;
import io.openmanufacturing.sds.aspectmodel.resolver.services.TurtleLoader;
import io.openmanufacturing.sds.aspectmodel.resolver.services.VersionedModel;
import io.openmanufacturing.sds.aspectmodel.serializer.PrettyPrinter;
import io.openmanufacturing.sds.aspectmodel.urn.AspectModelUrn;
import io.openmanufacturing.sds.aspectmodel.validation.report.ValidationError;
import io.openmanufacturing.sds.aspectmodel.validation.report.ValidationReport;
import io.openmanufacturing.sds.aspectmodel.validation.report.ValidationReportBuilder;
import io.openmanufacturing.sds.aspectmodel.validation.services.AspectModelValidator;
import io.openmanufacturing.sds.aspectmodel.versionupdate.MigratorService;
import io.openmanufacturing.sds.metamodel.Aspect;
import io.openmanufacturing.sds.metamodel.loader.AspectModelLoader;
import io.vavr.control.Try;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class ModelUtils {

   public static final String TTL = "ttl";
   public static final String TTL_EXTENSION = "." + TTL;

   public final static Pattern URN_PATTERN = Pattern.compile(
         "^urn:[a-z0-9][a-z0-9-]{0,31}:([a-z0-9()+,\\-.:=@;$_!*#']|%[0-9a-f]{2})+$", Pattern.CASE_INSENSITIVE );

   /**
    * This Method is used to create an in memory strategy for the given Aspect Model.
    *
    * @param aspectModel as a string
    * @param storagePath path of the folder structure
    * @return in memory for the given storage path.
    */
   public static InMemoryStrategy inMemoryStrategy( final String aspectModel, final String storagePath ) {
      return new InMemoryStrategy( aspectModel, Path.of( storagePath ) );
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
    * @param aspectModel as a string.
    * @param storagePath stored path to the Aspect Models.
    * @return The resolved model on success.
    */
   public static Try<VersionedModel> fetchVersionModel( final String aspectModel, final String storagePath ) {
      final InMemoryStrategy inMemoryStrategy = inMemoryStrategy( aspectModel, storagePath );
      return new AspectModelResolver().resolveAspectModel( inMemoryStrategy, inMemoryStrategy.getAspectModelUrn() );
   }

   /**
    * Migrates a model to its latest version.
    *
    * @param aspectModel as a string.
    * @param storagePath stored path to the Aspect Models.
    * @return migrated Aspect Model as a string.
    */
   public static String migrateModel( final String aspectModel, final String storagePath ) {
      final InMemoryStrategy inMemoryStrategy = ModelUtils.inMemoryStrategy( aspectModel, storagePath );

      final Try<VersionedModel> migratedFile = LoadModelFromStoragePath( inMemoryStrategy ).flatMap(
            new MigratorService()::updateMetaModelVersion );

      final VersionedModel versionedModel = migratedFile.getOrElseThrow(
            e -> new InvalidAspectModelException( "Aspect Model cannot be migrated.", e ) );

      return getPrettyPrintedVersionedModel( versionedModel, inMemoryStrategy.getAspectModelUrn().getUrn() );
   }

   /**
    * Creates an Aspect instance from an Aspect Model.
    *
    * @param aspectModel as a string.
    * @return the Aspect as an object.
    */
   public static Aspect resolveAspectFromModel( final String aspectModel ) {
      final InMemoryStrategy inMemoryStrategy = ModelUtils.inMemoryStrategy( aspectModel,
            ApplicationSettings.getMetaModelStoragePath() );

      final VersionedModel versionedModel = ModelUtils.LoadModelFromStoragePath( inMemoryStrategy ).getOrElseThrow(
            e -> new InvalidAspectModelException( "Cannot resolve Aspect Model.", e ) );

      return AspectModelLoader.fromVersionedModelUnchecked( versionedModel );
   }

   /**
    * Load Aspect Model from storage path.
    *
    * @param inMemoryStrategy for the given storage path.
    * @return the resulting {@link VersionedModel} that corresponds to the input Aspect model.
    */
   public static Try<VersionedModel> LoadModelFromStoragePath( final InMemoryStrategy inMemoryStrategy ) {
      return resolveModel( inMemoryStrategy.model );
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
    * @param storagePath stored path to the Aspect Models.
    * @param aspectModelValidator Aspect Model Validator from sds-sdk
    * @return Either a ValidationReport.ValidReport if the model is syntactically correct and conforms to the Aspect
    *       Meta Model semantics or a ValidationReport.InvalidReport that provides a number of ValidationErrors that
    *       describe all validation violations.
    */
   public static ValidationReport validateModel( final String aspectModel, final String storagePath,
         final AspectModelValidator aspectModelValidator ) {
      try {
         final InMemoryStrategy inMemoryStrategy = inMemoryStrategy( aspectModel, storagePath );
         final Try<VersionedModel> versionedModel = ModelUtils.fetchVersionModel( aspectModel, storagePath );

         if ( versionedModel.isFailure() ) {
            final String message = versionedModel.getCause().toString();

            if ( versionedModel.getCause() instanceof UrnNotFoundException ) {
               final String aspectModelUrn = inMemoryStrategy.getAspectModelUrn().toString();
               final String causedAspectModelUrn = ((UrnNotFoundException) versionedModel.getCause()).getUrn()
                                                                                                     .toString();
               return buildValidationSemanticReport( message, aspectModelUrn, null, null, causedAspectModelUrn );
            }

            return buildValidationSemanticReport( message, null, null, null, null );
         }

         return aspectModelValidator.validate( versionedModel );
      } catch ( final RiotException riotException ) {

         return buildValidationSyntacticReport( riotException );
      } catch ( final IllegalArgumentException illegalArgumentException ) {

         return buildValidationSemanticReport( illegalArgumentException.getMessage(), null, null, null, null );
      }
   }

   private static ValidationReport buildValidationSyntacticReport( final RiotException riotException ) {
      return new ValidationReportBuilder().withValidationErrors(
            List.of( new ValidationError.Syntactic( riotException ) ) ).buildInvalidReport();
   }

   private static ValidationReport buildValidationSemanticReport( final String message, final String focusNode,
         final String resultPath, final String Severity, final String value ) {
      final ValidationError.Semantic semantic = new ValidationError.Semantic( message, focusNode, resultPath, Severity,
            value );
      return new ValidationReportBuilder().withValidationErrors( List.of( semantic ) ).buildInvalidReport();
   }
}
