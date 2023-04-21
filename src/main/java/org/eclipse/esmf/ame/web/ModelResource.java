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

package org.eclipse.esmf.ame.web;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.esmf.ame.config.ApplicationSettings;
import org.eclipse.esmf.ame.exceptions.FileNotFoundException;
import org.eclipse.esmf.ame.model.ValidationProcess;
import org.eclipse.esmf.ame.model.migration.Namespaces;
import org.eclipse.esmf.ame.model.validation.ViolationReport;
import org.eclipse.esmf.ame.services.ModelService;
import org.eclipse.esmf.ame.services.utils.ModelUtils;
import org.eclipse.esmf.ame.web.utils.MediaTypeExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller class where all the requests are mapped RequestMapping for the class is "aspect" generates a response
 * based on the mapping by calling the particular request handler methods
 */
@RestController
@RequestMapping( "models" )
public class ModelResource {

   public static final String FILE_NAME = "file-name";
   public static final String NAMESPACE = "namespace";
   private final ModelService modelService;

   public ModelResource( final ModelService modelService ) {
      this.modelService = modelService;
   }

   /**
    * Method used to return a turtle file based on the header parameter: Ame-Model-Urn which consists of
    * namespace:version:turtleFileName.ttl.
    */
   @GetMapping()
   public ResponseEntity<String> getModel( @RequestHeader final Map<String, String> headers ) {
      final Optional<String> optionalNameSpace = Optional.ofNullable( headers.get( NAMESPACE ) );
      final Optional<String> optionalFilename = Optional.ofNullable( headers.get( FILE_NAME ) );

      final String filename = optionalFilename.orElseThrow(
            () -> new FileNotFoundException( "Please specify a file name" ) );

      return ResponseEntity.ok( modelService.getModel( optionalNameSpace.orElse( "" ), filename, Optional.empty() ) );
   }

   /**
    * Method used to create a turtle file.
    *
    * @param turtleData To store in file.
    */
   @PostMapping( consumes = { MediaType.TEXT_PLAIN_VALUE, MediaTypeExtension.TEXT_TURTLE_VALUE } )
   public ResponseEntity<String> createModel( @RequestHeader final Map<String, String> headers,
         @RequestBody final String turtleData ) {
      final Optional<String> namespace = Optional.ofNullable( headers.get( NAMESPACE ) );
      final Optional<String> fileName = Optional.ofNullable( headers.get( FILE_NAME ) );
      modelService.saveModel( namespace, fileName, turtleData, Optional.empty() );

      return new ResponseEntity<>( HttpStatus.CREATED );
   }

   /**
    * This Method is used to validate a Turtle file
    *
    * @param aspectModel The Aspect Model Data
    * @return Either an empty array if the model is syntactically correct and conforms to the Aspect Meta Model
    *       semantics or provides a number of * {@link ViolationReport}s that describe all validation violations.
    */
   @PostMapping( "validate" )
   public ResponseEntity<ViolationReport> validateModel( @RequestBody final String aspectModel ) {
      return ResponseEntity.ok( modelService.validateModel( aspectModel, ValidationProcess.VALIDATION ) );
   }

   /**
    * This Method is used to migrate a Turtle file. Performs a validation check.
    *
    * @param aspectModel - The Aspect Model Data
    * @return A migrated version of the Aspect Model
    */
   @PostMapping( path = "migrate", consumes = { MediaType.TEXT_PLAIN_VALUE, MediaTypeExtension.TEXT_TURTLE_VALUE } )
   public ResponseEntity<String> migrateModel( @RequestBody final String aspectModel ) {
      return ResponseEntity.ok( ModelUtils.migrateModel( aspectModel, ValidationProcess.MIGRATE ) );
   }

   /**
    * This Method is used to format a Turtle file.
    *
    * @param aspectModel - The Aspect Model Data
    * @return A formatted version of the Aspect Model
    */
   @PostMapping( path = "format", consumes = { MediaType.TEXT_PLAIN_VALUE, MediaTypeExtension.TEXT_TURTLE_VALUE } )
   public ResponseEntity<String> getFormattedModel( @RequestBody final String aspectModel ) {
      return ResponseEntity.ok( modelService.getFormattedModel( aspectModel ) );
   }

   /**
    * This method migrates all Aspect models in the workspace.
    *
    * @return A list of Aspect Models that are migrated or not.
    */
   @GetMapping( path = "migrate-workspace" )
   public ResponseEntity<Namespaces> migrateWorkspace() {
      return ResponseEntity.ok( modelService.migrateWorkspace( ApplicationSettings.getMetaModelStoragePath() ) );
   }

   /**
    * Returns a map of key = namespace + version and value = list of turtle files that are present in that namespace.
    *
    * @param shouldRefresh - parameter that indicates if the map should be recreated of it should be returned from
    *       memory.
    */
   @GetMapping( "namespaces" )
   public ResponseEntity<Map<String, List<String>>> getAllNamespaces(
         @RequestParam( value = "shouldRefresh", required = false, defaultValue = "false" )
         final boolean shouldRefresh ) {
      return ResponseEntity.ok( modelService.getAllNamespaces( shouldRefresh, ValidationProcess.MODELS ) );
   }

   /**
    * Method used to delete a turtle file based on the header parameter: Ame-Model-Urn which consists of
    * namespace:version:turtleFileName.ttl.
    */
   @DeleteMapping()
   public void deleteModel( @RequestHeader final Map<String, String> headers ) {
      final Optional<String> optionalNameSpace = Optional.ofNullable( headers.get( NAMESPACE ) );
      final Optional<String> optionalFilename = Optional.ofNullable( headers.get( FILE_NAME ) );

      final String namespace = optionalNameSpace.orElseThrow(
            () -> new FileNotFoundException( "Please specify a namespace" ) );

      final String filename = optionalFilename.orElseThrow(
            () -> new FileNotFoundException( "Please specify a file name" ) );

      modelService.deleteModel( namespace, filename );
   }
}
