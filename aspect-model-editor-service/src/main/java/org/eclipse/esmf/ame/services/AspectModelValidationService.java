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

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.esmf.ame.exceptions.AspectModelEditorException;
import org.eclipse.esmf.ame.exceptions.InvalidAspectModelException;
import org.eclipse.esmf.ame.repository.AspectModelRepository;
import org.eclipse.esmf.ame.validation.model.ViolationError;
import org.eclipse.esmf.ame.validation.model.ViolationReport;
import org.eclipse.esmf.ame.validation.services.ViolationFormatter;
import org.eclipse.esmf.aspectmodel.resolver.ModelResolutionViolation;
import org.eclipse.esmf.aspectmodel.resolver.exceptions.ModelResolutionException;
import org.eclipse.esmf.aspectmodel.urn.AspectModelUrn;
import org.eclipse.esmf.aspectmodel.validation.services.AspectModelValidator;
import org.eclipse.esmf.metamodel.AspectModel;

import io.micronaut.http.multipart.CompletedFileUpload;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service responsible for validating aspect models.
 * Handles validation of models from various sources.
 */
@Singleton
public class AspectModelValidationService {
   private static final Logger LOG = LoggerFactory.getLogger( AspectModelValidationService.class );

   private final AspectModelRepository aspectModelRepository;
   private final AspectModelValidator aspectModelValidator;
   private final ViolationFormatter violationFormatter;

   public AspectModelValidationService(
         final AspectModelRepository aspectModelRepository,
         final AspectModelValidator aspectModelValidator ) {
      this.aspectModelRepository = aspectModelRepository;
      this.aspectModelValidator = aspectModelValidator;
      this.violationFormatter = new ViolationFormatter();
   }

   /**
    * Validates an aspect model from an uploaded file.
    *
    * @param uri the source URI of the model
    * @param upload the uploaded file containing the model
    * @return a violation report with validation results
    */
   public ViolationReport validate( final URI uri, final CompletedFileUpload upload ) {
      LOG.debug( "Validating model from URI: {}", uri );

      try {
         final AspectModel aspectModel = aspectModelRepository.loadFromUpload( upload, uri );
         final org.eclipse.esmf.aspectmodel.ViolationReport violationReport = aspectModelValidator.validateModel( aspectModel );
         final List<ViolationError> violationErrors = violationFormatter.apply( violationReport );

         LOG.info( "Validation completed with {} violations", violationErrors.size() );
         return new ViolationReport( violationErrors );
      } catch ( final AspectModelEditorException e ) {
         LOG.error( "Validation failed for URI: {}", uri, e );
         throw e;
      } catch ( final Exception e ) {
         LOG.error( "Validation failed for URI: {}", uri, e );
         final String errorMessage = buildErrorMessage( e );
         throw new InvalidAspectModelException( errorMessage, e );
      }
   }

   private String buildErrorMessage( final Exception e ) {
      final Optional<ModelResolutionException> mreOpt = findModelResolutionException( e );
      if ( mreOpt.isEmpty() ) {
         return e.getMessage() != null && !e.getMessage().isBlank() ? e.getMessage() : "Aspect Model validation failed";
      }

      final ModelResolutionException mre = mreOpt.get();
      final List<ModelResolutionViolation> checkedLocations = mre.getCheckedLocations();
      if ( checkedLocations != null && !checkedLocations.isEmpty() ) {
         final List<String> elementMessages = checkedLocations.stream()
               .map( ModelResolutionViolation::element )
               .flatMap( Optional::stream )
               .map( AspectModelUrn::getUrn )
               .distinct()
               .map( urn -> String.format( "Element '%s' does not exist in a file.", urn ) )
               .toList();

         if ( !elementMessages.isEmpty() ) {
            return String.join( " ", elementMessages );
         }

         final List<String> violationMessages = checkedLocations.stream()
               .map( ModelResolutionViolation::message )
               .filter( msg -> msg != null && !msg.isBlank() )
               .distinct()
               .toList();

         if ( !violationMessages.isEmpty() ) {
            return String.join( "; ", violationMessages );
         }
      }

      final String fallbackMessage = mre.getMessage() != null && !mre.getMessage().isBlank()
            ? mre.getMessage()
            : e.getMessage();
      return fallbackMessage != null && !fallbackMessage.isBlank() ? fallbackMessage : "Aspect Model validation failed";
   }

   private Optional<ModelResolutionException> findModelResolutionException( final Throwable throwable ) {
      Throwable current = throwable;
      while ( current != null ) {
         if ( current instanceof final ModelResolutionException mre ) {
            return Optional.of( mre );
         }
         current = current.getCause();
      }
      return Optional.empty();
   }

   /**
    * Validates an aspect model instance directly.
    *
    * @param aspectModel the aspect model to validate
    * @return a violation report with validation results
    */
   public ViolationReport validate( final AspectModel aspectModel ) {
      LOG.debug( "Validating aspect model directly" );

      final org.eclipse.esmf.aspectmodel.ViolationReport violationReport = aspectModelValidator.validateModel( aspectModel );
      final List<ViolationError> violationErrors = violationFormatter.apply( violationReport );

      LOG.info( "Validation completed with {} violations", violationErrors.size() );
      return new ViolationReport( violationErrors );
   }

   /**
    * Checks if a model has any validation errors.
    *
    * @param aspectModel the model to check
    * @return true if the model is valid (no violations)
    */
   public boolean isValid( final AspectModel aspectModel ) {
      final org.eclipse.esmf.aspectmodel.ViolationReport violationReport = aspectModelValidator.validateModel( aspectModel );
      return violationReport.isEmpty();
   }
}

