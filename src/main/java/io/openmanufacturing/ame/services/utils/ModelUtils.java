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

import org.apache.jena.riot.RiotException;

import io.openmanufacturing.ame.exceptions.InvalidAspectModelException;
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
import io.vavr.control.Try;

public class ModelUtils {

   public static final String TTL = "ttl";
   public static final String TTL_EXTENSION = "." + TTL;

   public final static Pattern URN_PATTERN = Pattern.compile(
         "^urn:[a-z0-9][a-z0-9-]{0,31}:([a-z0-9()+,\\-.:=@;$_!*#']|%[0-9a-f]{2})+$",
         Pattern.CASE_INSENSITIVE );

   private ModelUtils() {
   }

   private static InMemoryStrategy inMemoryStrategy( final String aspectModel, final String storagePath ) {
      return new InMemoryStrategy( aspectModel, Path.of( storagePath ) );
   }

   public static Try<VersionedModel> fetchVersionModel( final String aspectModel, final String storagePath ) {
      final InMemoryStrategy inMemoryStrategy = inMemoryStrategy( aspectModel, storagePath );
      return new AspectModelResolver().resolveAspectModel( inMemoryStrategy, inMemoryStrategy.getAspectModelUrn() );
   }

   public static Try<VersionedModel> loadButNotResolveModel( final File inputFile ) {
      try ( final InputStream inputStream = new FileInputStream( inputFile ) ) {
         final SdsAspectMetaModelResourceResolver metaModelResourceResolver = new SdsAspectMetaModelResourceResolver();
         return TurtleLoader.loadTurtle( inputStream ).flatMap( model ->
               metaModelResourceResolver.getBammVersion( model ).flatMap( metaModelVersion ->
                     metaModelResourceResolver.mergeMetaModelIntoRawModel( model, metaModelVersion ) ) );
      } catch ( final IOException exception ) {
         return Try.failure( exception );
      }
   }

   public static String migrateModel( final String aspectModel, final String storagePath ) {
      final InMemoryStrategy inMemoryStrategy = inMemoryStrategy( aspectModel, storagePath );

      final SdsAspectMetaModelResourceResolver metaModelResourceResolver = new SdsAspectMetaModelResourceResolver();

      final Try<VersionedModel> migratedFile = metaModelResourceResolver.getBammVersion( inMemoryStrategy.model )
                                                                        .flatMap( metaModelVersion ->
                                                                              metaModelResourceResolver.mergeMetaModelIntoRawModel(
                                                                                    inMemoryStrategy.model,
                                                                                    metaModelVersion ) )
                                                                        .flatMap(
                                                                              new MigratorService()::updateMetaModelVersion );

      final VersionedModel versionedModel = migratedFile.getOrElseThrow(
            e -> new InvalidAspectModelException( "AspectModel cannot be migrated.", e ) );

      return getPrettyPrintedVersionedModel( versionedModel, inMemoryStrategy.getAspectModelUrn().getUrn() );
   }

   public static ValidationReport validateModel( final String aspectModel, final String storagePath,
         final AspectModelValidator aspectModelValidator ) {
      try {
         final Try<VersionedModel> versionedModel = ModelUtils.fetchVersionModel( aspectModel,
               storagePath );

         return aspectModelValidator.validate( versionedModel );
      } catch ( final RiotException riotException ) {
         return new ValidationReportBuilder()
               .withValidationErrors( List.of( new ValidationError.Syntactic( riotException ) ) )
               .buildInvalidReport();
      } catch ( final IllegalArgumentException illegalArgumentException ) {
         return new ValidationReportBuilder()
               .withValidationErrors( List
                     .of( new ValidationError.Semantic( illegalArgumentException.getMessage(), null, null, null,
                           null ) ) )
               .buildInvalidReport();
      }
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
}
