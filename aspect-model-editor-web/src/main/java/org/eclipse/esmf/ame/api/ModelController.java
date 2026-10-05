/*
 * Copyright (c) 2026 Robert Bosch Manufacturing Solutions GmbH
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
import org.eclipse.esmf.ame.api.model.response.AspectModelResponse;
import org.eclipse.esmf.ame.api.model.response.StoragePathResponse;
import org.eclipse.esmf.ame.config.ApplicationSettings;
import org.eclipse.esmf.ame.constants.ApplicationConstants;
import org.eclipse.esmf.ame.exceptions.FileNotFoundException;
import org.eclipse.esmf.ame.exceptions.InvalidAspectModelException;
import org.eclipse.esmf.ame.exceptions.UriNotDefinedException;
import org.eclipse.esmf.ame.model.ReferenceReport;
import org.eclipse.esmf.ame.security.FileNameSanitizer;
import org.eclipse.esmf.ame.services.AspectModelMigrator;
import org.eclipse.esmf.ame.services.AspectModelReader;
import org.eclipse.esmf.ame.services.AspectModelValidationService;
import org.eclipse.esmf.ame.services.AspectModelWriter;
import org.eclipse.esmf.ame.services.ModelService;
import org.eclipse.esmf.ame.services.models.AspectModelResult;
import org.eclipse.esmf.ame.services.models.ClearWorkspaceResult;
import org.eclipse.esmf.ame.services.models.FileEntry;
import org.eclipse.esmf.ame.services.models.FileInformation;
import org.eclipse.esmf.ame.services.models.MigrationResult;
import org.eclipse.esmf.ame.services.models.Version;
import org.eclipse.esmf.ame.services.workspace.WorkspaceCleanupService;
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
   private final ModelService modelService;
   private final AspectModelReader aspectModelReader;
   private final AspectModelWriter aspectModelWriter;
   private final AspectModelValidationService validationService;
   private final AspectModelMigrator aspectModelMigrator;
   private final FileNameSanitizer fileNameSanitizer;
   private final WorkspaceCleanupService workspaceCleanupService;

   public ModelController(
         final ModelService modelService,
         final AspectModelReader aspectModelReader,
         final AspectModelWriter aspectModelWriter,
         final AspectModelValidationService validationService,
         final AspectModelMigrator aspectModelMigrator,
         final FileNameSanitizer fileNameSanitizer,
         final WorkspaceCleanupService workspaceCleanupService ) {
      this.modelService = modelService;
      this.aspectModelReader = aspectModelReader;
      this.aspectModelWriter = aspectModelWriter;
      this.validationService = validationService;
      this.aspectModelMigrator = aspectModelMigrator;
      this.fileNameSanitizer = fileNameSanitizer;
      this.workspaceCleanupService = workspaceCleanupService;
   }

   private AspectModelUrn parseAspectModelUrn( final Optional<String> urn ) {
      final String urnString = urn.map( fileNameSanitizer::sanitize )
            .orElseThrow( () -> new FileNotFoundException( ApplicationConstants.ErrorMessages.SPECIFY_ASPECT_MODEL_URN ) );

      return AspectModelUrn.from( urnString ).toJavaOptional()
            .orElseThrow( () -> new InvalidAspectModelException(
                  String.format( "Invalid Aspect Model URN format: '%s'. Expected format: 'urn:samm:<namespace>:<version>#<element>'", urnString ) ) );
   }

   private URI parseAndValidateUri( final Optional<String> optionalUri ) {
      final String uriString = optionalUri.orElseThrow(
            () -> new UriNotDefinedException( ApplicationConstants.ErrorMessages.INVALID_URI_FORMAT ) );
      try {
         return new URI( uriString );
      } catch ( final URISyntaxException e ) {
         throw new UriNotDefinedException( "Invalid URI format: " + e.getMessage() );
      }
   }

   /**
    * Method used to return a turtle file based on the header parameter: Aspect-Model-Urn which consists of
    * urn:samm:namespace:version#AspectModelElement
    *
    * @param ignoreMissing if the references of the file cannot be resolved, returns the unresolved file instead of
    *       failing, so that the file can be opened and repaired
    */
   @Get()
   @Produces( MediaType.APPLICATION_JSON )
   public HttpResponse<AspectModelResponse> getModel( @Header( ApplicationConstants.Headers.URN ) final Optional<String> urn,
         @QueryValue( defaultValue = "false" ) final boolean ignoreMissing ) {
      final AspectModelUrn aspectModelUrn = parseAspectModelUrn( urn );
      final AspectModelResult result = ignoreMissing
            ? getModelOrUnresolved( aspectModelUrn )
            : aspectModelReader.getModel( aspectModelUrn, null );
      return HttpResponse.ok( new AspectModelResponse( result.content(), result.sourceLocation().orElse( null ) ) );
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
   public HttpResponse<Boolean> checkElementExists( @Header( ApplicationConstants.Headers.URN ) final Optional<String> urn,
         @QueryValue() final String fileName ) {
      final AspectModelUrn aspectModelUrn = parseAspectModelUrn( urn );
      return HttpResponse.ok( aspectModelReader.checkElementExists( aspectModelUrn, fileName ) );
   }

   private AspectModelResult getModelOrUnresolved( final AspectModelUrn aspectModelUrn ) {
      try {
         return aspectModelReader.getModel( aspectModelUrn, null );
      } catch ( final FileNotFoundException e ) {
         return modelService.getUnresolvedModel( aspectModelUrn ).orElseThrow( () -> e );
      }
   }

   /**
    * Method used to return multiple turtle files in batch based on a list of Aspect Model URNs.
    * Each URN consists of urn:samm:namespace:version#AspectModelElement
    *
    * @param fileEntries the requested elements
    * @param ignoreMissing leaves out elements that no workspace file defines instead of failing the request, and
    *       returns files whose own references cannot be resolved unresolved
    */
   @Post( uri = "batch", consumes = MediaType.APPLICATION_JSON )
   @Produces( MediaType.APPLICATION_JSON )
   public HttpResponse<List<FileInformation>> getModels( @Body final List<FileEntry> fileEntries,
         @QueryValue( defaultValue = "false" ) final boolean ignoreMissing ) {
      return HttpResponse.ok( modelService.getModels( fileEntries, ignoreMissing ) );
   }

   /**
    * Method used to create a turtle file.
    *
    * @param turtleData To store in file.
    */
   @Post( consumes = { MediaType.TEXT_PLAIN, MediaTypeExtension.TEXT_TURTLE_VALUE } )
   public HttpResponse<String> createOrSaveModel( @Body final String turtleData,
         @Header( ApplicationConstants.Headers.URN ) final Optional<String> urn,
         @Header( "file-name" ) final Optional<String> fileName ) {
      final Optional<String> optionalFileName = fileName.map( fileNameSanitizer::sanitize );
      final AspectModelUrn aspectModelUrn = parseAspectModelUrn( urn );
      final String name = optionalFileName.orElse( "" );
      aspectModelWriter.saveModel( turtleData, aspectModelUrn, name, ApplicationSettings.getMetaModelStoragePath() );
      return HttpResponse.status( HttpStatus.CREATED );
   }

   /**
    * Method used to delete a turtle file based on the header parameter: Ame-Model-Urn which consists of
    * urn:samm:namespace:version#AspectModelElement. Answers 409 with the reference report if other files still use
    * elements of the model.
    */
   @Delete()
   public void deleteModel( @Header( ApplicationConstants.Headers.URN ) final Optional<String> urn ) {
      aspectModelWriter.deleteModel( parseAspectModelUrn( urn ) );
   }

   /**
    * Checks which other workspace files use elements of a namespace version or, if a file name is given, of a single
    * Aspect Model file. Only incoming references count: what the checked files use themselves does not matter.
    *
    * @param namespace the namespace, e.g. {@code org.eclipse.example}
    * @param version the version, e.g. {@code 1.0.0}
    * @param fileName optional file name to check a single file instead of the whole namespace version
    * @return the files that use the elements and the files that could not be checked
    */
   @Get( uri = "references" )
   @Produces( MediaType.APPLICATION_JSON )
   public HttpResponse<ReferenceReport> getReferences( @QueryValue( "namespace" ) final String namespace,
         @QueryValue( "version" ) final String version,
         @QueryValue( "fileName" ) final Optional<String> fileName ) {
      return HttpResponse.ok( fileName
            .map( name -> workspaceCleanupService.checkFile( namespace, version, name ) )
            .orElseGet( () -> workspaceCleanupService.checkNamespace( namespace, version ) ) );
   }

   /**
    * Deletes all Aspect Model files of a namespace version. Not allowed (409 with the reference report) while files
    * of other namespaces or versions use its elements or cannot be checked.
    *
    * @param namespace the namespace
    * @param version the version
    * @return 200 if deleted, 409 with the reference report if it is still used
    */
   @Delete( uri = "namespace" )
   @Produces( MediaType.APPLICATION_JSON )
   public HttpResponse<ReferenceReport> deleteNamespace( @QueryValue( "namespace" ) final String namespace,
         @QueryValue( "version" ) final String version ) {
      workspaceCleanupService.deleteNamespace( namespace, version );
      return HttpResponse.ok( ReferenceReport.empty() );
   }

   /**
    * Deletes all Aspect Model files of the workspace. Backups and other files are kept.
    *
    * @param backup whether a backup of the workspace is created first (default: true)
    * @return the number of deleted files and whether a backup was created
    */
   @Delete( uri = "workspace" )
   @Produces( MediaType.APPLICATION_JSON )
   public HttpResponse<ClearWorkspaceResult> clearWorkspace( @QueryValue( value = "backup", defaultValue = "true" ) final boolean backup ) {
      return HttpResponse.ok( workspaceCleanupService.clearWorkspace( backup ) );
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
   public HttpResponse<ViolationReport> validateModel( @Header( ApplicationConstants.Headers.URI ) final Optional<String> optionalUri,
         @Part( "aspectModel" ) final CompletedFileUpload aspectModel ) {
      final URI uri = parseAndValidateUri( optionalUri );
      return HttpResponse.ok( validationService.validate( uri, aspectModel ) ).contentType( MediaType.APPLICATION_JSON );
   }

   /**
    * This Method is used to migrate a Turtle file. Performs a validation check.
    *
    * @param aspectModel - The Aspect Model Data
    * @return A migrated version of the Aspect Model
    */
   @Post( uri = "migrate", consumes = { MediaType.MULTIPART_FORM_DATA } )
   @Produces( MediaTypeExtension.TEXT_TURTLE_VALUE )
   public HttpResponse<String> migrateModel( @Header( ApplicationConstants.Headers.URI ) final Optional<String> optionalUri,
         @Part( "aspectModel" ) final CompletedFileUpload aspectModel ) {
      final URI uri = parseAndValidateUri( optionalUri );
      return HttpResponse.ok( aspectModelMigrator.migrate( uri, aspectModel ) );
   }

   /**
    * This Method is used to format a Turtle file.
    *
    * @param aspectModel - The Aspect Model Data
    * @return A formatted version of the Aspect Model
    */
   @Post( uri = "format", consumes = { MediaType.MULTIPART_FORM_DATA } )
   @Produces( MediaTypeExtension.TEXT_TURTLE_VALUE )
   public HttpResponse<String> getFormattedModel( @Header( ApplicationConstants.Headers.URI ) final Optional<String> optionalUri,
         @Part( "aspectModel" ) final CompletedFileUpload aspectModel ) {
      final URI uri = parseAndValidateUri( optionalUri );
      return HttpResponse.ok( aspectModelMigrator.format( uri, aspectModel ) );
   }

   /**
    * Returns a map of namespaces to their respective versions and models.
    * Each namespace is mapped to a list of versions, and each version contains a list of models.
    *
    * @return a HttpResponse containing a map where the key is the namespace and the value is a list of Version objects.
    */
   @Get( uri = "namespaces", consumes = MediaType.TEXT_PLAIN )
   public HttpResponse<Map<String, List<Version>>> getAllNamespaces() {
      return HttpResponse.ok( modelService.getAllNamespaces() );
   }

   /**
    * Returns the storage path where aspect models are stored.
    *
    * @return a HttpResponse containing the storage path details.
    */
   @Get( uri = "storage-path" )
   @Produces( MediaType.APPLICATION_JSON )
   public HttpResponse<StoragePathResponse> getStoragePath() {
      return HttpResponse.ok( new StoragePathResponse( modelService.getModelPath() ) );
   }

   /**
    * Alias for {@link #getStoragePath()} returning the models storage path.
    *
    * @return a HttpResponse containing the storage path details.
    */
   @Get( uri = "path" )
   @Produces( MediaType.APPLICATION_JSON )
   public HttpResponse<StoragePathResponse> getPath() {
      return getStoragePath();
   }

   /**
    * This method migrates all Aspect models in the workspace.
    *
    * @param setNewVersion set new Version for Aspect Models
    * @return A list of Aspect Models that are migrated or not.
    */
   @Get( uri = "migrate-workspace" )
   public HttpResponse<MigrationResult> migrateWorkspace( @QueryValue( defaultValue = "false" ) final boolean setNewVersion ) {
      return HttpResponse.ok( aspectModelMigrator.migrateWorkspace( modelService.getAllNamespaces(), setNewVersion,
            ApplicationSettings.getMetaModelStoragePath() ) );
   }
}
