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
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.eclipse.esmf.ame.constants.ApplicationConstants;
import org.eclipse.esmf.ame.exceptions.FileNotFoundException;
import org.eclipse.esmf.ame.exceptions.InvalidAspectModelException;
import org.eclipse.esmf.ame.repository.AspectModelRepository;
import org.eclipse.esmf.ame.services.file.FilePathResolver;
import org.eclipse.esmf.ame.services.models.AspectModelResult;
import org.eclipse.esmf.aspectmodel.AspectModelFile;
import org.eclipse.esmf.aspectmodel.UnsupportedVersionException;
import org.eclipse.esmf.aspectmodel.Violation;
import org.eclipse.esmf.aspectmodel.loader.AspectModelLoader;
import org.eclipse.esmf.aspectmodel.resolver.exceptions.ModelResolutionException;
import org.eclipse.esmf.aspectmodel.serializer.AspectSerializer;
import org.eclipse.esmf.aspectmodel.urn.AspectModelUrn;
import org.eclipse.esmf.aspectmodel.validation.services.AspectModelValidator;
import org.eclipse.esmf.metamodel.AspectModel;
import org.eclipse.esmf.metamodel.impl.DefaultScalar;
import org.eclipse.esmf.metamodel.impl.DefaultScalarValue;

import jakarta.inject.Singleton;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service responsible for reading aspect models.
 * Handles loading, validation, and retrieval of aspect models.
 */
@Singleton
public class AspectModelReader {
   private static final Logger LOG = LoggerFactory.getLogger( AspectModelReader.class );

   private final AspectModelLoader aspectModelLoader;
   private final AspectModelRepository aspectModelRepository;
   private final AspectModelValidator aspectModelValidator;
   private final FilePathResolver filePathResolver;
   private final Path modelPath;

   public AspectModelReader(
         final AspectModelLoader aspectModelLoader,
         final AspectModelRepository aspectModelRepository,
         final AspectModelValidator aspectModelValidator,
         final FilePathResolver filePathResolver,
         final Path modelPath ) {
      this.aspectModelLoader = aspectModelLoader;
      this.aspectModelRepository = aspectModelRepository;
      this.aspectModelValidator = aspectModelValidator;
      this.filePathResolver = filePathResolver;
      this.modelPath = modelPath;
   }

   /**
    * Retrieves an aspect model by its URN.
    *
    * @param aspectModelUrn the URN of the aspect model
    * @param filePath optional file path for loading from a specific file
    * @return the aspect model result containing content and location
    * @throws FileNotFoundException if the model cannot be found
    * @throws InvalidAspectModelException if the model is invalid
    */
   public AspectModelResult getModel( final AspectModelUrn aspectModelUrn, final @Nullable String filePath ) {
      try {
         final AspectModel aspectModel = loadModel( aspectModelUrn, filePath );
         validateBasicStructure( aspectModel );

         return aspectModel.files().stream()
               .filter( file -> containsElement( file, aspectModelUrn ) )
               .filter( this::hasValidCasing )
               .findFirst()
               .map( this::toResult )
               .orElseThrow( () -> new FileNotFoundException(
                     String.format( "Aspect Model '%s' not found in workspace", aspectModelUrn ) ) );
      } catch ( final ModelResolutionException e ) {
         LOG.error( "Model resolution failed for URN: {}", aspectModelUrn, e );
         throw new FileNotFoundException( String.format( "Aspect model resolution failed for URN '%s': %s", aspectModelUrn, e.getMessage() ), e );
      } catch ( final UnsupportedVersionException e ) {
         LOG.error( "Unsupported version for URN: {}", aspectModelUrn, e );
         throw new InvalidAspectModelException( String.format( "Aspect Model '%s' uses an unsupported SAMM version: %s", aspectModelUrn, e.getMessage() ), e );
      } catch ( final IllegalArgumentException e ) {
         LOG.error( "Illegal argument for URN: {}", aspectModelUrn, e );
         throw new FileNotFoundException( String.format( "Invalid path or argument for Aspect Model '%s': %s", aspectModelUrn, e.getMessage() ), e );
      }
   }

   /**
    * Checks if an element with the given URN exists in a different file.
    *
    * @param aspectModelUrn the URN to check
    * @param fileName the file name to exclude from the check
    * @return true if the element exists in a different file
    */
   public boolean checkElementExists( final AspectModelUrn aspectModelUrn, final String fileName ) {
      try {
         final AspectModel aspectModel = aspectModelRepository.loadByUrn( aspectModelUrn ).get();
         return aspectModel.files().stream()
               .anyMatch( f -> !fileName.equals( f.filename().orElse( "" ) ) );
      } catch ( final ModelResolutionException e ) {
         LOG.debug( "Element not found for URN: {}", aspectModelUrn );
         return false;
      }
   }

   private AspectModel loadModel( final AspectModelUrn aspectModelUrn, final @Nullable String filePath ) {
      if ( filePath != null && !filePath.isEmpty() ) {
         final Path resolvedPath = filePathResolver.resolveFromRelativePath( modelPath, filePath );
         return aspectModelLoader.load( resolvedPath.toFile() );
      }
      return aspectModelRepository.loadByUrn( aspectModelUrn ).get();
   }

   private void validateBasicStructure( final AspectModel aspectModel ) {
      final List<Violation> violations = aspectModelValidator.validateModel( aspectModel ).violations();
      final List<String> syntaxErrors = violations.stream()
            .filter( v -> v.code() != null && v.code().code() != null && v.code().code().contains( "INVALID_SYNTAX" ) )
            .map( Violation::message )
            .toList();

      if ( !syntaxErrors.isEmpty() ) {
         throw new InvalidAspectModelException( "Aspect Model has invalid syntax: " + String.join( "; ", syntaxErrors ) );
      }
   }

   /**
    * Checks if the given file contains the specified element.
    *
    * @param file the aspect model file
    * @param aspectModelUrn the URN to check for
    * @return true if the file contains the element
    */
   public boolean containsElement( final AspectModelFile file, final AspectModelUrn aspectModelUrn ) {
      return file.elements().stream().anyMatch( e ->
            ( e instanceof DefaultScalarValue && ( (DefaultScalarValue) e ).getType()
                  .equals( new DefaultScalar( aspectModelUrn.toString() ) ) )
                  || e.urn().equals( aspectModelUrn )
      );
   }

   /**
    * Checks if the file has valid casing (matches the actual filesystem).
    *
    * @param aspectModelFile the aspect model file to check
    * @return true if the casing is valid
    */
   public boolean hasValidCasing( final AspectModelFile aspectModelFile ) {
      try {
         final URI sourceLocation = aspectModelFile.sourceLocation()
               .orElseThrow( () -> new IOException( "Source location not present" ) );
         final Path file = Path.of( sourceLocation );

         if ( !Files.exists( file ) ) {
            return false;
         }

         final Path realPath = file.toRealPath();
         final Path providedPath = file.toAbsolutePath().normalize();

         return realPath.getFileName().toString().equals( providedPath.getFileName().toString() );
      } catch ( final IOException e ) {
         LOG.warn( "Could not verify file casing: {}", aspectModelFile.filename().orElse( "unknown" ), e );
         return false;
      }
   }

   private AspectModelResult toResult( final AspectModelFile file ) {
      return new AspectModelResult(
            file.filename(),
            AspectSerializer.INSTANCE.aspectModelFileToString( file ),
            file.sourceLocation()
      );
   }
}

