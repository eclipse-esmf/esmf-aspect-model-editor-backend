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

package org.eclipse.esmf.ame.services.validation;

import java.util.List;
import java.util.function.Predicate;

import org.eclipse.esmf.ame.exceptions.FileReadException;
import org.eclipse.esmf.aspectmodel.AspectModelFile;
import org.eclipse.esmf.aspectmodel.Violation;
import org.eclipse.esmf.samm.KnownVersion;

import jakarta.inject.Singleton;
import org.apache.jena.rdf.model.Model;

/**
 * Service for violation handling and SAMM-related operations.
 */
@Singleton
public class ValidationOperations {

   /**
    * Checks if any violation in the list matches the given predicate.
    *
    * @param violations list of violations to check
    * @param predicate predicate to test violations against
    * @return true if any violation matches the predicate
    */
   public boolean hasViolationMatching( final List<Violation> violations, final Predicate<Violation> predicate ) {
      return violations.stream().anyMatch( predicate );
   }

   /**
    * Throws the given exception if any violation in the list matches the predicate.
    *
    * @param violations the list of Violation objects to check
    * @param predicate the predicate to filter violations
    * @param exception the exception to throw if a matching violation is found
    */
   public void throwIfViolationMatches( final List<Violation> violations,
         final Predicate<Violation> predicate, final RuntimeException exception ) {
      violations.stream()
            .filter( predicate )
            .findFirst()
            .ifPresent( v -> { throw exception; } );
   }


   /**
    * Extracts the SAMM (Semantic Aspect Meta Model) version from an AspectModelFile.
    * This method retrieves the SAMM namespace URI from the model's source and parses
    * the version number using a regular expression pattern.
    *
    * @param aspectModelFile the aspect model file from which to extract the SAMM version
    * @return the SAMM version as a string (e.g., "2.2.0")
    * @throws FileReadException if the SAMM version cannot be found or is invalid
    */
   public String extractSammVersion( final AspectModelFile aspectModelFile ) {
      final Model sourceModel = aspectModelFile.sourceModel();
      final String sammPrefixUri = sourceModel.getNsPrefixURI( "samm" );

      if ( sammPrefixUri == null ) {
         final String fileName = aspectModelFile.filename().orElse( "unknown" );
         throw new FileReadException( String.format( "SAMM prefix '@prefix samm:' not found in model file '%s'", fileName ) );
      }

      return KnownVersion.fromVersionString(
                  sammPrefixUri.replaceAll( ".*meta-model:([\\d.]+)#", "$1" ) )
            .map( KnownVersion::toVersionString )
            .orElseThrow( () -> new FileReadException( String.format( "Unsupported or invalid SAMM meta-model version in prefix URI '%s' for file '%s'",
                  sammPrefixUri, aspectModelFile.filename().orElse( "unknown" ) ) ) );
   }
}

