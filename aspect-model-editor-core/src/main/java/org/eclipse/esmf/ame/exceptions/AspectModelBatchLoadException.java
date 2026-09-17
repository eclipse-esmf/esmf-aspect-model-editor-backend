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

package org.eclipse.esmf.ame.exceptions;

import java.io.Serial;
import java.util.Collections;
import java.util.List;

import org.eclipse.esmf.ame.model.FileLoadError;

/**
 * Exception thrown when loading one or more Aspect Model files in a batch fails.
 * Collects errors across all requested files, providing sorted file and SourceDocument information.
 */
public class AspectModelBatchLoadException extends FileNotFoundException {
   @Serial
   private static final long serialVersionUID = 1L;

   private final List<FileLoadError> errors;

   /**
    * Constructs a new AspectModelBatchLoadException with the specified detail message and error list.
    *
    * @param message the detail message explaining which files failed to load
    * @param errors the list of collected errors across files
    */
   public AspectModelBatchLoadException( final String message, final List<FileLoadError> errors ) {
      super( message );
      this.errors = errors != null ? Collections.unmodifiableList( errors ) : Collections.emptyList();
   }

   /**
    * Constructs a new AspectModelBatchLoadException with the specified detail message, cause, and error list.
    *
    * @param message the detail message explaining which files failed to load
    * @param cause the cause of this exception
    * @param errors the list of collected errors across files
    */
   public AspectModelBatchLoadException( final String message, final Throwable cause, final List<FileLoadError> errors ) {
      super( message, cause );
      this.errors = errors != null ? Collections.unmodifiableList( errors ) : Collections.emptyList();
   }

   /**
    * Returns the collected errors for all failed files.
    *
    * @return unmodifiable list of file load errors
    */
   public List<FileLoadError> getErrors() {
      return errors;
   }
}
