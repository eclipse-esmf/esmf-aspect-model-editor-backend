/*
 * Copyright (c) 2025 Robert Bosch Manufacturing Solutions GmbH
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

package org.eclipse.esmf.ame.api;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.esmf.ame.MediaTypeExtension;
import org.eclipse.esmf.ame.config.ApplicationSettings;
import org.eclipse.esmf.ame.exceptions.FileNotFoundException;
import org.eclipse.esmf.ame.exceptions.UriNotDefinedException;
import org.eclipse.esmf.ame.services.ModelService;
import org.eclipse.esmf.ame.services.models.MigrationResult;
import org.eclipse.esmf.ame.services.models.Version;
import org.eclipse.esmf.ame.utils.ModelUtils;
import org.eclipse.esmf.ame.validation.model.ViolationReport;
import org.eclipse.esmf.aspectmodel.urn.AspectModelUrn;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.Part;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.http.multipart.CompletedFileUpload;
import io.vavr.Value;

/**
 * Controller class where all the requests are mapped. The RequestMapping for the class is "models".
 * This class generates a response based on the mapping by calling the particular request handler methods.
 */
@Controller( "models" )
public class ModelController {
   private static final String URI = "uri";
   private static final String URN = "aspect-model-urn";

   private final ModelService modelService;

   public ModelController( final ModelService modelService ) {
      this.modelService = modelService;
   }

   private AspectModelUrn parseAspectModelUrn( final Optional<String> urn ) {
      return urn.map( ModelUtils::sanitizeFileInformation ).map( AspectModelUrn::from ).flatMap( Value::toJavaOptional )
            .orElseThrow( () -> new FileNotFoundException( "Please specify an aspect model urn" ) );
   }

   /**
    * Method used to return a turtle file based on the header parameter: Aspect-Model-Urn which consists of
    * urn:samm:namespace:version#AspectModelElement
    */
   @Get()
   @Produces( MediaTypeExtension.TEXT_TURTLE_VALUE )
   public HttpResponse<String> getModel( @Header( URN ) final Optional<String> urn,
         @Header( "file-path" ) final Optional<String> filePath ) {
      final AspectModelUrn aspectModelUrn = parseAspectModelUrn( urn );
      final String path = filePath.orElse( null );
      return HttpResponse.ok( modelService.getModel( aspectModelUrn, path ) );
   }

   /**
    * Method used to create a turtle file.
    *
    * @param turtleData To store in file.
    */
   @Post( consumes = { MediaType.TEXT_PLAIN, MediaTypeExtension.TEXT_TURTLE_VALUE } )
   public HttpResponse<String> createOrSaveModel( @Body final String turtleData, @Header( URN ) final Optional<String> urn,
         @Header( "file-name" ) final Optional<String> fileName ) {
      final Optional<String> optionalFileName = fileName.map( ModelUtils::sanitizeFileInformation );
      final AspectModelUrn aspectModelUrn = parseAspectModelUrn( urn );
      final String name = optionalFileName.orElse( "" );
      modelService.createOrSaveModel( turtleData, aspectModelUrn, name, ApplicationSettings.getMetaModelStoragePath() );
      return HttpResponse.status( HttpStatus.CREATED );
   }

   /**
    * Method used to delete a turtle file based on the header parameter: Ame-Model-Urn which consists of
    * urn:samm:namespace:version#AspectModelElement.
    */
   @Delete()
   public void deleteModel( @Header( URN ) final Optional<String> urn ) {
      modelService.deleteModel( parseAspectModelUrn( urn ) );
   }

   /**
    * This Method is used to validate a Turtle file
    *
    * @param aspectModel The Aspect Model Data
    * @return Either an empty array if the model is syntactically correct and conforms to the Aspect Meta Model
    * semantics or provides a number of * {@link ViolationReport}s that describe all validation violations.
    */
   @Post( uri = "validate", consumes = { MediaType.MULTIPART_FORM_DATA } )
   @Produces( MediaType.APPLICATION_JSON )
   public HttpResponse<ViolationReport> validateModel( @Header( URI ) final Optional<String> optionalUri,
         @Part( "aspectModel" ) final CompletedFileUpload aspectModel ) throws URISyntaxException {
      final String uriString = optionalUri.orElseThrow( () -> new UriNotDefinedException( "Invalid Aspect Model File URI Format" ) );
      return HttpResponse.ok( modelService.validateModel( new URI( uriString ), aspectModel ) ).contentType( MediaType.APPLICATION_JSON );
   }

   /**
    * This Method is used to migrate a Turtle file. Performs a validation check.
    *
    * @param aspectModel - The Aspect Model Data
    * @return A migrated version of the Aspect Model
    */
   @Post( uri = "migrate", consumes = { MediaType.MULTIPART_FORM_DATA } )
   @Produces( MediaTypeExtension.TEXT_TURTLE_VALUE )
   public HttpResponse<String> migrateModel( @Header( URI ) final Optional<String> optionalUri,
         @Part( "aspectModel" ) final CompletedFileUpload aspectModel ) throws URISyntaxException {
      final String uriString = optionalUri.orElseThrow( () -> new UriNotDefinedException( "Invalid Aspect Model File URI Format" ) );
      return HttpResponse.ok( modelService.migrateModel( new URI( uriString ), aspectModel ) );
   }

   /**
    * This Method is used to format a Turtle file.
    *
    * @param aspectModel - The Aspect Model Data
    * @return A formatted version of the Aspect Model
    */
   @Post( uri = "format", consumes = { MediaType.MULTIPART_FORM_DATA } )
   @Produces( MediaTypeExtension.TEXT_TURTLE_VALUE )
   public HttpResponse<String> getFormattedModel( @Header( URI ) final Optional<String> optionalUri,
         @Part( "aspectModel" ) final CompletedFileUpload aspectModel ) throws URISyntaxException {
      final String uriString = optionalUri.orElseThrow( () -> new UriNotDefinedException( "Invalid Aspect Model File URI Format" ) );
      return HttpResponse.ok( modelService.getFormattedModel( new URI( uriString ), aspectModel ) );
   }

   /**
    * Returns a map of namespaces to their respective versions and models.
    * Each namespace is mapped to a list of versions, and each version contains a list of models.
    *
    * @param onlyAspectModels get only Aspect Models with Aspects as namespace list.
    * @return a HttpResponse containing a map where the key is the namespace and the value is a list of Version objects.
    */
   @Get( uri = "namespaces", consumes = MediaType.TEXT_PLAIN )
   public HttpResponse<Map<String, List<Version>>> getAllNamespaces(
         @QueryValue( defaultValue = "false" ) final boolean onlyAspectModels ) {
      return HttpResponse.ok( modelService.getAllNamespaces( onlyAspectModels ) );
   }

   /**
    * This method migrates all Aspect models in the workspace.
    *
    * @param setNewVersion set new Version for Aspect Models
    * @return A list of Aspect Models that are migrated or not.
    */
   @Get( uri = "migrate-workspace" )
   public HttpResponse<MigrationResult> migrateWorkspace( @QueryValue( defaultValue = "false" ) final boolean setNewVersion ) {
      return HttpResponse.ok( modelService.migrateWorkspace( setNewVersion ) );
   }
}
