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
import io.openmanufacturing.sds.aspectmodel.shacl.violation.ProcessingViolation;
import io.openmanufacturing.sds.aspectmodel.shacl.violation.SparqlConstraintViolation;
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
            || ModelUtils.isProcessingViolationViolation().test( violation ) ).toList();
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

   public ViolationError visitInvalidSyntaxViolation( final String invalidSyntaxViolation ) {
      return new ViolationError( invalidSyntaxViolation );
   }
}
