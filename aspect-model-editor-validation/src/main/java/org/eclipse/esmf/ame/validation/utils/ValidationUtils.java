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

package org.eclipse.esmf.ame.validation.utils;

import java.util.List;
import java.util.function.Predicate;

import org.apache.jena.riot.RiotException;
import org.eclipse.esmf.ame.validation.model.ViolationReport;
import org.eclipse.esmf.ame.validation.services.ViolationFormatter;
import org.eclipse.esmf.aspectmodel.resolver.services.VersionedModel;
import org.eclipse.esmf.aspectmodel.shacl.violation.InvalidSyntaxViolation;
import org.eclipse.esmf.aspectmodel.shacl.violation.ProcessingViolation;
import org.eclipse.esmf.aspectmodel.shacl.violation.Violation;
import org.eclipse.esmf.aspectmodel.validation.services.AspectModelValidator;

import io.vavr.control.Try;

public class ValidationUtils {
   /**
    * Validates a versioned model in memory using a specified aspect model validator.
    * This method checks the model for any violations according to the rules defined in the provided validator.
    * If violations are found, they are formatted and added to a ViolationReport object.
    * In case of a syntax error or other processing issues, an invalid syntax violation is added to the report.
    *
    * @param versionedModel The versioned model to be validated, wrapped in a Try for safe exception handling.
    * @param aspectModelValidator The validator used to check the model for compliance with specific rules.
    * @return A ViolationReport object containing all the violations found during validation.
    *       If no violations are found, this report will be empty.
    *
    * @throws RiotException If a syntax error is encountered during the validation
    *       process, a RiotException is thrown and caught within the
    *       method. The exception's message is then added to the ViolationReport.
    */
   public static ViolationReport validateModel( Try<VersionedModel> versionedModel,
         final AspectModelValidator aspectModelValidator ) {
      final ViolationReport violationReport = new ViolationReport();

      try {
         final List<Violation> violations = aspectModelValidator.validateModel( versionedModel );

         violationReport.setViolationErrors( new ViolationFormatter().apply( violations ) );

         return violationReport;
      } catch ( final RiotException riotException ) {
         violationReport.addViolation(
               new ViolationFormatter().visitInvalidSyntaxViolation( riotException.getMessage() ) );

         return violationReport;
      }
   }

   /**
    * Returns a predicate that tests if a given violation is an invalid syntax violation.
    *
    * @return Predicate<Violation> that can be used to filter invalid syntax violations.
    */
   public static Predicate<Violation> isInvalidSyntaxViolation() {
      return violation -> violation.errorCode() != null && violation.errorCode()
                                                                    .equals( InvalidSyntaxViolation.ERROR_CODE );
   }

   /**
    * Returns a predicate that tests if a given violation is a processing violation.
    *
    * @return Predicate<Violation> that can be used to filter processing violations.
    */
   public static Predicate<Violation> isProcessingViolation() {
      return violation -> violation.errorCode() != null && violation.errorCode()
                                                                    .equals( ProcessingViolation.ERROR_CODE );
   }
}
