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

package org.eclipse.esmf.ame.services;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.eclipse.esmf.ame.exceptions.CreateFileException;
import org.eclipse.esmf.ame.exceptions.FileNotFoundException;
import org.eclipse.esmf.ame.exceptions.FileReadException;
import org.eclipse.esmf.ame.exceptions.InvalidAspectModelException;
import org.eclipse.esmf.ame.repository.AspectModelRepository;
import org.eclipse.esmf.ame.services.file.FileOperations;
import org.eclipse.esmf.ame.services.file.FilePathResolver;
import org.eclipse.esmf.ame.services.validation.ValidationOperations;
import org.eclipse.esmf.ame.validation.services.ViolationFormatter;
import org.eclipse.esmf.aspectmodel.AspectModelFile;
import org.eclipse.esmf.aspectmodel.Violation;
import org.eclipse.esmf.aspectmodel.loader.AspectModelLoader;
import org.eclipse.esmf.aspectmodel.serializer.AspectSerializer;
import org.eclipse.esmf.aspectmodel.urn.AspectModelUrn;
import org.eclipse.esmf.aspectmodel.validation.services.AspectModelValidator;
import org.eclipse.esmf.metamodel.AspectModel;

import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service responsible for writing aspect models.
 * Handles creation, updating, and deletion of aspect models.
 */
@Singleton
public class AspectModelWriter {
   private static final Logger LOG = LoggerFactory.getLogger( AspectModelWriter.class );

   private final AspectModelRepository aspectModelRepository;
   private final AspectModelValidator aspectModelValidator;
   private final AspectModelLoader aspectModelLoader;
   private final FileOperations fileOperations;
   private final FilePathResolver filePathResolver;
   private final ValidationOperations validationOperations;

   public AspectModelWriter(
         final AspectModelRepository aspectModelRepository,
         final AspectModelValidator aspectModelValidator,
         final AspectModelLoader aspectModelLoader,
         final FileOperations fileOperations,
         final FilePathResolver filePathResolver,
         final ValidationOperations validationOperations ) {
      this.aspectModelRepository = aspectModelRepository;
      this.aspectModelValidator = aspectModelValidator;
      this.aspectModelLoader = aspectModelLoader;
      this.fileOperations = fileOperations;
      this.filePathResolver = filePathResolver;
      this.validationOperations = validationOperations;
   }

   /**
    * Creates a new aspect model.
    *
    * @param turtleContent the Turtle content of the model
    * @param aspectModelUrn the URN for the model
    * @param fileName optional file name (empty for default)
    * @param storagePath the base storage path
    * @throws CreateFileException if the model cannot be created
    * @throws FileReadException if the model has invalid syntax
    */
   public void createModel( final String turtleContent, final AspectModelUrn aspectModelUrn,
         final String fileName, final Path storagePath ) {
      try {
         final Path targetFile = filePathResolver.resolveFilePath( aspectModelUrn, fileName, storagePath );
         LOG.info( "Creating model at: {}", targetFile );

         final Supplier<AspectModel> modelSupplier = aspectModelRepository.loadFromTurtle(
               turtleContent, targetFile.toFile() );

         validateForCreation( modelSupplier );

         fileOperations.createFile( targetFile );

         final AspectModelFile createdFile = findFileInModel( modelSupplier.get(), targetFile );
         AspectSerializer.INSTANCE.write( createdFile );

         LOG.info( "Model created successfully: {}", aspectModelUrn );
      } catch ( final IOException e ) {
         LOG.error( "Failed to create model: {}", aspectModelUrn, e );
         throw new CreateFileException( String.format( "Cannot create file %s on workspace", aspectModelUrn ), e );
      }
   }

   /**
    * Saves an existing aspect model (same as createModel for now).
    *
    * @param turtleContent the Turtle content of the model
    * @param aspectModelUrn the URN for the model
    * @param fileName optional file name
    * @param storagePath the base storage path
    */
   public void saveModel( final String turtleContent, final AspectModelUrn aspectModelUrn,
         final String fileName, final Path storagePath ) {
      createModel( turtleContent, aspectModelUrn, fileName, storagePath );
   }

   /**
    * Deletes an aspect model.
    *
    * @param aspectModelUrn the URN of the model to delete
    * @throws FileNotFoundException if the model cannot be found
    */
   public void deleteModel( final AspectModelUrn aspectModelUrn ) {
      LOG.info( "Deleting model: {}", aspectModelUrn );
      try {
         final AspectModel aspectModel = aspectModelLoader.load( aspectModelUrn );
         if ( aspectModel.files().isEmpty() ) {
            throw new FileNotFoundException( "No files found for Aspect Model: " + aspectModelUrn );
         }
         final AspectModelFile aspectModelFile = aspectModel.files().getFirst();
         fileOperations.deleteAspectModelFile( aspectModelFile );
         LOG.info( "Model deleted successfully: {}", aspectModelUrn );
      } catch ( final Exception e ) {
         LOG.error( "Failed to delete model: {}", aspectModelUrn, e );
         throw new FileNotFoundException( "Could not delete Aspect Model '" + aspectModelUrn + "': " + e.getMessage(), e );
      }
   }

   private void validateForCreation( final Supplier<AspectModel> modelSupplier ) {
      final List<Violation> violations = aspectModelValidator.validateModel( modelSupplier ).violations();

      validationOperations.throwIfViolationMatches(
            violations,
            ViolationFormatter.isInvalidSyntaxViolation(),
            new InvalidAspectModelException(
                  violations.stream()
                        .filter( ViolationFormatter.isInvalidSyntaxViolation() )
                        .findFirst()
                        .map( v -> "Aspect Model syntax error: " + v.message() )
                        .orElse( "Aspect Model syntax is invalid" )
            )
      );

      validationOperations.throwIfViolationMatches(
            violations,
            ViolationFormatter.isProcessingViolation(),
            new CreateFileException(
                  violations.stream()
                        .filter( ViolationFormatter.isProcessingViolation() )
                        .findFirst()
                        .map( v -> "Aspect Model processing error: " + v.message() )
                        .orElse( "Failed to process Aspect Model" )
            )
      );
   }

   private AspectModelFile findFileInModel( final AspectModel model, final Path targetFile ) {
      return model.files().stream()
            .filter( file -> file.sourceLocation()
                  .or( () -> Optional.ofNullable( file.sourceUri() ) )
                  .map( src -> {
                     try {
                        return Path.of( src ).toAbsolutePath().normalize().equals( targetFile.toAbsolutePath().normalize() );
                     } catch ( final Exception e ) {
                        return src.equals( targetFile.toUri() );
                     }
                  } )
                  .orElse( false ) )
            .findFirst()
            .orElseThrow( () -> new FileNotFoundException(
                  "Created aspect model file not found: " + targetFile ) );
   }
}

