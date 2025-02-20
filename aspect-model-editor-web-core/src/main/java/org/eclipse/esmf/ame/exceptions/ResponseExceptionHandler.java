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

package org.eclipse.esmf.ame.exceptions;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;

import java.util.Objects;

import org.eclipse.esmf.ame.model.ErrorResponse;
import org.eclipse.esmf.aspectmodel.AspectLoadingException;
import org.eclipse.esmf.aspectmodel.resolver.exceptions.InvalidNamespaceException;
import org.eclipse.esmf.aspectmodel.resolver.exceptions.ModelResolutionException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.util.WebUtils;

/**
 * Provides custom exception handling for the REST API.
 */
@ControllerAdvice
public class ResponseExceptionHandler extends ResponseEntityExceptionHandler {
   private static final Logger LOG = LoggerFactory.getLogger( ResponseExceptionHandler.class );

   private MessageSource messageSource;

   protected static void logRequest( final WebRequest request, final Throwable ex,
         final HttpStatus httpStatus ) {
      if ( httpStatus.is5xxServerError() ) {
         LOG.error( getLogRequestMessage( request, ex, httpStatus ), ex );
      } else {
         LOG.info( getLogRequestMessage( request, ex, httpStatus ) );
      }
   }

   private static String getLogRequestMessage( final WebRequest request, final Throwable ex,
         final HttpStatus httpStatus ) {
      final HttpServletRequest servletRequest = ( (ServletWebRequest) request ).getRequest();
      return servletRequest.getQueryString() == null ?
            getLogRequestMessage( servletRequest.getRequestURI(), ex, httpStatus ) :
            String.format( "Handling exception %s with response code %s of request %s?%s", ex.getClass().getName(),
                  httpStatus.value(), servletRequest.getRequestURI(), servletRequest.getQueryString() );
   }

   private static String getLogRequestMessage( final String requestURL, final Throwable ex,
         final HttpStatus httpStatus ) {
      return String.format( "Handling exception %s with response code %s of request %s", ex.getClass().getName(),
            httpStatus.value(), requestURL );
   }

   /**
    * Method for handling exception to type {@link FileNotFoundException}
    *
    * @param request the Http request
    * @param e the exception which occurred
    * @return the custom {@link ErrorResponse} as {@link ResponseEntity} for the exception
    */
   @ExceptionHandler( FileNotFoundException.class )
   public ResponseEntity<ErrorResponse> handleInvalidStateTransitionException( final WebRequest request,
         final FileNotFoundException e ) {
      return error( HttpStatus.NOT_FOUND, request, e, e.getMessage() );
   }

   /**
    * Method for handling exception to type {@link FileWriteException}
    *
    * @param request the Http request
    * @param e the exception which occurred
    * @return the custom {@link ErrorResponse} as {@link ResponseEntity} for the exception
    */
   @ExceptionHandler( FileWriteException.class )
   public ResponseEntity<ErrorResponse> handleInvalidStateTransitionException( final WebRequest request,
         final FileWriteException e ) {
      return error( HttpStatus.BAD_REQUEST, request, e, e.getMessage() );
   }

   /**
    * Method for handling exception to type {@link FileReadException}
    *
    * @param request the Http request
    * @param e the exception which occurred
    * @return the custom {@link ErrorResponse} as {@link ResponseEntity} for the exception
    */
   @ExceptionHandler( FileReadException.class )
   public ResponseEntity<ErrorResponse> handleInvalidStateTransitionException( final WebRequest request,
         final FileReadException e ) {
      return error( HttpStatus.CONFLICT, request, e, e.getMessage() );
   }

   /**
    * Method for handling exception to type {@link CreateFileException}
    *
    * @param request the Http request
    * @param e the exception which occurred
    * @return the custom {@link ErrorResponse} as {@link ResponseEntity} for the exception
    */
   @ExceptionHandler( CreateFileException.class )
   public ResponseEntity<ErrorResponse> handleInvalidStateTransitionException( final WebRequest request,
         final CreateFileException e ) {
      return error( HttpStatus.CONFLICT, request, e, e.getMessage() );
   }

   /**
    * Method for handling exception to type {@link IllegalArgumentException}
    *
    * @param request the Http request
    * @param e the exception which occurred
    * @return the custom {@link ErrorResponse} as {@link ResponseEntity} for the exception
    */
   @ExceptionHandler( IllegalArgumentException.class )
   public ResponseEntity<ErrorResponse> handleInvalidStateTransitionException( final WebRequest request,
         final IllegalArgumentException e ) {
      return error( HttpStatus.CONFLICT, request, e, e.getMessage() );
   }

   /**
    * Method for handling exception to type {@link AspectLoadingException}
    *
    * @param request the Http request
    * @param e the exception which occurred
    * @return the custom {@link ErrorResponse} as {@link ResponseEntity} for the exception
    */
   @ExceptionHandler( AspectLoadingException.class )
   public ResponseEntity<ErrorResponse> handleAspectLoadingException( final WebRequest request,
         final AspectLoadingException e ) {
      return error( HttpStatus.CONFLICT, request, e, e.getMessage() );
   }

   /**
    * Method for handling exception to type {@link InvalidAspectModelException}
    *
    * @param request the Http request
    * @param e the exception which occurred
    * @return the custom {@link ErrorResponse} as {@link ResponseEntity} for the exception
    */
   @ExceptionHandler( InvalidAspectModelException.class )
   public ResponseEntity<ErrorResponse> handleInvalidAspectModelException( final WebRequest request,
         final InvalidAspectModelException e ) {
      return error( HttpStatus.CONFLICT, request, e, e.getMessage() );
   }

   /**
    * Method for handling exception to type {@link InvalidNamespaceException}
    *
    * @param request the Http request
    * @param e the exception which occurred
    * @return the custom {@link ErrorResponse} as {@link ResponseEntity} for the exception
    */
   @ExceptionHandler( InvalidNamespaceException.class )
   public ResponseEntity<ErrorResponse> handleInvalidAspectModelException( final WebRequest request,
         final InvalidNamespaceException e ) {
      return error( HttpStatus.CONFLICT, request, e, e.getMessage() );
   }

   /**
    * Method for handling exception to type {@link FileCannotDeleteException}
    *
    * @param request the Http request
    * @param e the exception which occurred
    * @return the custom {@link ErrorResponse} as {@link ResponseEntity} for the exception
    */
   @ExceptionHandler( FileCannotDeleteException.class )
   public ResponseEntity<ErrorResponse> handleInvalidAspectModelException( final WebRequest request,
         final FileCannotDeleteException e ) {
      return error( HttpStatus.CONFLICT, request, e, e.getMessage() );
   }

   /**
    * Method for handling exception to type {@link GenerationException}
    *
    * @param request the Http request
    * @param e the exception which occurred
    * @return the custom {@link ErrorResponse} as {@link ResponseEntity} for the exception
    */
   @ExceptionHandler( GenerationException.class )
   public ResponseEntity<ErrorResponse> handleInvalidAspectModelException( final WebRequest request,
         final GenerationException e ) {
      return error( HttpStatus.BAD_REQUEST, request, e, e.getMessage() );
   }

   /**
    * Method for handling exception to type {@link FileHandlingException}
    *
    * @param request the Http request
    * @param e the exception which occurred
    * @return the custom {@link ErrorResponse} as {@link ResponseEntity} for the exception
    */
   @ExceptionHandler( FileHandlingException.class )
   public ResponseEntity<ErrorResponse> handleInvalidAspectModelException( final WebRequest request,
         final FileHandlingException e ) {
      return error( HttpStatus.CONFLICT, request, e, e.getMessage() );
   }

   /**
    * Method for handling exception to type {@link ModelResolutionException}
    *
    * @param request the Http request
    * @param e the exception which occurred
    * @return the custom {@link ErrorResponse} as {@link ResponseEntity} for the exception
    */
   @ExceptionHandler( ModelResolutionException.class )
   public ResponseEntity<ErrorResponse> handleInvalidAspectModelException( final WebRequest request,
         final ModelResolutionException e ) {
      return error( HttpStatus.CONFLICT, request, e, e.getMessage() );
   }

   private ResponseEntity<ErrorResponse> error( final HttpStatus responseCode, final WebRequest request,
         final RuntimeException e, final String message ) {
      logRequest( request, e, responseCode );

      final ErrorResponse errorResponse = new ErrorResponse( message,
            ( (ServletWebRequest) request ).getRequest().getRequestURI(), responseCode.value() );

      final HttpHeaders headers = new HttpHeaders();
      headers.add( CONTENT_TYPE, MediaType.APPLICATION_JSON.toString() );

      return handleExceptionInternal( e, errorResponse, headers, responseCode, request );
   }

   private ResponseEntity<ErrorResponse> handleExceptionInternal( final Exception ex,
         @Nullable final ErrorResponse body,
         final HttpHeaders headers, final HttpStatusCode statusCode, final WebRequest request ) {

      if ( isResponseCommitted( request ) ) {
         logger.warn( "Response already committed. Ignoring: " + ex );
         return null;
      }

      if ( statusCode.equals( HttpStatus.INTERNAL_SERVER_ERROR ) ) {
         request.setAttribute( WebUtils.ERROR_EXCEPTION_ATTRIBUTE, ex, WebRequest.SCOPE_REQUEST );
      }

      if ( body == null && ex instanceof org.springframework.web.ErrorResponse ) {
         return handleErrorResponse( ex, headers, statusCode, request );
      }

      return new ResponseEntity<>( body, headers, statusCode );
   }

   private boolean isResponseCommitted( final WebRequest request ) {
      if ( request instanceof ServletWebRequest ) {
         final HttpServletResponse response = ( (ServletWebRequest) request ).getResponse();
         return response != null && response.isCommitted();
      }
      return false;
   }

   private ResponseEntity<ErrorResponse> handleErrorResponse( final Exception ex, final HttpHeaders headers,
         final HttpStatusCode statusCode, final WebRequest request ) {
      final ResponseEntity<Object> entity = super.handleExceptionInternal( ex, null, headers, statusCode, request );
      final Object responseBody = Objects.requireNonNull( entity ).getBody();
      final ErrorResponse errorResponse = ( responseBody instanceof ErrorResponse ) ? (ErrorResponse) responseBody : null;

      return new ResponseEntity<>( errorResponse, entity.getHeaders(), entity.getStatusCode() );
   }
}
