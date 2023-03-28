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

package org.eclipse.esmf.ame.services;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Optional;

import org.apache.commons.lang3.LocaleUtils;
import org.eclipse.esmf.ame.exceptions.InvalidAspectModelException;
import org.eclipse.esmf.ame.model.ValidationProcess;
import org.eclipse.esmf.ame.services.utils.ModelUtils;
import org.eclipse.esmf.aspectmodel.generator.docu.AspectModelDocumentationGenerator;
import org.eclipse.esmf.aspectmodel.generator.json.AspectModelJsonPayloadGenerator;
import org.eclipse.esmf.aspectmodel.generator.jsonschema.AspectModelJsonSchemaGenerator;
import org.eclipse.esmf.aspectmodel.generator.openapi.AspectModelOpenApiGenerator;
import org.eclipse.esmf.aspectmodel.generator.openapi.PagingOption;
import org.eclipse.esmf.aspectmodel.resolver.services.DataType;
import org.eclipse.esmf.aspectmodel.resolver.services.VersionedModel;
import org.eclipse.esmf.metamodel.AspectContext;
import org.eclipse.esmf.metamodel.loader.AspectModelLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;

import io.vavr.control.Try;

@Service
public class GenerateService {
   private static final Logger LOG = LoggerFactory.getLogger( GenerateService.class );

   private static final String COULD_NOT_LOAD_ASPECT = "Could not load Aspect";
   private static final String COULD_NOT_LOAD_ASPECT_MODEL = "Could not load Aspect model, please make sure the model is valid.";

   public GenerateService() {
      DataType.setupTypeMapping();
   }

   public byte[] generateHtmlDocument( final String aspectModel, final String language,
         final ValidationProcess validationProcess ) throws IOException {
      final AspectModelDocumentationGenerator generator = new AspectModelDocumentationGenerator( language,
            generateAspectContext( aspectModel, validationProcess ) );

      final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

      generator.generate( artifactName -> byteArrayOutputStream,
            ImmutableMap.<AspectModelDocumentationGenerator.HtmlGenerationOption, String> builder().build() );

      return byteArrayOutputStream.toByteArray();
   }

   public String jsonSchema( final String aspectModel, final ValidationProcess validationProcess,
         final String language ) {
      try {
         final AspectContext aspectContext = generateAspectContext( aspectModel, validationProcess );

         final AspectModelJsonSchemaGenerator generator = new AspectModelJsonSchemaGenerator();
         final JsonNode jsonSchema = generator.apply( aspectContext.aspect(), Locale.forLanguageTag( language ) );

         final ByteArrayOutputStream out = new ByteArrayOutputStream();
         final ObjectMapper objectMapper = new ObjectMapper();

         objectMapper.writerWithDefaultPrettyPrinter().writeValue( out, jsonSchema );

         return out.toString();
      } catch ( final IOException e ) {
         LOG.error( "Aspect Model could not be loaded correctly." );
         throw new InvalidAspectModelException( COULD_NOT_LOAD_ASPECT, e );
      }
   }

   public String sampleJSONPayload( final String aspectModel, final ValidationProcess validationProcess ) {
      try {
         return new AspectModelJsonPayloadGenerator(
               generateAspectContext( aspectModel, validationProcess ) ).generateJson();
      } catch ( final IOException e ) {
         LOG.error( "Aspect Model could not be loaded correctly." );
         throw new InvalidAspectModelException( COULD_NOT_LOAD_ASPECT, e );
      }
   }

   private AspectContext generateAspectContext( final String aspectModel, final ValidationProcess validationProcess ) {
      final VersionedModel versionedModel = getVersionModel( aspectModel, validationProcess ).getOrElseThrow( () -> {
         LOG.error( COULD_NOT_LOAD_ASPECT_MODEL );
         return new InvalidAspectModelException( COULD_NOT_LOAD_ASPECT_MODEL );
      } );

      return new AspectContext( versionedModel, AspectModelLoader.getSingleAspectUnchecked( versionedModel ) );
   }

   private Try<VersionedModel> getVersionModel( final String aspectModel, final ValidationProcess validationProcess ) {
      return ModelUtils.fetchVersionModel( ModelUtils.inMemoryStrategy( aspectModel, validationProcess ) );
   }

   public String generateYamlOpenApiSpec( final String language, final String aspectModel, final String baseUrl,
         final boolean includeQueryApi, final boolean useSemanticVersion, final Optional<PagingOption> pagingOption ) {
      try {
         final AspectModelOpenApiGenerator generator = new AspectModelOpenApiGenerator();

         return generator.applyForYaml( ModelUtils.resolveAspectFromModel( aspectModel, ValidationProcess.MODELS ),
               useSemanticVersion, baseUrl, Optional.empty(), Optional.empty(), includeQueryApi, pagingOption,
               Locale.forLanguageTag( language ) );
      } catch ( final IOException e ) {
         LOG.error( "YAML OpenAPI specification could not be generated." );
         throw new InvalidAspectModelException( "Error generating YAML OpenAPI specification", e );
      }
   }

   public String generateJsonOpenApiSpec( final String language, final String aspectModel, final String baseUrl,
         final boolean includeQueryApi, final boolean useSemanticVersion, final Optional<PagingOption> pagingOption ) {
      try {
         final AspectModelOpenApiGenerator generator = new AspectModelOpenApiGenerator();

         final JsonNode json = generator.applyForJson(
               ModelUtils.resolveAspectFromModel( aspectModel, ValidationProcess.MODELS ), useSemanticVersion, baseUrl,
               Optional.empty(), Optional.empty(), includeQueryApi, pagingOption, LocaleUtils.toLocale( language ) );

         final ByteArrayOutputStream out = new ByteArrayOutputStream();
         final ObjectMapper objectMapper = new ObjectMapper();

         objectMapper.writerWithDefaultPrettyPrinter().writeValue( out, json );

         return out.toString();
      } catch ( final IOException e ) {
         LOG.error( "JSON OpenAPI specification could not be generated." );
         throw new InvalidAspectModelException( "Error generating JSON OpenAPI specification", e );
      }
   }
}
