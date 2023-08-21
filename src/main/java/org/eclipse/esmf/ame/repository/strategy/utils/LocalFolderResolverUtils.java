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

package org.eclipse.esmf.ame.repository.strategy.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import javax.annotation.Nonnull;

import org.apache.commons.io.FileUtils;
import org.eclipse.esmf.ame.exceptions.FileNotFoundException;
import org.eclipse.esmf.ame.exceptions.InvalidAspectModelException;
import org.eclipse.esmf.ame.model.resolver.FolderStructure;
import org.eclipse.esmf.aspectmodel.urn.AspectModelUrn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalFolderResolverUtils {

   private static final Logger LOG = LoggerFactory.getLogger( LocalFolderResolverUtils.class );

   private LocalFolderResolverUtils() {
   }

   public static final String NAMESPACE_VERSION_NAME_SEPARATOR = ":";

   /**
    * This method will extract namespace, version and name from path based on ':'.
    */
   public static FolderStructure extractFilePath( @Nonnull final String path ) {
      final String[] splitNamespace = path.split( NAMESPACE_VERSION_NAME_SEPARATOR );

      return switch ( splitNamespace.length ) {
         case 1 -> new FolderStructure( path );
         case 2 -> extractNamespaceVersion( splitNamespace );
         default -> new FolderStructure();
      };
   }

   /**
    * Split the given path in path, version and filename based on ':'.
    * ex: org.eclipse.esmf.samm:1.0.0
    *
    * @param path - path of the current ttl.
    */
   private static FolderStructure extractNamespaceVersion( final String[] path ) {
      return new FolderStructure( path[0], path[1] );
   }

   /**
    * This method will build the path of the file.
    *
    * @param namespace - namespace of the aspect model.
    * @param fileName - name of the file.
    * @return path of the file.
    */
   public static String buildFilePath( final String namespace, final String fileName ) {
      final FolderStructure folderStructure = LocalFolderResolverUtils.extractFilePath( namespace );
      folderStructure.setFileName( fileName );
      return folderStructure.toString();
   }

   /**
    * Reads the content of a file located at the specified path using the provided character encoding.
    *
    * @param path The path to the file to be read.
    * @param charset The character encoding to be used for decoding the file content.
    * @return The content of the file as a string decoded with the specified character encoding.
    * @throws IOException If an I/O error occurs while reading the file.
    */
   public static String readString( Path path, Charset charset) throws IOException {
      try ( InputStream inputStream = Files.newInputStream(path)) {
         byte[] bytes = inputStream.readAllBytes();
         return new String(bytes, charset);
      }
   }
}
