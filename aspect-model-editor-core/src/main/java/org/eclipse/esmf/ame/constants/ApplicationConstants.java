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

package org.eclipse.esmf.ame.constants;

/**
 * Central constants class for the Aspect Model Editor application.
 * Contains commonly used string constants across different modules.
 */
public final class ApplicationConstants {

   private ApplicationConstants() {
   }

   /**
    * HTTP Header and Query Parameter names
    */
   public static final class Headers {
      public static final String URI = "uri";
      public static final String URN = "aspect-model-urn";

      private Headers() {
      }
   }

   /**
    * File extensions
    */
   public static final class FileExtensions {
      public static final String TTL = ".ttl";
      public static final String ZIP = ".zip";

      private FileExtensions() {
      }
   }

   /**
    * Directory names
    */
   public static final class Directories {
      public static final String ASPECT_MODEL_EDITOR = "aspect-model-editor";
      public static final String MODELS = "models";

      private Directories() {
      }
   }

   /**
    * Error messages
    */
   public static final class ErrorMessages {
      public static final String ASPECT_MODEL_NOT_FOUND = "Aspect Model not found";
      public static final String SPECIFY_ASPECT_MODEL_URN = "Please specify an aspect model urn";
      public static final String INVALID_URI_FORMAT = "Please define a file URI in the request header";
      public static final String SAMM_STRUCTURE_INFO = "Please check whether the SAMM structure has been followed in the workspace: "
            + "Namespace/Version/Aspect model.";

      private ErrorMessages() {
      }
   }

   /**
    * Output format types
    */
   public static final class OutputFormats {
      public static final String JSON = "json";
      public static final String YAML = "yaml";

      private OutputFormats() {
      }
   }

   /**
    * URI scheme constants
    */
   public static final class Schemes {
      public static final String BLOB = "blob";
      public static final String FILE = "file";

      private Schemes() {
      }
   }
}

