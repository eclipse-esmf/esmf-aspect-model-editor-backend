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

package org.eclipse.esmf.ame;

import io.micronaut.http.MediaType;

/**
 * A utility class that extends the functionality of {@link MediaType} by providing additional media type constants.
 * This class includes constants for media types such as `application/schema+json`, `application/aasx`, `text/turtle`,
 * `image/svg+xml`, and `application/zip`.
 *
 * <p>Note: This class is not meant to be instantiated.</p>
 */
public final class MediaTypeExtension {
   /**
    * Public constant media type for {@code application/schema+json}.
    */
   public static final MediaType APPLICATION_SCHEMA_JSON;

   /**
    * Public constant media type for {@code application/aasx}.
    */
   public static final String APPLICATION_AASX = "application/aasx";

   /**
    * A String equivalent of {@link MediaTypeExtension#TEXT_TURTLE} which is a new {@link MediaType}.
    */
   public static final String TEXT_TURTLE_VALUE = "text/turtle";

   /**
    * Public constant media type for {@code text/turtle}.
    */
   public static final MediaType TEXT_TURTLE;

   /**
    * Public constant media type for {@code image/svg+xml}.
    */
   public static final MediaType IMAGE_SVG;

   public static final String APPLICATION_ZIP_VALUE = "application/zip";

   /**
    * Public constant media type for {@code application/zip}.
    */
   public static final MediaType APPLICATION_ZIP;

   static {
      TEXT_TURTLE = new MediaType( "text/turtle" );
      IMAGE_SVG = new MediaType( "image/svg+xml" );
      APPLICATION_SCHEMA_JSON = new MediaType( "application/schema+json" );
      APPLICATION_ZIP = new MediaType( "application/zip" );
   }

   private MediaTypeExtension() {
   }
}
