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
package org.eclipse.esmf.ame.exceptions;

import org.eclipse.esmf.ame.model.ReferenceReport;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Answers a {@link ModelReferencedException} with 409 and the {@link ReferenceReport} as body, so clients can show
 * which files still use the elements to be deleted.
 */
@Singleton
@Produces( MediaType.APPLICATION_JSON )
public class ModelReferencedExceptionHandler implements ExceptionHandler<ModelReferencedException, HttpResponse<ReferenceReport>> {
   private static final Logger LOG = LoggerFactory.getLogger( ModelReferencedExceptionHandler.class );

   @Override
   public HttpResponse<ReferenceReport> handle( final HttpRequest request, final ModelReferencedException exception ) {
      LOG.info( "{} for request {}", exception.getMessage(), request.getUri() );
      return HttpResponse.<ReferenceReport> status( HttpStatus.CONFLICT ).body( exception.getReport() );
   }
}
