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

package org.eclipse.esmf.ame.model;

import io.micronaut.core.io.buffer.ReadBuffer;
import io.micronaut.core.io.buffer.ReadBufferFactory;
import io.micronaut.http.MediaType;
import io.micronaut.http.multipart.CompletedFileUpload;
import io.micronaut.http.multipart.FormFieldMetadata;

public final class MockFileUpload {

   private MockFileUpload() {
   }

   public static CompletedFileUpload create( final String filename, final byte[] content, final MediaType mediaType ) {
      final byte[] data = content != null ? content : new byte[0];

      final FormFieldMetadata metadata = new FormFieldMetadata( filename, filename, mediaType );
      final ReadBuffer readBuffer = ReadBufferFactory.getJdkFactory().adapt( data.clone() );

      return CompletedFileUpload.ofMemory( metadata, readBuffer );
   }

   public static CompletedFileUpload create( final String filename, final MediaType mediaType, final byte[] content ) {
      return create( filename, content, mediaType );
   }
}