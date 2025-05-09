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

package org.eclipse.esmf.ame.services.utils;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.esmf.ame.services.models.Model;
import org.eclipse.esmf.ame.services.models.Version;

/**
 * A utility class for grouping model URIs by namespace and version.
 */
public class ModelGroupingUtils {

   private final Path modelPath;

   /**
    * Constructs a ModelGrouper with the given base model path.
    *
    * @param modelPath the base path to relativize URIs against
    */
   public ModelGroupingUtils( final Path modelPath ) {
      this.modelPath = modelPath;
   }

   /**
    * Groups model URIs by namespace and version, setting the existing field as specified.
    *
    * @param uriStream a stream of model URIs
    * @return a map where the keys are namespaces and the values are lists of maps containing versions and their associated models
    */
   public Map<String, List<Version>> groupModelsByNamespaceAndVersion( final Stream<URI> uriStream ) {
      return uriStream.map( this::relativizePath ).map( this::splitPath ).collect(
            Collectors.groupingBy( this::extractNamespace, TreeMap::new, Collectors.collectingAndThen(
                  Collectors.groupingBy( this::extractVersion,
                        Collectors.mapping(
                              parts -> createModel( parts,
                                    modelPath.resolve( Paths.get( parts[0], parts[1], parts[2] ) ).toFile().exists() ),
                              Collectors.toList() ) ),
                  this::convertAndSortVersionMap ) ) );
   }

   /**
    * Relativizes the given URI against the base model path.
    *
    * @param uri the URI to relativize
    * @return the relativized path as a string
    */
   private String relativizePath( final URI uri ) {
      return modelPath.relativize( Path.of( uri ) ).toString();
   }

   /**
    * Splits the given path string into parts using the file separator.
    *
    * @param path the path string to split
    * @return an array of path parts
    */
   private String[] splitPath( final String path ) {
      return path.split( Pattern.quote( File.separator ) );
   }

   /**
    * Extracts the namespace from the given path parts.
    *
    * @param parts an array of path parts
    * @return the namespace (first part of the path)
    */
   private String extractNamespace( final String[] parts ) {
      return parts[0];
   }

   /**
    * Extracts the version from the given path parts.
    *
    * @param parts an array of path parts
    * @return the version (second part of the path)
    */
   private String extractVersion( final String[] parts ) {
      return parts[1];
   }

   /**
    * Creates a map representing a model from the given path parts, setting the existing field as specified.
    *
    * @param parts an array of path parts
    * @param existing whether to set the existing field to true
    * @return a map containing the model information
    */
   private Model createModel( final String[] parts, final boolean existing ) {
      return new Model( parts[2], existing );
   }

   /**
    * Converts a version-to-models map into a list of sorted Version objects.
    *
    * @param versionMap a map where keys are versions and values are lists of Model objects
    * @return a list of Version objects sorted by semantic version
    */
   private List<Version> convertAndSortVersionMap( final Map<String, List<Model>> versionMap ) {
      return versionMap.entrySet().stream().sorted( Map.Entry.comparingByKey( this::compareSemanticVersions ) )
            .map( entry -> new Version( entry.getKey(), sortModelsAlphabetically( entry.getValue() ) ) ).collect( Collectors.toList() );
   }

   /**
    * Sorts a list of models alphabetically by their names.
    *
    * @param models the list of models to sort
    * @return a sorted list of models
    */
   private List<Model> sortModelsAlphabetically( final List<Model> models ) {
      return models.stream().sorted( Comparator.comparing( Model::getModel ) ).collect( Collectors.toList() );
   }

   /**
    * Compares two semantic version strings (e.g., "1.0.0" and "1.0.1").
    *
    * @param v1 the first version string
    * @param v2 the second version string
    * @return a negative number if v1 < v2, zero if v1 == v2, or a positive number if v1 > v2
    */
   private int compareSemanticVersions( final String v1, final String v2 ) {
      final String[] parts1 = v1.split( "\\." );
      final String[] parts2 = v2.split( "\\." );
      for ( int i = 0; i < Math.max( parts1.length, parts2.length ); i++ ) {
         final int part1 = i < parts1.length ? Integer.parseInt( parts1[i] ) : 0;
         final int part2 = i < parts2.length ? Integer.parseInt( parts2[i] ) : 0;
         final int comparison = Integer.compare( part1, part2 );
         if ( comparison != 0 ) {
            return comparison;
         }
      }
      return 0;
   }
}
