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

import java.net.URISyntaxException;
import java.util.NoSuchElementException;

import org.eclipse.esmf.ame.api.model.response.Error;
import org.eclipse.esmf.ame.api.model.response.ErrorResponse;
import org.eclipse.esmf.aspectmodel.AspectLoadingException;
import org.eclipse.esmf.aspectmodel.UnsupportedVersionException;
import org.eclipse.esmf.aspectmodel.ValueParsingException;
import org.eclipse.esmf.aspectmodel.resolver.exceptions.ModelResolutionException;
import org.eclipse.esmf.aspectmodel.resolver.exceptions.ParserException;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.exceptions.HttpStatusException;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import jakarta.inject.Singleton;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;

/**
 * Global exception handler for all exceptions in the Aspect Model Editor.
 * Converts exceptions into appropriate HTTP responses with proper status codes and intuitive error messages.
 */
@Singleton
@Produces( "application/json" )
public class GlobalExceptionHandler implements ExceptionHandler<Throwable, HttpResponse<?>> {

   private static final Logger LOG = LoggerFactory.getLogger( GlobalExceptionHandler.class );

   @Override
   public HttpResponse<?> handle( final @NonNull HttpRequest request, final @NonNull Throwable exception ) {
      final HttpStatus status = determineHttpStatus( exception );
      final String errorMessage = extractErrorMessage( exception );
      logException( request, exception, status, errorMessage );

      final ErrorResponse errorResponse = new ErrorResponse(
            new Error(
                  errorMessage,
                  request.getUri().toString(),
                  status.getCode() )
      );

      return HttpResponse.status( status ).body( errorResponse );
   }

   /**
    * Extracts an intuitive, user-friendly error message from the given exception.
    *
    * @param exception the exception to extract the message from
    * @return the extracted error message
    */
   private String extractErrorMessage( final Throwable exception ) {
      if ( exception instanceof AspectLoadingException ) {
         final Throwable cause = exception.getCause();
         if ( cause != null && cause.getMessage() != null && !cause.getMessage().isBlank() ) {
            return "Failed to load Aspect Model: " + cause.getMessage();
         }
      }

      if ( exception instanceof NoSuchElementException ) {
         final String msg = exception.getMessage();
         if ( msg == null || msg.isBlank() || "No value present".equalsIgnoreCase( msg ) ) {
            return "The requested Aspect Model element was not found.";
         }
      }

      if ( exception instanceof URISyntaxException ) {
         return "Invalid URI format: " + exception.getMessage();
      }

      if ( exception instanceof JacksonException ) {
         return "Invalid JSON/YAML data: " + exception.getMessage();
      }

      final String message = exception.getMessage();
      if ( message != null && !message.isBlank() ) {
         return message;
      }

      final Throwable cause = exception.getCause();
      if ( cause != null && cause.getMessage() != null && !cause.getMessage().isBlank() ) {
         return cause.getMessage();
      }

      return "An unexpected error occurred: " + exception.getClass().getSimpleName();
   }

   /**
    * Determines the appropriate HTTP status code for the given exception.
    *
    * @param exception the exception to determine status for
    * @return the appropriate HTTP status
    */
   private HttpStatus determineHttpStatus( final Throwable exception ) {
      if ( exception instanceof final AspectModelEditorException ameException ) {
         return HttpStatus.valueOf( ameException.getHttpStatusCode() );
      } else if ( exception instanceof AspectLoadingException || exception instanceof ModelResolutionException ) {
         return HttpStatus.CONFLICT;
      } else if ( exception instanceof UnsupportedVersionException ) {
         return HttpStatus.CONFLICT;
      } else if ( exception instanceof ParserException || exception instanceof ValueParsingException ) {
         return HttpStatus.BAD_REQUEST;
      } else if ( exception instanceof JacksonException ) {
         return HttpStatus.BAD_REQUEST;
      } else if ( exception instanceof IllegalArgumentException ) {
         return HttpStatus.BAD_REQUEST;
      } else if ( exception instanceof NoSuchElementException ) {
         return HttpStatus.NOT_FOUND;
      } else if ( exception instanceof IllegalStateException ) {
         return HttpStatus.BAD_REQUEST;
      } else if ( exception instanceof URISyntaxException ) {
         return HttpStatus.UNPROCESSABLE_ENTITY;
      } else if ( exception instanceof final HttpStatusException httpStatusException ) {
         return httpStatusException.getStatus();
      } else {
         return HttpStatus.INTERNAL_SERVER_ERROR;
      }
   }

   /**
    * Logs the exception with appropriate level based on HTTP status code.
    * Server errors (5xx) are logged as errors with stack trace,
    * client errors (4xx) are logged as info.
    *
    * @param request the HTTP request that caused the exception
    * @param exception the exception that occurred
    * @param status the HTTP status being returned
    * @param errorMessage the resolved error message
    */
   private void logException( final HttpRequest request, final Throwable exception, final HttpStatus status, final String errorMessage ) {
      if ( status.getCode() >= 500 ) {
         LOG.error( "Server error {} ({}) for request {}: {}",
               exception.getClass().getSimpleName(),
               status.getCode(),
               request.getUri(),
               errorMessage,
               exception );
      } else {
         LOG.info( "Client error {} ({}) for request {}: {}",
               exception.getClass().getSimpleName(),
               status.getCode(),
               request.getUri(),
               errorMessage );
      }
   }
}
