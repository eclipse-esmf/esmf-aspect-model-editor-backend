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

package org.eclipse.esmf.ame.api;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.esmf.ame.MediaTypeExtension;
import org.eclipse.esmf.ame.config.ApplicationSettings;
import org.eclipse.esmf.ame.exceptions.FileNotFoundException;
import org.eclipse.esmf.ame.services.ModelService;
import org.eclipse.esmf.ame.services.models.MigrationResult;
import org.eclipse.esmf.ame.services.models.Version;
import org.eclipse.esmf.ame.utils.ModelUtils;
import org.eclipse.esmf.ame.validation.model.ViolationError;
import org.eclipse.esmf.ame.validation.model.ViolationReport;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller class where all the requests are mapped. The RequestMapping for the class is "models".
 * This class generates a response based on the mapping by calling the particular request handler methods.
 */
@RestController
@RequestMapping( "models" )
public class ModelController {
   public static final String URN = "aspect-model-urn";

   private final ModelService modelService;

   public ModelController( final ModelService modelService ) {
      this.modelService = modelService;
   }

   /**
    * Method used to return a turtle file based on the header parameter: Aspect-Model-Urn which consists of
    * urn:samm:namespace:version#AspectModelElement
    */
   @GetMapping()
   public ResponseEntity<String> getModel( @RequestHeader final Map<String, String> headers ) {
      final Optional<String> optionalUrn = Optional.of(
            ModelUtils.sanitizeFileInformation( headers.get( URN ) ) );

      final String aspectModelUrn = optionalUrn.orElseThrow(
            () -> new FileNotFoundException( "Please specify an aspect model urn" ) );

      final String filePath = headers.get( "file-path" );

      return ResponseEntity.ok( modelService.getModel( aspectModelUrn, filePath ) );
   }

   /**
    * Method used to create a turtle file.
    *
    * @param turtleData To store in file.
    */
   @PostMapping( consumes = { MediaType.TEXT_PLAIN_VALUE, MediaTypeExtension.TEXT_TURTLE_VALUE } )
   public ResponseEntity<String> createOrSaveModel( @RequestBody final String turtleData,
         @RequestHeader final Map<String, String> headers ) {
      final Optional<String> optionalUrn = Optional.of(
            ModelUtils.sanitizeFileInformation( headers.get( URN ) ) );

      final Optional<String> optionalFileName = Optional.of(
            ModelUtils.sanitizeFileInformation( headers.get( "file-name" ) ) );

      final String aspectModelUrn = optionalUrn.orElseThrow(
            () -> new FileNotFoundException( "Please specify an aspect model urn" ) );

      final String fileName = optionalFileName.orElse( "" );

      modelService.createOrSaveModel( turtleData, aspectModelUrn, fileName, ApplicationSettings.getMetaModelStoragePath() );

      return new ResponseEntity<>( HttpStatus.CREATED );
   }

   /**
    * Method used to delete a turtle file based on the header parameter: Ame-Model-Urn which consists of
    * namespace:version:turtleFileName.ttl.
    */
   @DeleteMapping()
   public void deleteModel( @RequestHeader final Map<String, String> headers ) {
      final Optional<String> optionalUrn = Optional.of(
            ModelUtils.sanitizeFileInformation( headers.get( URN ) ) );

      final String aspectModelUrn = optionalUrn.orElseThrow(
            () -> new FileNotFoundException( "Please specify an aspect model urn" ) );

      modelService.deleteModel( aspectModelUrn );
   }

   /**
    * This Method is used to validate a Turtle file
    *
    * @param aspectModel The Aspect Model Data
    * @return Either an empty array if the model is syntactically correct and conforms to the Aspect Meta Model
    * semantics or provides a number of * {@link ViolationReport}s that describe all validation violations.
    */
   @PostMapping( "validate" )
   public ResponseEntity<List<ViolationError>> validateModel( @RequestBody final String aspectModel ) {
      return ResponseEntity.ok( modelService.validateModel( aspectModel ) );
   }

   /**
    * This Method is used to migrate a Turtle file. Performs a validation check.
    *
    * @param turtleData - The Aspect Model Data
    * @return A migrated version of the Aspect Model
    */
   @PostMapping( path = "migrate", consumes = { MediaType.TEXT_PLAIN_VALUE, MediaTypeExtension.TEXT_TURTLE_VALUE } )
   public ResponseEntity<String> migrateModel( @RequestBody final String turtleData ) {
      return ResponseEntity.ok( modelService.migrateModel( turtleData ) );
   }

   /**
    * This Method is used to format a Turtle file.
    *
    * @param turtleData - The Aspect Model Data
    * @return A formatted version of the Aspect Model
    */
   @PostMapping( path = "format", consumes = { MediaType.TEXT_PLAIN_VALUE, MediaTypeExtension.TEXT_TURTLE_VALUE } )
   public ResponseEntity<String> getFormattedModel( @RequestBody final String turtleData ) {
      return ResponseEntity.ok( modelService.getFormattedModel( turtleData ) );
   }

   /**
    * Returns a map of namespaces to their respective versions and models.
    * Each namespace is mapped to a list of versions, and each version contains a list of models.
    *
    * @return a ResponseEntity containing a map where the key is the namespace and the value is a list of Version objects.
    */
   @GetMapping( "namespaces" )
   public ResponseEntity<Map<String, List<Version>>> getAllNamespaces() {
      return ResponseEntity.ok( modelService.getAllNamespaces() );
   }

   /**
    * This method migrates all Aspect models in the workspace.
    *
    * @return A list of Aspect Models that are migrated or not.
    */
   @GetMapping( path = "migrate-workspace" )
   public ResponseEntity<MigrationResult> migrateWorkspace() {
      return ResponseEntity.ok( modelService.migrateWorkspace() );
   }
}
