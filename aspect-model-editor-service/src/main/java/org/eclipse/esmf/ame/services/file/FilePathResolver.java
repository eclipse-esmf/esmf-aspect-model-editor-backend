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

package org.eclipse.esmf.ame.services.file;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

import org.eclipse.esmf.ame.constants.ApplicationConstants;
import org.eclipse.esmf.ame.exceptions.FileHandlingException;
import org.eclipse.esmf.ame.services.models.FileEntry;
import org.eclipse.esmf.aspectmodel.urn.AspectModelUrn;

import jakarta.inject.Singleton;

/**
 * Service responsible for resolving file paths for aspect models.
 * Handles path construction based on URNs, namespaces, and versions.
 */
@Singleton
public class FilePathResolver {
   private static final Pattern NAMESPACE_PATTERN = Pattern.compile( "[A-Za-z0-9_][A-Za-z0-9_.\\-]*" );
   private static final Pattern VERSION_PATTERN = Pattern.compile( "\\d+\\.\\d+\\.\\d+([.\\-+][A-Za-z0-9.\\-+]*)?" );
   private static final Pattern FILE_NAME_PATTERN = Pattern.compile( "[^/\\\\:*?\"<>|]+\\.ttl", Pattern.CASE_INSENSITIVE );

   /**
    * Creates a file path based on the given AspectModelUrn, file name, and storage path.
    *
    * @param aspectModelUrn the Aspect Model URN
    * @param fileName the name of the Aspect Model file
    * @param storagePath the storage path where the file will be created
    * @return the resolved file path
    */
   public Path resolveFilePath( final AspectModelUrn aspectModelUrn, final String fileName, final Path storagePath ) {
      final String actualFileName = fileName.isEmpty() ? aspectModelUrn.getName() + ApplicationConstants.FileExtensions.TTL : fileName;
      return Paths.get( storagePath.toString(), aspectModelUrn.getNamespaceMainPart(),
            aspectModelUrn.getVersion(), actualFileName );
   }

   /**
    * Constructs a model file path from the given components.
    *
    * @param modelPath the base path to the model storage directory
    * @param namespace the namespace part of the model
    * @param version the version part of the model
    * @param modelName the name of the model file
    * @return the constructed Path
    */
   public Path constructModelPath( final Path modelPath, final String namespace,
         final String version, final String modelName ) {
      return Path.of( modelPath.toString(), namespace, version, modelName );
   }

   /**
    * Resolves a file path from a relative path string.
    * The relative path format is expected to be "namespace:version:filename".
    *
    * @param modelPath the base path to the model storage directory
    * @param relativePath the relative file path with colon separators
    * @return the resolved Path
    */
   public Path resolveFromRelativePath( final Path modelPath, final String relativePath ) {
      // Reject dangerous patterns early
      if ( relativePath.contains( ".." ) || relativePath.contains( "\0" ) ) {
         throw new FileHandlingException( "Invalid path: contains illegal characters or path traversal patterns" );
      }

      final Path path = Paths.get( relativePath.replace( ":", File.separator ) ).normalize();
      final String[] pathParts = StreamSupport.stream( path.spliterator(), false )
            .map( Path::toString )
            .toArray( String[]::new );

      if ( pathParts.length < 3 ) {
         throw new IllegalArgumentException(
               "Relative path must contain at least namespace, version, and filename: " + relativePath );
      }

      final Path resolved = constructModelPath( modelPath, pathParts[0], pathParts[1], pathParts[2] );

      // Validate resolved path stays within base directory
      if ( !resolved.normalize().startsWith( modelPath.normalize() ) ) {
         throw new FileHandlingException( "Path traversal attempt detected: " + relativePath );
      }

      return resolved;
   }

   /**
    * Converts a FileEntry to a resolved Path.
    *
    * @param fileEntry the file entry containing the absolute name path
    * @param modelPath the base path to the model storage directory
    * @return the resolved Path
    */
   public Path resolveFromFileEntry( final FileEntry fileEntry, final Path modelPath ) {
      if ( fileEntry.absoluteName() == null ) {
         throw new IllegalArgumentException( "FileEntry absoluteName must not be null" );
      }
      return resolveFromRelativePath( modelPath, fileEntry.absoluteName() );
   }

   /**
    * Resolves the directory {@code <modelPath>/<namespace>/<version>} from user supplied values.
    *
    * @param modelPath the base path to the model storage directory
    * @param namespace the namespace, e.g. {@code org.eclipse.example}
    * @param version the version, e.g. {@code 1.0.0}
    * @return the normalized, absolute version directory
    * @throws IllegalArgumentException if a value is invalid or the path points outside of the model storage directory
    */
   public Path resolveVersionDirectory( final Path modelPath, final String namespace, final String version ) {
      requireMatches( namespace, NAMESPACE_PATTERN, "namespace" );
      requireMatches( version, VERSION_PATTERN, "version" );
      return requireInside( modelPath, modelPath.resolve( namespace ).resolve( version ) );
   }

   /**
    * Resolves the Aspect Model file {@code <modelPath>/<namespace>/<version>/<fileName>} from user supplied values.
    *
    * @param modelPath the base path to the model storage directory
    * @param namespace the namespace of the file
    * @param version the version of the file
    * @param fileName the Turtle file name
    * @return the normalized, absolute file path
    * @throws IllegalArgumentException if a value is invalid or the path points outside of the model storage directory
    */
   public Path resolveModelFile( final Path modelPath, final String namespace, final String version, final String fileName ) {
      requireMatches( fileName, FILE_NAME_PATTERN, "file name" );
      if ( fileName.startsWith( "." ) ) {
         throw new IllegalArgumentException( "Invalid file name: " + fileName );
      }
      return requireInside( modelPath, resolveVersionDirectory( modelPath, namespace, version ).resolve( fileName ) );
   }

   private static void requireMatches( final String value, final Pattern pattern, final String label ) {
      if ( value == null || !pattern.matcher( value ).matches() ) {
         throw new IllegalArgumentException( "Invalid " + label + ": " + value );
      }
   }

   private static Path requireInside( final Path modelPath, final Path path ) {
      final Path root = modelPath.toAbsolutePath().normalize();
      final Path resolved = path.toAbsolutePath().normalize();
      if ( !resolved.startsWith( root ) || resolved.equals( root ) ) {
         throw new IllegalArgumentException( "Path is outside of the workspace: " + path );
      }
      return resolved;
   }
}
