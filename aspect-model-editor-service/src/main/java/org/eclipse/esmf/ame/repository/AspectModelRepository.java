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

package org.eclipse.esmf.ame.repository;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

import org.eclipse.esmf.ame.exceptions.CreateFileException;
import org.eclipse.esmf.ame.exceptions.FileReadException;
import org.eclipse.esmf.aspectmodel.AspectLoadingException;
import org.eclipse.esmf.aspectmodel.loader.AspectModelLoader;
import org.eclipse.esmf.aspectmodel.resolver.exceptions.ModelResolutionException;
import org.eclipse.esmf.aspectmodel.urn.AspectModelUrn;
import org.eclipse.esmf.metamodel.AspectModel;
import org.eclipse.esmf.metamodel.ModelElement;

import io.micronaut.http.multipart.CompletedFileUpload;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Repository for loading and managing AspectModel instances.
 * Provides lazy-loading capabilities and various loading strategies.
 */
@Singleton
public class AspectModelRepository {
   private static final Logger LOG = LoggerFactory.getLogger( AspectModelRepository.class );

   private final AspectModelLoader aspectModelLoader;

   public AspectModelRepository( final AspectModelLoader aspectModelLoader ) {
      this.aspectModelLoader = aspectModelLoader;
   }

   /**
    * Loads an AspectModel by its URN.
    *
    * @param aspectModelUrn the Aspect Model URN
    * @return a lazy-loading Supplier for the Aspect Model
    */
   public Supplier<AspectModel> loadByUrn( final AspectModelUrn aspectModelUrn ) {
      return createLazySupplier( () -> aspectModelLoader.load( aspectModelUrn ) );
   }

   /**
    * Loads an AspectModel from multiple URNs.
    *
    * @param aspectModelUrns a list of Aspect Model URNs
    * @return a lazy-loading Supplier for the Aspect Model
    */
   public Supplier<AspectModel> loadByUrns( final List<AspectModelUrn> aspectModelUrns ) {
      return createLazySupplier( () -> aspectModelLoader.loadUrns( aspectModelUrns ) );
   }

   /**
    * Loads an AspectModel from Turtle data.
    *
    * @param turtleData the Turtle data as a string
    * @param targetFile the target file location
    * @return a lazy-loading Supplier for the Aspect Model
    */
   public Supplier<AspectModel> loadFromTurtle( final String turtleData, final File targetFile ) {
      return createLazySupplier( () -> {
         try ( final ByteArrayInputStream inputStream = new ByteArrayInputStream(
               turtleData.getBytes( StandardCharsets.UTF_8 ) ) ) {
            final AspectModel aspectModel = aspectModelLoader.load( inputStream, targetFile.toURI() );
            validateNoDuplicateElements( aspectModel, targetFile.getName() );
            return aspectModel;
         } catch ( final IOException e ) {
            throw new CreateFileException( "Failed to process Turtle data", e );
         }
      } );
   }

   /**
    * Loads an AspectModel from multiple files.
    *
    * @param files files containing Aspect Models
    * @return a lazy-loading Supplier for the Aspect Model
    */
   public Supplier<AspectModel> loadFromFiles( final List<File> files ) {
      return createLazySupplier( () -> aspectModelLoader.load( files ) );
   }

   /**
    * Loads an AspectModel from a file path.
    *
    * @param filePath the path to the model file
    * @return a lazy-loading Supplier for the Aspect Model
    */
   public Supplier<AspectModel> loadFromFile( final Path filePath ) {
      return createLazySupplier( () -> aspectModelLoader.load( filePath.toFile() ) );
   }

   /**
    * Loads an AspectModel from an uploaded file.
    *
    * @param upload the uploaded file
    * @param sourceUri the source URI for the model
    * @return the loaded AspectModel
    * @throws FileReadException if the file cannot be read
    */
   public AspectModel loadFromUpload( final CompletedFileUpload upload, final URI sourceUri ) {
      try ( final InputStream inputStream = upload.getInputStream() ) {
         return aspectModelLoader.load( inputStream, sourceUri );
      } catch ( final IOException e ) {
         throw new FileReadException( "Failed to read uploaded file: " + upload.getFilename() + " - " + e.getMessage() );
      }
   }

   private void validateNoDuplicateElements( final AspectModel aspectModel, final String sourceFilename ) {
      final boolean hasDifferentFile = aspectModel.elements().stream()
            .filter( modelElement -> modelElement.getSourceFile().filename().orElse( "" ).equals( sourceFilename ) )
            .anyMatch( modelElement -> isDuplicateInDifferentFile( modelElement, sourceFilename ) );

      if ( hasDifferentFile ) {
         final String message = "Some elements are already defined in the same namespace in a different file";
         LOG.warn( message );
         throw new CreateFileException( message );
      }
   }

   private boolean isDuplicateInDifferentFile( final ModelElement modelElement, final String sourceFilename ) {
      try {
         final String modelSourceFileName = modelElement.getSourceFile().filename().orElse( "" );
         return !modelSourceFileName.equals( sourceFilename );
      } catch ( final ModelResolutionException | AspectLoadingException ex ) {
         LOG.debug( "Could not determine source file for element: {}", modelElement.urn(), ex );
         return false;
      }
   }

   private Supplier<AspectModel> createLazySupplier( final Supplier<AspectModel> loader ) {
      return new Supplier<>() {
         private AspectModel aspectModel;
         private boolean isLoaded = false;

         @Override
         public AspectModel get() {
            if ( !isLoaded ) {
               aspectModel = loader.get();
               isLoaded = true;
            }
            return aspectModel;
         }
      };
   }
}

