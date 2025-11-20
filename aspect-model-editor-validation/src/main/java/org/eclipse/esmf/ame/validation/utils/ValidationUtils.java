/*
 * Copyright (c) 2025 Robert Bosch Manufacturing Solutions GmbH
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

import org.eclipse.esmf.ame.validation.model.ViolationError;
import org.eclipse.esmf.ame.validation.services.ViolationFormatter;
import org.eclipse.esmf.aspectmodel.shacl.violation.Violation;
import org.eclipse.esmf.aspectmodel.validation.InvalidSyntaxViolation;
import org.eclipse.esmf.aspectmodel.validation.ProcessingViolation;

/**
 * Utility class for validation-related operations.
 * <p>
 * This class provides methods to handle validation errors and predicates
 * for filtering specific types of violations, such as invalid syntax or processing violations.
 */
public class ValidationUtils {
   public static List<ViolationError> violationErrors( final List<Violation> violations ) {
      return new ViolationFormatter().apply( violations );
   }

   /**
    * Returns a predicate that tests if a given violation is an invalid syntax violation.
    *
    * @return {@link Predicate<Violation>} that can be used to filter invalid syntax violations.
    */
   public static Predicate<Violation> isInvalidSyntaxViolation() {
      return violation -> violation.errorCode() != null && violation.errorCode()
            .equals( InvalidSyntaxViolation.ERROR_CODE );
   }

   /**
    * Returns a predicate that tests if a given violation is a processing violation.
    *
    * @return {@link Predicate<Violation>} that can be used to filter processing violations.
    */
   public static Predicate<Violation> isProcessingViolation() {
      return violation -> violation.errorCode() != null && violation.errorCode()
            .equals( ProcessingViolation.ERROR_CODE );
   }
}
