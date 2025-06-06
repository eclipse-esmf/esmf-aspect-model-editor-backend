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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.esmf.ame.services.models.Model;
import org.eclipse.esmf.ame.services.models.Version;
import org.eclipse.esmf.aspectmodel.loader.AspectModelLoader;
import org.eclipse.esmf.metamodel.ModelElement;

/**
 * A utility class for grouping model URIs by namespace and version.
 */
public class ModelGroupingUtils {
   private final AspectModelLoader aspectModelLoader;

   /**
    * Constructs a ModelGrouper with the given base model path.
    *
    * @param aspectModelLoader the loader for aspect models
    */
   public ModelGroupingUtils( final AspectModelLoader aspectModelLoader ) {
      this.aspectModelLoader = aspectModelLoader;
   }

   /**
    * Groups model URIs by namespace and version, setting the existing field as specified.
    *
    * @param uriStream a stream of model URIs
    * @return a map where the keys are namespaces and the values are lists of maps containing versions and their associated models
    */
   public Map<String, List<Version>> groupModelsByNamespaceAndVersion( final Stream<URI> uriStream ) {
      return aspectModelLoader.load( uriStream.map( File::new ).toList() ).files().stream()
            .flatMap( aspectModelFile -> aspectModelFile.aspects().stream().map( ModelElement.class::cast ).findFirst().or( () ->
                  aspectModelFile.elements().stream().filter( modelElement -> !modelElement.isAnonymous() ).findAny() ).stream() )
            .map( modelElement -> new Model( modelElement.getSourceFile().filename().orElse( "unnamed file" ), modelElement.urn(), true ) )
            .collect( Collectors.groupingBy( model -> model.getAspectModelUrn().getNamespaceMainPart() ) )
            .entrySet().stream().sorted( Map.Entry.comparingByKey() )
            .map( modelsByNamespaceEntry -> Map.entry( modelsByNamespaceEntry.getKey(),
                  modelsByNamespaceEntry.getValue().stream()
                        .collect( Collectors.groupingBy( model -> model.getAspectModelUrn().getVersion() ) )
                        .entrySet().stream().sorted( Map.Entry.comparingByKey() )
                        .map( modelsByVersion -> new Version( modelsByVersion.getKey(),
                              modelsByVersion.getValue().stream().sorted( Comparator.comparing( Model::getModel ) ).toList() ) )
                        .toList() ) )
            .collect( Collectors.toMap( Map.Entry::getKey, Map.Entry::getValue, ( v1, v2 ) -> {
               throw new RuntimeException( String.format( "Duplicate key for values %s and %s", v1, v2 ) );
            }, LinkedHashMap::new ) );
   }
}
