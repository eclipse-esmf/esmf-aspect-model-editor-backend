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
import java.util.List;

import org.eclipse.esmf.ame.repository.AspectModelRepository;
import org.eclipse.esmf.ame.validation.model.ViolationError;
import org.eclipse.esmf.ame.validation.model.ViolationReport;
import org.eclipse.esmf.ame.validation.services.ViolationFormatter;
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
      } catch ( final Exception e ) {
         LOG.error( "Validation failed for URI: {}", uri, e );
         throw e;
      }
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

