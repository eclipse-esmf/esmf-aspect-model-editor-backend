/*
 * Copyright (c) 2022 Robert Bosch Manufacturing Solutions GmbH
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

package io.openmanufacturing.ame.repository.strategy.utils;

import java.io.File;
import java.io.IOException;

import javax.annotation.Nonnull;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openmanufacturing.ame.exceptions.FileNotFoundException;
import io.openmanufacturing.ame.exceptions.InvalidAspectModelException;
import io.openmanufacturing.ame.model.resolver.FolderStructure;
import io.openmanufacturing.sds.aspectmodel.urn.AspectModelUrn;

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
         case 3 -> extractNamespaceVersionName( splitNamespace );
         default -> new FolderStructure();
      };
   }

   /**
    * Split the given path in path, version and filename based on ':'.
    * ex: io.openmanufacturing:1.0.0
    *
    * @param path - path of the current ttl.
    */
   private static FolderStructure extractNamespaceVersion( final String[] path ) {
      return new FolderStructure( path[0], path[1] );
   }

   /**
    * Split the given path in path, version and filename based on ':'.
    * ex: io.openmanufacturing:1.0.0:AspectDefault.ttl
    *
    * @param path - path of the current ttl.
    */
   private static FolderStructure extractNamespaceVersionName( final String[] path ) {
      return new FolderStructure( path[0], path[1], path[2] );
   }

   /**
    * This method will convert the given urn to AspectModelUrn.
    *
    * @param urn - urn of the aspect model.
    * @return AspectModelUrn.
    */
   public static AspectModelUrn convertToAspectModelUrn( final String urn ) {
      return AspectModelUrn.from( urn ).getOrElse( () -> {
         throw new InvalidAspectModelException(
               String.format( "The URN constructed from the input file path is invalid: %s", urn ) );
      } );
   }

   /**
    * This method will delete the given directory and all of its contents.
    *
    * @param storagePath - path of the directory to be deleted.
    */
   public static void deleteDirectory( final File storagePath ) {
      try {
         if ( storagePath.exists() && storagePath.isDirectory() ) {
            FileUtils.forceDelete( storagePath );
         }
      } catch ( final IOException error ) {
         LOG.error( "Cannot delete exported package folder." );
         throw new FileNotFoundException( String.format( "Unable to delete folder on: %s", storagePath ), error );
      }
   }
}
