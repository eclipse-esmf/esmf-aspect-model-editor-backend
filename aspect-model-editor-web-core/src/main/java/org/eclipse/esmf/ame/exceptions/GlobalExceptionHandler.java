package org.eclipse.esmf.ame.exceptions;

import org.eclipse.esmf.ame.model.ErrorResponse;
import org.eclipse.esmf.aspectmodel.AspectLoadingException;
import org.eclipse.esmf.aspectmodel.resolver.exceptions.InvalidNamespaceException;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Produces( "application/json" )
public class GlobalExceptionHandler implements ExceptionHandler<RuntimeException, HttpResponse<?>> {

   private static final Logger LOG = LoggerFactory.getLogger( GlobalExceptionHandler.class );

   @Override
   public HttpResponse<?> handle( final HttpRequest request, final RuntimeException e ) {
      final HttpStatus status = determineHttpStatus( e );
      logRequest( request, e, status );

      final ErrorResponse errorResponse = new ErrorResponse(
            new org.eclipse.esmf.ame.model.Error(
                  e.getMessage(),
                  request.getUri().toString(),
                  status.getCode() )
      );

      return HttpResponse.status( status ).body( errorResponse );
   }

   private HttpStatus determineHttpStatus( final RuntimeException e ) {
      if ( e instanceof FileNotFoundException ) {
         return HttpStatus.NOT_FOUND;
      } else if ( e instanceof FileWriteException || e instanceof GenerationException ) {
         return HttpStatus.BAD_REQUEST;
      } else if ( e instanceof AspectLoadingException || e instanceof InvalidNamespaceException || e instanceof InvalidAspectModelException
            || e instanceof FileReadException ) {
         return HttpStatus.CONFLICT;
      } else {
         return HttpStatus.INTERNAL_SERVER_ERROR;
      }
   }

   private void logRequest( final HttpRequest request, final Throwable ex, final HttpStatus status ) {
      if ( status.getCode() >= 500 ) {
         LOG.error( "Exception {} with response code {} for request {}", ex.getClass().getName(), status, request.getUri(), ex );
      } else {
         LOG.info( "Exception {} with response code {} for request {}", ex.getClass().getName(), status, request.getUri() );
      }
   }
}
