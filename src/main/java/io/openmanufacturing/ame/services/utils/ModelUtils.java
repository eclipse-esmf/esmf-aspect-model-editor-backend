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
import java.io.PrintWriter;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.jena.riot.RiotException;

import io.openmanufacturing.ame.resolver.inmemory.InMemoryStrategy;
import io.openmanufacturing.sds.aspectmodel.resolver.AspectModelResolver;
import io.openmanufacturing.sds.aspectmodel.resolver.services.VersionedModel;
import io.openmanufacturing.sds.aspectmodel.serializer.PrettyPrinter;
import io.openmanufacturing.sds.aspectmodel.urn.AspectModelUrn;
import io.openmanufacturing.sds.aspectmodel.validation.report.ValidationError;
import io.openmanufacturing.sds.aspectmodel.validation.report.ValidationReport;
import io.openmanufacturing.sds.aspectmodel.validation.report.ValidationReportBuilder;
import io.openmanufacturing.sds.aspectmodel.validation.services.AspectModelValidator;
import io.vavr.control.Try;

public class ModelUtils {

   public final static Pattern URN_PATTERN = Pattern.compile(
         "^urn:[a-z0-9][a-z0-9-]{0,31}:([a-z0-9()+,\\-.:=@;$_!*#']|%[0-9a-f]{2})+$",
         Pattern.CASE_INSENSITIVE );

   private ModelUtils() {
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

   public static Try<VersionedModel> fetchVersionModel( final String aspectModel, final String storagePath ) {
      final InMemoryStrategy inMemoryStrategy = new InMemoryStrategy( aspectModel, Path.of( storagePath ) );
      final AspectModelUrn aspectModelUrn = inMemoryStrategy.getAspectModelUrn();

      return new AspectModelResolver().resolveAspectModel( inMemoryStrategy, aspectModelUrn );
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
}
