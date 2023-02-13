/*
 * Copyright (c) 2022 Robert Bosch Manufacturing Solutions GmbH
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

package io.openmanufacturing.ame.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.openmanufacturing.ame.exceptions.UrnNotFoundException;
import io.openmanufacturing.ame.model.validation.ViolationError;
import io.openmanufacturing.ame.services.utils.ModelUtils;
import io.openmanufacturing.sds.aspectmodel.shacl.fix.Fix;
import io.openmanufacturing.sds.aspectmodel.shacl.violation.ClassTypeViolation;
import io.openmanufacturing.sds.aspectmodel.shacl.violation.ClosedViolation;
import io.openmanufacturing.sds.aspectmodel.shacl.violation.DatatypeViolation;
import io.openmanufacturing.sds.aspectmodel.shacl.violation.DisjointViolation;
import io.openmanufacturing.sds.aspectmodel.shacl.violation.EqualsViolation;
import io.openmanufacturing.sds.aspectmodel.shacl.violation.InvalidValueViolation;
import io.openmanufacturing.sds.aspectmodel.shacl.violation.JsConstraintViolation;
import io.openmanufacturing.sds.aspectmodel.shacl.violation.LanguageFromListViolation;
import io.openmanufacturing.sds.aspectmodel.shacl.violation.LessThanOrEqualsViolation;
import io.openmanufacturing.sds.aspectmodel.shacl.violation.LessThanViolation;
import io.openmanufacturing.sds.aspectmodel.shacl.violation.MaxCountViolation;
import io.openmanufacturing.sds.aspectmodel.shacl.violation.MaxExclusiveViolation;
import io.openmanufacturing.sds.aspectmodel.shacl.violation.MaxInclusiveViolation;
import io.openmanufacturing.sds.aspectmodel.shacl.violation.MaxLengthViolation;
import io.openmanufacturing.sds.aspectmodel.shacl.violation.MinCountViolation;
import io.openmanufacturing.sds.aspectmodel.shacl.violation.MinExclusiveViolation;
import io.openmanufacturing.sds.aspectmodel.shacl.violation.MinInclusiveViolation;
import io.openmanufacturing.sds.aspectmodel.shacl.violation.MinLengthViolation;
import io.openmanufacturing.sds.aspectmodel.shacl.violation.NodeKindViolation;
import io.openmanufacturing.sds.aspectmodel.shacl.violation.NotViolation;
import io.openmanufacturing.sds.aspectmodel.shacl.violation.PatternViolation;
import io.openmanufacturing.sds.aspectmodel.shacl.violation.ProcessingViolation;
import io.openmanufacturing.sds.aspectmodel.shacl.violation.SparqlConstraintViolation;
import io.openmanufacturing.sds.aspectmodel.shacl.violation.UniqueLanguageViolation;
import io.openmanufacturing.sds.aspectmodel.shacl.violation.ValueFromListViolation;
import io.openmanufacturing.sds.aspectmodel.shacl.violation.Violation;
import io.openmanufacturing.sds.aspectmodel.urn.AspectModelUrn;

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
