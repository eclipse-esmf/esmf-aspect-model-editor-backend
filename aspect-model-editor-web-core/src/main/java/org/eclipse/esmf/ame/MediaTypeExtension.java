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

package org.eclipse.esmf.ame;

import org.springframework.http.MediaType;

public final class MediaTypeExtension {

   private static final String APPLICATION = "application";

   /**
    * Public constant mediat typ for {@code application/schema+json}.
    */
   public static final MediaType APPLICATION_SCHEMA_JSON;

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
    * Public constant media type for {@code "application/zip"}.
    */
   public static final MediaType APPLICATION_ZIP;

   static {
      TEXT_TURTLE = new MediaType( "text", "turtle" );
      IMAGE_SVG = new MediaType( "image", "svg+xml" );
      APPLICATION_SCHEMA_JSON = new MediaType( APPLICATION, "schema+json" );
      APPLICATION_ZIP = new MediaType( APPLICATION, "zip" );
   }

   private MediaTypeExtension() {
   }
}
