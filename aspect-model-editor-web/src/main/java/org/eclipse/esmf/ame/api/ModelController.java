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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.esmf.ame.MediaTypeExtension;
import org.eclipse.esmf.ame.config.ApplicationSettings;
import org.eclipse.esmf.ame.exceptions.FileNotFoundException;
import org.eclipse.esmf.ame.services.ModelService;
import org.eclipse.esmf.ame.services.models.AspectModelResult;
import org.eclipse.esmf.ame.services.models.FileEntry;
import org.eclipse.esmf.ame.services.models.FileInformation;
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
   public HttpResponse<String> getModel( @Header( URN ) final Optional<String> urn ) {
      final AspectModelUrn aspectModelUrn = parseAspectModelUrn( urn );
      return HttpResponse.ok( modelService.getModel( aspectModelUrn, null ).content() );
   }

   /**
    * Checks if an Aspect Model element exists in the workspace.
    * <p>
    * This endpoint verifies the existence of an Aspect Model element identified by its URN
    * and checks if it exists in a file with a name different from the provided file name.
    *
    * @param urn the Aspect Model URN header parameter in the format urn:samm:namespace:version#AspectModelElement
    * @param fileName the file name to exclude from the existence check
    * @return True if the element exists in a different file, false otherwise
    */
   @Get( uri = "check-element", consumes = MediaType.APPLICATION_JSON )
   public HttpResponse<Boolean> checkElementExists( @Header( URN ) final Optional<String> urn, @QueryValue() final String fileName ) {
      final AspectModelUrn aspectModelUrn = parseAspectModelUrn( urn );
      return HttpResponse.ok( modelService.checkElementExists( aspectModelUrn, fileName ) );
   }

   /**
    * Method used to return multiple turtle files in batch based on a list of Aspect Model URNs.
    * Each URN consists of urn:samm:namespace:version#AspectModelElement
    */
   @Post( uri = "batch", consumes = MediaType.APPLICATION_JSON )
   @Produces( MediaType.APPLICATION_JSON )
   public HttpResponse<List<FileInformation>> getModelsBatch( @Body final List<FileEntry> fileEntries ) {
      final List<FileInformation> fileInformations = new ArrayList<>();

      for ( final FileEntry entry : fileEntries ) {
         try {
            final AspectModelUrn urn = parseAspectModelUrn( Optional.of( entry.aspectModelUrn() ) );
            final AspectModelResult aspectModelResult = modelService.getModel( urn, entry.absoluteName() );

            final FileInformation fileInformation = new FileInformation( entry.absoluteName(), entry.aspectModelUrn(), entry.modelVersion(),
                  aspectModelResult.content(), aspectModelResult.filename().orElse( "" ) );
            fileInformations.add( fileInformation );
         } catch ( final Exception e ) {
            throw new FileNotFoundException(
                  String.format( "Failed to retrieve Aspect Model for URN: %s - %s", entry.absoluteName(), e.getMessage() ) );
         }
      }

      return HttpResponse.ok( fileInformations );
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
   public HttpResponse<ViolationReport> validateModel( @Part( "aspectModel" ) final CompletedFileUpload aspectModel ) {
      return HttpResponse.ok( modelService.validateModel( aspectModel ) ).contentType( MediaType.APPLICATION_JSON );
   }

   /**
    * This Method is used to migrate a Turtle file. Performs a validation check.
    *
    * @param aspectModel - The Aspect Model Data
    * @return A migrated version of the Aspect Model
    */
   @Post( uri = "migrate", consumes = { MediaType.MULTIPART_FORM_DATA } )
   @Produces( MediaTypeExtension.TEXT_TURTLE_VALUE )
   public HttpResponse<String> migrateModel( @Part( "aspectModel" ) final CompletedFileUpload aspectModel ) {
      return HttpResponse.ok( modelService.migrateModel( aspectModel ) );
   }

   /**
    * This Method is used to format a Turtle file.
    *
    * @param aspectModel - The Aspect Model Data
    * @return A formatted version of the Aspect Model
    */
   @Post( uri = "format", consumes = { MediaType.MULTIPART_FORM_DATA } )
   @Produces( MediaTypeExtension.TEXT_TURTLE_VALUE )
   public HttpResponse<String> getFormattedModel( @Part( "aspectModel" ) final CompletedFileUpload aspectModel ) {
      return HttpResponse.ok( modelService.getFormattedModel( aspectModel ) );
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
      return HttpResponse.ok( modelService.migrateWorkspace( setNewVersion, ApplicationSettings.getMetaModelStoragePath() ) );
   }
}
