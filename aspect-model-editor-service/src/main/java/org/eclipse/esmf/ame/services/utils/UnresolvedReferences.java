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

package org.eclipse.esmf.ame.services.utils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.esmf.aspectmodel.ValueParsingException;
import org.eclipse.esmf.aspectmodel.resolver.ModelResolutionViolation;
import org.eclipse.esmf.aspectmodel.resolver.exceptions.ModelResolutionException;
import org.eclipse.esmf.aspectmodel.urn.AspectModelUrn;

import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * Detects Aspect Models which could not be loaded only because referenced elements are not defined in any file, and
 * describes them the same way for every endpoint (validate, format, save, ...).
 */
public final class UnresolvedReferences {
   private UnresolvedReferences() {
   }

   /**
    * The referenced elements which no file defines, if this is the only reason why the model could not be loaded.
    * Syntax or value errors anywhere in the model make the list empty, because then the model cannot be opened.
    *
    * @param throwable the exception thrown while loading the model
    * @return the sorted URNs of the unresolved elements, or an empty list
    */
   public static List<String> find( final Throwable throwable ) {
      if ( hasValueError( throwable ) ) {
         return List.of();
      }

      final ModelResolutionException mre = ExceptionUtils.throwableOfType( throwable, ModelResolutionException.class );
      if ( mre == null || mre.getCheckedLocations() == null ) {
         return List.of();
      }

      final List<ModelResolutionViolation> violations = mre.getCheckedLocations();
      final boolean hasNestedValueError = violations.stream()
            .map( ModelResolutionViolation::cause )
            .flatMap( Optional::stream )
            .anyMatch( UnresolvedReferences::hasValueError );
      if ( hasNestedValueError ) {
         return List.of();
      }

      return violations.stream()
            .map( ModelResolutionViolation::element )
            .flatMap( Optional::stream )
            .map( AspectModelUrn::getUrn )
            .map( Object::toString )
            .distinct()
            .sorted()
            .toList();
   }

   /**
    * @param unresolvedElements the URNs of the unresolved elements
    * @return one sentence per element, e.g. {@code Element 'urn:...#x' does not exist in a file.}
    */
   public static String message( final List<String> unresolvedElements ) {
      return unresolvedElements.stream()
            .map( urn -> String.format( "Element '%s' does not exist in a file.", urn ) )
            .collect( Collectors.joining( " " ) );
   }

   private static boolean hasValueError( final Throwable throwable ) {
      return ExceptionUtils.indexOfType( throwable, ValueParsingException.class ) >= 0;
   }
}
