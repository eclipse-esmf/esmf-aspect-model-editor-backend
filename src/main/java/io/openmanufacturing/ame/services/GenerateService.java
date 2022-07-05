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

package io.openmanufacturing.ame.services;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;

import io.openmanufacturing.ame.config.ApplicationSettings;
import io.openmanufacturing.ame.exceptions.InvalidAspectModelException;
import io.openmanufacturing.ame.services.utils.ModelUtils;
import io.openmanufacturing.sds.aspectmodel.generator.docu.AspectModelDocumentationGenerator;
import io.openmanufacturing.sds.aspectmodel.generator.json.AspectModelJsonPayloadGenerator;
import io.openmanufacturing.sds.aspectmodel.generator.jsonschema.AspectModelJsonSchemaGenerator;
import io.openmanufacturing.sds.aspectmodel.resolver.services.DataType;
import io.openmanufacturing.sds.aspectmodel.resolver.services.VersionedModel;
import io.openmanufacturing.sds.metamodel.Aspect;
import io.openmanufacturing.sds.metamodel.loader.AspectLoadingException;
import io.openmanufacturing.sds.metamodel.loader.AspectModelLoader;
import io.vavr.control.Try;

@Service
public class GenerateService {
   private static final Logger LOG = LoggerFactory.getLogger( GenerateService.class );

   public GenerateService() {
      DataType.setupTypeMapping();
   }

   public byte[] generateHtmlDocument( final String model, final Optional<String> storagePath ) throws IOException {
      final Try<VersionedModel> versionedModel = ModelUtils.fetchVersionModel( model,
            storagePath.orElse( ApplicationSettings.getMetaModelStoragePath() ) );
      final AspectModelDocumentationGenerator generator = new AspectModelDocumentationGenerator(
            versionedModel.get() );
      final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

      generator.generate( artifactName -> byteArrayOutputStream,
            ImmutableMap.<AspectModelDocumentationGenerator.HtmlGenerationOption, String> builder().build() );

      return byteArrayOutputStream.toByteArray();
   }

   public String jsonSchema( final String aspectModel, final Optional<String> storagePath ) {
      try {
         final Try<VersionedModel> versionedModel = ModelUtils.fetchVersionModel( aspectModel,
               storagePath.orElse( ApplicationSettings.getMetaModelStoragePath() ) );

         final Aspect aspect = AspectModelLoader.fromVersionedModel( versionedModel.get() ).getOrElseThrow(
               e -> {
                  LOG.error( "Could not load Aspect model, please make sure the model is valid." );
                  return new InvalidAspectModelException(
                        "Could not load Aspect model, please make sure the model is valid.", e );
               } );

         final AspectModelJsonSchemaGenerator generator = new AspectModelJsonSchemaGenerator();
         final JsonNode jsonSchema = generator.apply( aspect );

         final ByteArrayOutputStream out = new ByteArrayOutputStream();
         final ObjectMapper objectMapper = new ObjectMapper();

         objectMapper.writerWithDefaultPrettyPrinter().writeValue( out, jsonSchema );
         return out.toString();
      } catch ( final IOException e ) {
         LOG.error( "Aspect Model could not be loaded correctly." );
         throw new InvalidAspectModelException( "Could not load Aspect", e );
      }
   }

   public String sampleJSONPayload( final String aspectModel, final Optional<String> storagePath ) {
      try {
         final Try<VersionedModel> versionedModel = ModelUtils.fetchVersionModel( aspectModel,
               storagePath.orElse( ApplicationSettings.getMetaModelStoragePath() ) );

         final AspectModelJsonPayloadGenerator generator = new AspectModelJsonPayloadGenerator( versionedModel.get() );
         return generator.generateJson();
      } catch ( final AspectLoadingException e ) {
         LOG.error( "Could not load Aspect model, please make sure the model is valid." );
         throw new InvalidAspectModelException( "Could not load Aspect model, please make sure the model is valid.",
               e );
      } catch ( final IOException e ) {
         LOG.error( "Aspect Model could not be loaded correctly." );
         throw new InvalidAspectModelException( "Could not load Aspect", e );
      }
   }
}
