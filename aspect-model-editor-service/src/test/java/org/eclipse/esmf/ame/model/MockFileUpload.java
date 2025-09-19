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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Optional;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.MediaType;
import io.micronaut.http.multipart.CompletedFileUpload;

public class MockFileUpload implements CompletedFileUpload {
   private final String filename;
   private final MediaType mediaType;
   private final byte[] content;

   public MockFileUpload( final String filename, final byte[] content, final MediaType mediaType ) {
      this( filename, mediaType, content );
   }

   public MockFileUpload( final String filename, final MediaType mediaType, @Nullable final byte[] content ) {
      this.filename = filename;
      this.mediaType = mediaType;
      this.content = ( content != null ? content : new byte[0] );
   }

   @Override
   public InputStream getInputStream() {
      return new ByteArrayInputStream( content );
   }

   @Override
   public byte[] getBytes() {
      return content;
   }

   @Override
   public ByteBuffer getByteBuffer() {
      return ByteBuffer.wrap( content );
   }

   @Override
   public Optional<MediaType> getContentType() {
      return Optional.of( mediaType );
   }

   @Override
   public String getName() {
      return filename;
   }

   @Override
   public String getFilename() {
      return filename;
   }

   @Override
   public long getSize() {
      return content.length;
   }

   @Override
   public long getDefinedSize() {
      return content.length;
   }

   @Override
   public boolean isComplete() {
      return true;
   }
}
