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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

import org.eclipse.esmf.ame.constants.ApplicationConstants;
import org.eclipse.esmf.ame.exceptions.FileNotFoundException;
import org.eclipse.esmf.ame.exceptions.FileReadException;
import org.eclipse.esmf.ame.exceptions.InvalidAspectModelException;
import org.eclipse.esmf.ame.repository.AspectModelRepository;
import org.eclipse.esmf.ame.services.file.FilePathResolver;
import org.eclipse.esmf.ame.services.models.FileEntry;
import org.eclipse.esmf.ame.services.models.FileInformation;
import org.eclipse.esmf.ame.services.models.Version;
import org.eclipse.esmf.ame.services.utils.ModelGroupingUtils;
import org.eclipse.esmf.ame.services.validation.ValidationOperations;
import org.eclipse.esmf.aspectmodel.AspectModelFile;
import org.eclipse.esmf.aspectmodel.UnsupportedVersionException;
import org.eclipse.esmf.aspectmodel.loader.AspectModelLoader;
import org.eclipse.esmf.aspectmodel.resolver.ModelResolutionViolation;
import org.eclipse.esmf.aspectmodel.resolver.exceptions.ModelResolutionException;
import org.eclipse.esmf.aspectmodel.serializer.AspectSerializer;
import org.eclipse.esmf.aspectmodel.urn.AspectModelUrn;
import org.eclipse.esmf.aspectmodel.validation.services.AspectModelValidator;
import org.eclipse.esmf.metamodel.AspectModel;

import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service class for managing aspect models.
 * Provides methods to get, create, save, delete, validate, migrate, and format aspect models.
 */
@Singleton
public class ModelService {
   private static final Logger LOG = LoggerFactory.getLogger( ModelService.class );

   private final AspectModelValidator aspectModelValidator;
   private final AspectModelLoader aspectModelLoader;
   private final AspectModelRepository aspectModelRepository;
   private final AspectModelReader aspectModelReader;
   private final FilePathResolver filePathResolver;
   private final ValidationOperations validationOperations;
   private final Path modelPath;

   public ModelService( final AspectModelValidator aspectModelValidator, final AspectModelLoader aspectModelLoader,
         final AspectModelRepository aspectModelRepository, final AspectModelReader aspectModelReader,
         final FilePathResolver filePathResolver, final ValidationOperations validationOperations, final Path modelPath ) {
      this.aspectModelValidator = aspectModelValidator;
      this.aspectModelLoader = aspectModelLoader;
      this.aspectModelRepository = aspectModelRepository;
      this.aspectModelReader = aspectModelReader;
      this.filePathResolver = filePathResolver;
      this.validationOperations = validationOperations;
      this.modelPath = modelPath;
   }

   public Map<String, List<Version>> getAllNamespaces() {
      try {
         return new ModelGroupingUtils( aspectModelLoader, aspectModelValidator ).groupModelsByNamespaceAndVersion(
               aspectModelLoader.listContents() );
      } catch ( final ModelResolutionException e ) {
         LOG.error( e.getMessage() );
         throw new FileNotFoundException( "The models folder was not found. Please restart the application to create it automatically." );
      } catch ( final UnsupportedVersionException e ) {
         LOG.error( "{} There is a loose {} file somewhere — remove it along with any other non-standardized files.",
               ApplicationConstants.ErrorMessages.SAMM_STRUCTURE_INFO, ApplicationConstants.FileExtensions.TTL, e );
         throw new FileReadException( ApplicationConstants.ErrorMessages.SAMM_STRUCTURE_INFO + " Remove all non-standardized files." );
      }
   }

   public List<FileInformation> getModels( final List<FileEntry> fileEntries ) {
      final List<FileInformation> results = new ArrayList<>();

      for ( final FileEntry fileEntry : fileEntries ) {
         final Supplier<AspectModel> lazySupplier;
         final String fileIdentifier;
         final AspectModelUrn urn;

         if ( fileEntry.absoluteName() != null ) {
            final Path filePath = filePathResolver.resolveFromFileEntry( fileEntry, modelPath );
            lazySupplier = aspectModelRepository.loadFromFiles( List.of( filePath.toFile() ) );
            fileIdentifier = filePath.toString();

            urn = AspectModelUrn.from( fileEntry.aspectModelUrn() ).getOrElseThrow(
                  () -> new IllegalArgumentException( String.format( "Invalid aspect model URN: '%s'", fileEntry.aspectModelUrn() ) ) );
         } else {
            urn = AspectModelUrn.from( fileEntry.aspectModelUrn() ).getOrElseThrow(
                  () -> new IllegalArgumentException( String.format( "Invalid aspect model URN: '%s'", fileEntry.aspectModelUrn() ) ) );

            lazySupplier = aspectModelRepository.loadByUrns( List.of( urn ) );
            fileIdentifier = fileEntry.aspectModelUrn();
         }

         try {
            final AspectModel aspectModel = lazySupplier.get();

            final AspectModelFile aspectModelFile = aspectModel.files().stream()
                  .filter( file -> aspectModelReader.containsElement( file, urn ) ).filter( aspectModelReader::hasValidCasing ).findFirst()
                  .orElseThrow( () -> new FileNotFoundException(
                        String.format( "Aspect Model not found for URN '%s' in file '%s'", urn, fileIdentifier ) ) );

            results.add( convertToFileInformation( aspectModelFile ) );
         } catch ( final ModelResolutionException e ) {
            final String elementInfo = e.getCheckedLocations().stream().findFirst()
                  .flatMap( ModelResolutionViolation::element )
                  .map( element -> String.format( "Element '%s' not found", element ) )
                  .orElse( "Model resolution failed" );

            throw new FileNotFoundException( String.format( "Failed to load file '%s': %s", fileIdentifier, elementInfo ), e );
         }
      }

      return results;
   }

   private FileInformation convertToFileInformation( final AspectModelFile aspectModelFile ) {
      final String urn = extractUrn( aspectModelFile );
      final String sammVersion = validationOperations.extractSammVersion( aspectModelFile );
      final AspectModelUrn aspectModelUrn = aspectModelFile.namespaceUrn();
      final String fileName = aspectModelFile.filename().orElse( "" );

      final String fileKey = String.format( "%s:%s:%s", aspectModelUrn.getNamespaceMainPart(), aspectModelUrn.getVersion(),
            aspectModelFile.filename()
                  .orElseThrow( () -> new FileReadException( String.format( "Filename missing for Aspect Model with URN: %s", urn ) ) ) );

      return new FileInformation( fileKey, urn, sammVersion, AspectSerializer.INSTANCE.aspectModelFileToString( aspectModelFile ),
            fileName );
   }

   private String extractUrn( final AspectModelFile aspectModelFile ) {
      final String fileName = aspectModelFile.filename().orElse( "unknown file" );
      try {
         return aspectModelFile.aspect().urn().toString();
      } catch ( final NoSuchElementException e ) {
         if ( !aspectModelFile.elements().isEmpty() ) {
            try {
               return aspectModelFile.elements().getFirst().urn().toString();
            } catch ( final Exception ex ) {
               throw new InvalidAspectModelException(
                     String.format( "Invalid URN for element in file '%s': %s", fileName, ex.getMessage() ), ex );
            }
         }
         return aspectModelFile.namespaceUrn().toString();
      } catch ( final Exception e ) {
         throw new InvalidAspectModelException(
               String.format( "Invalid Aspect URN in file '%s': %s", fileName, e.getMessage() ), e );
      }
   }
}
