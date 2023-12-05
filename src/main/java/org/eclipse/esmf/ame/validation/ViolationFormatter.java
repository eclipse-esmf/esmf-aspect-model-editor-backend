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

package org.eclipse.esmf.ame.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.esmf.ame.exceptions.UrnNotFoundException;
import org.eclipse.esmf.ame.model.validation.ViolationError;
import org.eclipse.esmf.ame.services.utils.ModelUtils;
import org.eclipse.esmf.aspectmodel.shacl.fix.Fix;
import org.eclipse.esmf.aspectmodel.shacl.violation.ClassTypeViolation;
import org.eclipse.esmf.aspectmodel.shacl.violation.ClosedViolation;
import org.eclipse.esmf.aspectmodel.shacl.violation.DatatypeViolation;
import org.eclipse.esmf.aspectmodel.shacl.violation.DisjointViolation;
import org.eclipse.esmf.aspectmodel.shacl.violation.EqualsViolation;
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
import org.eclipse.esmf.aspectmodel.shacl.violation.ProcessingViolation;
import org.eclipse.esmf.aspectmodel.shacl.violation.SparqlConstraintViolation;
import org.eclipse.esmf.aspectmodel.shacl.violation.UniqueLanguageViolation;
import org.eclipse.esmf.aspectmodel.shacl.violation.ValueFromListViolation;
import org.eclipse.esmf.aspectmodel.shacl.violation.Violation;
import org.eclipse.esmf.aspectmodel.urn.AspectModelUrn;

public class ViolationFormatter
      implements Function<List<Violation>, List<ViolationError>>, Violation.Visitor<ViolationError> {
   @Override
   public List<ViolationError> apply( final List<Violation> violations ) {
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
      return violations.stream().filter( violation -> ModelUtils.isInvalidSyntaxViolation().test( violation )
            || ModelUtils.isProcessingViolation().test( violation ) ).toList();
   }

   protected List<ViolationError> processNonSemanticViolation( final List<Violation> violations,
         final List<ViolationError> violationErrors ) {
      violations.forEach( violation -> violationErrors.add( violation.accept( this ) ) );

      return violationErrors;
   }

   protected List<ViolationError> processSemanticViolations( final List<Violation> violations,
         final List<ViolationError> violationErrors ) {
      final Map<Class<? extends Violation>, List<Violation>> violationsByType = groupViolationsByType( violations );

      for ( final Map.Entry<Class<? extends Violation>, List<Violation>> entry : violationsByType.entrySet() ) {
         entry.getValue().forEach( violation -> violationErrors.add( violation.accept( this ) ) );
      }
      return violationErrors;
   }

   private Map<Class<? extends Violation>, List<Violation>> groupViolationsByType( final List<Violation> violations ) {
      return violations.stream().collect( Collectors.groupingBy( Violation::getClass ) );
   }

   @Override
   public ViolationError visit( final Violation violation ) {
      final ViolationError violationError = new ViolationError( violation.message() );

      violationError.setErrorCode( violation.errorCode() );

      for ( final Fix possibleFix : violation.fixes() ) {
         violationError.addFix( String.format( "%n  > Possible fix: %s", possibleFix.description() ) );
      }

      return violationError;
   }

   @Override
   public ViolationError visitProcessingViolation( final ProcessingViolation violation ) {
      final ViolationError violationError = visit( violation );

      if ( violation.cause() instanceof UrnNotFoundException urnNotFoundException ) {
         violationError.setFocusNode( urnNotFoundException.getUrn() );
         violationError.setFix( List.of(
               "Ensure the referred element is available. If it's in a different model of the same namespace, include it in your workspace or the imported package." ) );
      }

      return violationError;
   }

   @Override
   public ViolationError visitSparqlConstraintViolation( final SparqlConstraintViolation violation ) {
      final ViolationError violationError = visit( violation );
      violationError.setFocusNode( AspectModelUrn.fromUrn( violation.context().element().getURI() ) );

      return violationError;
   }

   @Override
   public ViolationError visitMinCountViolation( final MinCountViolation violation ) {
      final ViolationError violationError = visit( violation );
      violationError.setFocusNode( AspectModelUrn.fromUrn( violation.context().element().getURI() ) );
      return violationError;
   }

   @Override
   public ViolationError visitMaxCountViolation( final MaxCountViolation violation ) {
      final ViolationError violationError = visit( violation );
      violationError.setFocusNode( AspectModelUrn.fromUrn( violation.context().element().getURI() ) );
      return violationError;
   }

   @Override
   public ViolationError visitClassTypeViolation( final ClassTypeViolation violation ) {
      final ViolationError violationError = visit( violation );
      violationError.setFocusNode( AspectModelUrn.fromUrn( violation.context().element().getURI() ) );
      return violationError;
   }

   @Override
   public ViolationError visitDatatypeViolation( final DatatypeViolation violation ) {
      final ViolationError violationError = visit( violation );
      violationError.setFocusNode( AspectModelUrn.fromUrn( violation.context().element().getURI() ) );
      return violationError;
   }

   @Override
   public ViolationError visitInvalidValueViolation( final InvalidValueViolation violation ) {
      final ViolationError violationError = visit( violation );
      violationError.setFocusNode( AspectModelUrn.fromUrn( violation.context().element().getURI() ) );
      return violationError;
   }

   @Override
   public ViolationError visitLanguageFromListViolation( final LanguageFromListViolation violation ) {
      final ViolationError violationError = visit( violation );
      violationError.setFocusNode( AspectModelUrn.fromUrn( violation.context().element().getURI() ) );
      return violationError;
   }

   @Override
   public ViolationError visitMaxExclusiveViolation( final MaxExclusiveViolation violation ) {
      final ViolationError violationError = visit( violation );
      violationError.setFocusNode( AspectModelUrn.fromUrn( violation.context().element().getURI() ) );
      return violationError;
   }

   @Override
   public ViolationError visitMaxInclusiveViolation( final MaxInclusiveViolation violation ) {
      final ViolationError violationError = visit( violation );
      violationError.setFocusNode( AspectModelUrn.fromUrn( violation.context().element().getURI() ) );
      return violationError;
   }

   @Override
   public ViolationError visitMaxLengthViolation( final MaxLengthViolation violation ) {
      final ViolationError violationError = visit( violation );
      violationError.setFocusNode( AspectModelUrn.fromUrn( violation.context().element().getURI() ) );
      return violationError;
   }

   @Override
   public ViolationError visitMinExclusiveViolation( final MinExclusiveViolation violation ) {
      final ViolationError violationError = visit( violation );
      violationError.setFocusNode( AspectModelUrn.fromUrn( violation.context().element().getURI() ) );
      return violationError;
   }

   @Override
   public ViolationError visitMinInclusiveViolation( final MinInclusiveViolation violation ) {
      final ViolationError violationError = visit( violation );
      violationError.setFocusNode( AspectModelUrn.fromUrn( violation.context().element().getURI() ) );
      return violationError;
   }

   @Override
   public ViolationError visitMinLengthViolation( final MinLengthViolation violation ) {
      final ViolationError violationError = visit( violation );
      violationError.setFocusNode( AspectModelUrn.fromUrn( violation.context().element().getURI() ) );
      return violationError;
   }

   @Override
   public ViolationError visitNodeKindViolation( final NodeKindViolation violation ) {
      final ViolationError violationError = visit( violation );
      violationError.setFocusNode( AspectModelUrn.fromUrn( violation.context().element().getURI() ) );
      return violationError;
   }

   @Override
   public ViolationError visitPatternViolation( final PatternViolation violation ) {
      final ViolationError violationError = visit( violation );
      violationError.setFocusNode( AspectModelUrn.fromUrn( violation.context().element().getURI() ) );
      return violationError;
   }

   @Override
   public ViolationError visitJsViolation( final JsConstraintViolation violation ) {
      final ViolationError violationError = visit( violation );
      violationError.setFocusNode( AspectModelUrn.fromUrn( violation.context().element().getURI() ) );
      return violationError;
   }

   @Override
   public ViolationError visitUniqueLanguageViolation( final UniqueLanguageViolation violation ) {
      final ViolationError violationError = visit( violation );
      violationError.setFocusNode( AspectModelUrn.fromUrn( violation.context().element().getURI() ) );
      return violationError;
   }

   @Override
   public ViolationError visitEqualsViolation( final EqualsViolation violation ) {
      final ViolationError violationError = visit( violation );
      violationError.setFocusNode( AspectModelUrn.fromUrn( violation.context().element().getURI() ) );
      return violationError;
   }

   @Override
   public ViolationError visitDisjointViolation( final DisjointViolation violation ) {
      final ViolationError violationError = visit( violation );
      violationError.setFocusNode( AspectModelUrn.fromUrn( violation.context().element().getURI() ) );
      return violationError;
   }

   @Override
   public ViolationError visitLessThanViolation( final LessThanViolation violation ) {
      final ViolationError violationError = visit( violation );
      violationError.setFocusNode( AspectModelUrn.fromUrn( violation.context().element().getURI() ) );
      return violationError;
   }

   @Override
   public ViolationError visitLessThanOrEqualsViolation( final LessThanOrEqualsViolation violation ) {
      final ViolationError violationError = visit( violation );
      violationError.setFocusNode( AspectModelUrn.fromUrn( violation.context().element().getURI() ) );
      return violationError;
   }

   @Override
   public ViolationError visitValueFromListViolation( final ValueFromListViolation violation ) {
      final ViolationError violationError = visit( violation );
      violationError.setFocusNode( AspectModelUrn.fromUrn( violation.context().element().getURI() ) );
      return violationError;
   }

   @Override
   public ViolationError visitClosedViolation( final ClosedViolation violation ) {
      final ViolationError violationError = visit( violation );
      violationError.setFocusNode( AspectModelUrn.fromUrn( violation.context().element().getURI() ) );
      return violationError;
   }

   @Override
   public ViolationError visitNotViolation( final NotViolation violation ) {
      final ViolationError violationError = visit( violation );
      violationError.setFocusNode( AspectModelUrn.fromUrn( violation.context().element().getURI() ) );
      return violationError;
   }

   public ViolationError visitInvalidSyntaxViolation( final String invalidSyntaxViolation ) {
      return new ViolationError( invalidSyntaxViolation );
   }
}
