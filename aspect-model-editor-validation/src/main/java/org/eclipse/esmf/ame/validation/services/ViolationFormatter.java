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

package org.eclipse.esmf.ame.validation.services;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.eclipse.esmf.ame.exceptions.UrnNotFoundException;
import org.eclipse.esmf.ame.validation.model.ViolationError;
import org.eclipse.esmf.aspectmodel.AspectModelFile;
import org.eclipse.esmf.aspectmodel.DocumentViolation;
import org.eclipse.esmf.aspectmodel.Violation;
import org.eclipse.esmf.aspectmodel.ViolationReport;
import org.eclipse.esmf.aspectmodel.resolver.ModelResolutionViolation;
import org.eclipse.esmf.aspectmodel.resolver.exceptions.ModelResolutionException;
import org.eclipse.esmf.aspectmodel.resolver.exceptions.ParserException;
import org.eclipse.esmf.aspectmodel.resolver.parser.SmartToken;
import org.eclipse.esmf.aspectmodel.resolver.parser.TokenRegistry;
import org.eclipse.esmf.aspectmodel.shacl.fix.Fix;
import org.eclipse.esmf.aspectmodel.shacl.violation.ClassTypeViolation;
import org.eclipse.esmf.aspectmodel.shacl.violation.ClosedViolation;
import org.eclipse.esmf.aspectmodel.shacl.violation.DatatypeViolation;
import org.eclipse.esmf.aspectmodel.shacl.violation.DisjointViolation;
import org.eclipse.esmf.aspectmodel.shacl.violation.EqualsViolation;
import org.eclipse.esmf.aspectmodel.shacl.violation.EvaluationContext;
import org.eclipse.esmf.aspectmodel.shacl.violation.InvalidValueViolation;
import org.eclipse.esmf.aspectmodel.shacl.violation.JsConstraintViolation;
import org.eclipse.esmf.aspectmodel.shacl.violation.LanguageFromListViolation;
import org.eclipse.esmf.aspectmodel.shacl.violation.LessThanOrEqualsViolation;
import org.eclipse.esmf.aspectmodel.shacl.violation.LessThanViolation;
import org.eclipse.esmf.aspectmodel.shacl.violation.MaxCountViolation;
import org.eclipse.esmf.aspectmodel.shacl.violation.MaxExclusiveViolation;
import org.eclipse.esmf.aspectmodel.shacl.violation.MaxInclusiveViolation;
import org.eclipse.esmf.aspectmodel.shacl.violation.MaxLengthViolation;
import org.eclipse.esmf.aspectmodel.shacl.violation.MinCountViolation;
import org.eclipse.esmf.aspectmodel.shacl.violation.MinExclusiveViolation;
import org.eclipse.esmf.aspectmodel.shacl.violation.MinInclusiveViolation;
import org.eclipse.esmf.aspectmodel.shacl.violation.MinLengthViolation;
import org.eclipse.esmf.aspectmodel.shacl.violation.NodeKindViolation;
import org.eclipse.esmf.aspectmodel.shacl.violation.NotViolation;
import org.eclipse.esmf.aspectmodel.shacl.violation.PatternViolation;
import org.eclipse.esmf.aspectmodel.shacl.violation.ShaclViolation;
import org.eclipse.esmf.aspectmodel.shacl.violation.SparqlConstraintViolation;
import org.eclipse.esmf.aspectmodel.shacl.violation.UniqueLanguageViolation;
import org.eclipse.esmf.aspectmodel.shacl.violation.ValueFromListViolation;
import org.eclipse.esmf.aspectmodel.urn.AspectModelUrn;
import org.eclipse.esmf.aspectmodel.validation.InvalidSyntaxViolation;
import org.eclipse.esmf.aspectmodel.validation.ProcessingViolation;

import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;

/**
 * The ViolationFormatter class is responsible for formatting a list of violations
 * into a list of violation errors. It implements the {@link Function} interface
 * to process violations and the {@link Violation.Visitor} interface to handle
 * specific types of violations.
 */
public class ViolationFormatter
      implements Function<ViolationReport, List<ViolationError>>, ShaclViolation.Visitor<ViolationError> {
   @Override
   public List<ViolationError> apply( final ViolationReport violationReport ) {
      final List<Violation> violations = violationReport.violations();
      if ( violations.isEmpty() ) {
         return List.of();
      }

      final List<ViolationError> violationErrors = new ArrayList<>();
      final List<Violation> nonSemanticViolations = filterNonSemanticViolations( violations );

      if ( !nonSemanticViolations.isEmpty() ) {
         return processNonSemanticViolation( nonSemanticViolations, violationErrors );
      }

      return processSemanticViolations( violations, violationErrors );
   }

   private List<Violation> filterNonSemanticViolations( final List<Violation> violations ) {
      return violations.stream().filter( violation -> isInvalidSyntaxViolation().test( violation )
            || isProcessingViolation().test( violation ) ).toList();
   }

   protected List<ViolationError> processNonSemanticViolation( final List<Violation> violations,
         final List<ViolationError> violationErrors ) {
      violations.forEach( violation -> violationErrors.add( formatViolation( violation ) ) );

      return violationErrors;
   }

   protected List<ViolationError> processSemanticViolations( final List<Violation> violations,
         final List<ViolationError> violationErrors ) {
      final Map<Class<? extends Violation>, List<Violation>> violationsByType = groupViolationsByType( violations );

      for ( final Map.Entry<Class<? extends Violation>, List<Violation>> entry : violationsByType.entrySet() ) {
         entry.getValue().forEach( violation -> violationErrors.add( formatViolation( violation ) ) );
      }
      return violationErrors;
   }

   private Map<Class<? extends Violation>, List<Violation>> groupViolationsByType( final List<Violation> violations ) {
      return violations.stream().collect( Collectors.groupingBy( Violation::getClass ) );
   }

   private ViolationError formatViolation( final Violation violation ) {
      if ( violation instanceof final ShaclViolation shaclViolation ) {
         try {
            return shaclViolation.accept( this );
         } catch ( final Exception e ) {
            final ViolationError fallback = visit( violation );
            fallback.addFix( "A validation error occurred while processing SHACL rules: " + e.getMessage() );
            return fallback;
         }
      }
      if ( violation instanceof final ProcessingViolation processingViolation ) {
         return visitProcessingViolation( processingViolation );
      }
      return visit( violation );
   }

   public ViolationError visit( final Violation violation ) {
      String message = cleanMessage( violation.message() );

      final Optional<String> filePath = extractFilePath( violation );
      if ( filePath.isPresent() && !filePath.get().isBlank() ) {
         message = String.format( "In '%s': %s", filePath.get(), message );
      }

      final ViolationError violationError = new ViolationError( message );

      if ( violation.code() != null ) {
         violationError.setErrorCode( violation.code().code() );
      }

      return violationError;
   }

   @Override
   public ViolationError visit( final ShaclViolation violation ) {
      final ViolationError violationError = visit( (Violation) violation );

      for ( final Fix possibleFix : violation.fixes() ) {
         violationError.addFix( String.format( "%n  > Possible fix: %s", possibleFix.description() ) );
      }

      return violationError;
   }

   public ViolationError visitProcessingViolation( final ProcessingViolation violation ) {
      final ViolationError violationError = visit( violation );
      final Throwable cause = violation.cause().orElse( null );

      if ( cause instanceof final UrnNotFoundException urnNotFoundException ) {
         violationError.setFocusNode( urnNotFoundException.getUrn() );
         violationError.setFix( List.of(
               "Ensure the referred element is available. If it's in a different model of the same namespace, include it in your "
                     + "workspace or the imported package." ) );
      } else if ( cause instanceof final org.eclipse.esmf.aspectmodel.resolver.exceptions.ModelResolutionException modelResolutionException ) {
         modelResolutionException.getCheckedLocations().stream().findFirst()
               .flatMap( org.eclipse.esmf.aspectmodel.resolver.ModelResolutionViolation::element )
               .ifPresent( violationError::setFocusNode );
         violationError.setFix( List.of(
               "The referenced element or model could not be resolved. Ensure that all referenced models are in the workspace with "
                     + "correct namespace and version structure." ) );
      } else if ( cause instanceof org.eclipse.esmf.aspectmodel.UnsupportedVersionException ) {
         violationError.setFix( List.of(
               "The Aspect Model uses an unsupported SAMM version. Please migrate the model to a supported SAMM version (e.g., SAMM 2.1.0"
                     + " or 2.2.0)." ) );
      } else if ( cause instanceof org.eclipse.esmf.aspectmodel.resolver.exceptions.ParserException ) {
         violationError.setFix( List.of(
               "The Turtle syntax contains errors. Check for missing prefixes, commas, semicolons, or unclosed quotes/brackets." ) );
      }

      return violationError;
   }

   @Override
   public ViolationError visitSparqlConstraintViolation( final SparqlConstraintViolation violation ) {
      return visit( violation );
   }

   @Override
   public ViolationError visitMinCountViolation( final MinCountViolation violation ) {
      return visit( violation );
   }

   @Override
   public ViolationError visitMaxCountViolation( final MaxCountViolation violation ) {
      return visit( violation );
   }

   @Override
   public ViolationError visitClassTypeViolation( final ClassTypeViolation violation ) {
      return visit( violation );
   }

   @Override
   public ViolationError visitDatatypeViolation( final DatatypeViolation violation ) {
      return visit( violation );
   }

   @Override
   public ViolationError visitInvalidValueViolation( final InvalidValueViolation violation ) {
      return visit( violation );
   }

   @Override
   public ViolationError visitLanguageFromListViolation( final LanguageFromListViolation violation ) {
      return visit( violation );
   }

   @Override
   public ViolationError visitMaxExclusiveViolation( final MaxExclusiveViolation violation ) {
      return visit( violation );
   }

   @Override
   public ViolationError visitMaxInclusiveViolation( final MaxInclusiveViolation violation ) {
      return visit( violation );
   }

   @Override
   public ViolationError visitMaxLengthViolation( final MaxLengthViolation violation ) {
      return visit( violation );
   }

   @Override
   public ViolationError visitMinExclusiveViolation( final MinExclusiveViolation violation ) {
      return visit( violation );
   }

   @Override
   public ViolationError visitMinInclusiveViolation( final MinInclusiveViolation violation ) {
      return visit( violation );
   }

   @Override
   public ViolationError visitMinLengthViolation( final MinLengthViolation violation ) {
      return visit( violation );
   }

   @Override
   public ViolationError visitNodeKindViolation( final NodeKindViolation violation ) {
      return visit( violation );
   }

   @Override
   public ViolationError visitPatternViolation( final PatternViolation violation ) {
      return visit( violation );
   }

   @Override
   public ViolationError visitJsViolation( final JsConstraintViolation violation ) {
      return visit( violation );
   }

   @Override
   public ViolationError visitUniqueLanguageViolation( final UniqueLanguageViolation violation ) {
      return visit( violation );
   }

   @Override
   public ViolationError visitEqualsViolation( final EqualsViolation violation ) {
      return visit( violation );
   }

   @Override
   public ViolationError visitDisjointViolation( final DisjointViolation violation ) {
      return visit( violation );
   }

   @Override
   public ViolationError visitLessThanViolation( final LessThanViolation violation ) {
      return visit( violation );
   }

   @Override
   public ViolationError visitLessThanOrEqualsViolation( final LessThanOrEqualsViolation violation ) {
      return visit( violation );
   }

   @Override
   public ViolationError visitValueFromListViolation( final ValueFromListViolation violation ) {
      return visit( violation );
   }

   @Override
   public ViolationError visitClosedViolation( final ClosedViolation violation ) {
      return visit( violation );
   }

   @Override
   public ViolationError visitNotViolation( final NotViolation violation ) {
      return visit( violation );
   }

   private String cleanMessage( final String message ) {
      if ( message == null ) {
         return "";
      }
      return message.replaceAll( "_:[a-zA-Z0-9]+", "(anonymous element)" );
   }

   private Optional<String> extractFilePath( final Violation violation ) {
      try {
         if ( violation instanceof final ShaclViolation shaclViolation ) {
            Optional<URI> loc = shaclViolation.sourceLocation();
            if ( loc.isEmpty() && shaclViolation.context() != null ) {
               loc = Optional.ofNullable( shaclViolation.context().element() )
                     .map( RDFNode::asNode )
                     .flatMap( TokenRegistry::getToken )
                     .map( SmartToken::getOriginatingFile )
                     .map( AspectModelFile::sourceUri );
            }
            if ( loc.isPresent() ) {
               return loc.map( this::formatUri );
            }
         }
         if ( violation instanceof final DocumentViolation documentViolation ) {
            try {
               return Optional.ofNullable( documentViolation.sourceDocument() ).map( this::formatUri );
            } catch ( final Exception ignored ) {
               // sourceDocument() can throw AspectModelException if the element has no registered token
            }
         }
         if ( violation instanceof final ProcessingViolation processingViolation ) {
            final Throwable cause = processingViolation.cause().orElse( null );
            if ( cause instanceof final ParserException parserException && parserException.getSourceLocation() != null ) {
               return Optional.of( formatUri( parserException.getSourceLocation() ) );
            }
            if ( cause instanceof final ModelResolutionException modelResolutionException ) {
               return modelResolutionException.getCheckedLocations().stream()
                     .findFirst()
                     .map( ModelResolutionViolation::location )
                     .map( this::formatUri );
            }
         }
      } catch ( final Exception ignored ) {
         // Gracefully handle any unexpected errors during file path extraction
      }
      return Optional.empty();
   }

   private String formatUri( final URI uri ) {
      if ( uri == null ) {
         return "";
      }
      return "file".equalsIgnoreCase( uri.getScheme() )
            ? new File( uri ).getAbsolutePath()
            : uri.toString();
   }

   /**
    * Creates a predicate that tests if a given violation is an invalid syntax violation.
    *
    * @return Predicate that can be used to filter invalid syntax violations
    */
   public static Predicate<Violation> isInvalidSyntaxViolation() {
      return violation -> violation.code() != null
            && violation.code().code().equals( InvalidSyntaxViolation.ERROR_CODE );
   }

   /**
    * Creates a predicate that tests if a given violation is a processing violation.
    *
    * @return Predicate that can be used to filter processing violations
    */
   public static Predicate<Violation> isProcessingViolation() {
      return violation -> violation.code() != null
            && violation.code().code().equals( ProcessingViolation.ERROR_CODE );
   }
}
