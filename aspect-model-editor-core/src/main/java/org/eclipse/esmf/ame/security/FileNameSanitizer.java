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

package org.eclipse.esmf.ame.security;

import java.io.File;

import jakarta.inject.Singleton;

/**
 * Service responsible for sanitizing file names to prevent path traversal attacks.
 * <p>
 * This service ensures that file names do not contain directory separators or
 * relative path components that could be used for malicious purposes.
 * </p>
 */
@Singleton
public class FileNameSanitizer {

   /**
    * Sanitizes the file name to remove any path information and retain only the base file name.
    * This method is used to ensure that the file name does not contain any directory path components,
    * which helps prevent path traversal attacks. It extracts only the file name portion from a given
    * string that may represent a path.
    *
    * @param fileInformation The file name string potentially including path information.
    * @return The sanitized base file name without any path components.
    * @throws FileHandlingException If the file contains path information.
    */
   public String sanitize( final String fileInformation ) {
      if ( fileInformation == null || fileInformation.isBlank() ) {
         throw new IllegalArgumentException( "File information must not be null or empty" );
      }

      final String trimmed = fileInformation.trim();

      if ( trimmed.contains( File.separator ) || trimmed.contains( "/" )
            || trimmed.contains( "\\" ) || trimmed.contains( ".." )
            || trimmed.contains( "\0" ) || trimmed.contains( "%" ) ) {
         throw new IllegalArgumentException(
               "Invalid file information: The provided string must not contain directory separators, "
                     + "relative path components, or encoded characters." );
      }

      return new File( trimmed ).getName();
   }
}

