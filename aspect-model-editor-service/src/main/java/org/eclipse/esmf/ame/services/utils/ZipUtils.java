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

package org.eclipse.esmf.ame.services.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.eclipse.esmf.ame.exceptions.CreateFileException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for creating zip packages.
 */
public class ZipUtils {
   private static final Logger LOG = LoggerFactory.getLogger( ZipUtils.class );

   private ZipUtils() {
   }

   public static byte[] createPackage( final Map<Path, byte[]> content ) {
      final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

      try ( final ZipOutputStream zos = new ZipOutputStream( outputStream ) ) {
         for ( final Map.Entry<Path, byte[]> entry : content.entrySet() ) {
            final ZipEntry zipEntry = new ZipEntry( entry.getKey().toString() );
            zos.putNextEntry( zipEntry );

            final byte[] bytes = entry.getValue();
            zos.write( bytes, 0, bytes.length );

            zos.closeEntry();
         }
      } catch ( final IOException e ) {
         LOG.error( "Failed to create the zip file." );
         throw new CreateFileException( "An error occurred while creating the zip file.", e );
      }

      return outputStream.toByteArray();
   }
}
