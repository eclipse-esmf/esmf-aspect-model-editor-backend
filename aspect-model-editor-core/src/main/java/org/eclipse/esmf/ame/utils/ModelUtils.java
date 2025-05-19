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

package org.eclipse.esmf.ame.utils;

import java.io.File;

import org.eclipse.esmf.ame.exceptions.FileHandlingException;

/**
 * Utility class for handling model-related operations.
 * Provides methods for sanitizing file information to prevent path traversal attacks.
 */
public class ModelUtils {

   private ModelUtils() {
   }

   /**
    * Sanitizes the file name to remove any path information and retain only the base file name.
    * This method is used to ensure that the file name does not contain any directory path components,
    * which helps prevent path traversal attacks. It extracts only the file name portion from a given
    * string that may represent a path.
    *
    * @param fileInformation The file name string potentially including path information.
    * @return The sanitized base file name without any path components.
    * @throws FileHandlingException If the file contains path information´s.
    */
   public static String sanitizeFileInformation( final String fileInformation ) {
      if ( fileInformation.contains( File.separator ) || fileInformation.contains( ".." ) ) {
         throw new FileHandlingException(
               "Invalid file information: The provided string must not contain directory separators or relative path components." );
      }

      return new File( fileInformation ).getName();
   }
}
