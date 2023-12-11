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

import javax.annotation.Nonnull;

import org.eclipse.esmf.ame.resolver.strategy.model.FolderStructure;
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
}
