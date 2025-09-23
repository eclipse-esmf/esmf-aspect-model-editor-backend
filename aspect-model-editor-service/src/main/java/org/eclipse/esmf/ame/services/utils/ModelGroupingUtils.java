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
import java.util.AbstractMap;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.esmf.ame.services.models.Model;
import org.eclipse.esmf.ame.services.models.Version;
import org.eclipse.esmf.aspectmodel.AspectModelFile;
import org.eclipse.esmf.aspectmodel.loader.AspectModelLoader;
import org.eclipse.esmf.aspectmodel.resolver.AspectModelFileLoader;
import org.eclipse.esmf.aspectmodel.urn.AspectModelUrn;
import org.eclipse.esmf.metamodel.AspectModel;
import org.eclipse.esmf.metamodel.ModelElement;
import org.eclipse.esmf.metamodel.vocabulary.SammNs;
import org.eclipse.esmf.samm.KnownVersion;

import io.vavr.control.Try;

/**
 * A utility class for grouping model URIs by namespace and version.
 *
 * @param aspectModelLoader the loader for aspect models
 */
public record ModelGroupingUtils( AspectModelLoader aspectModelLoader ) {
   /**
    * Constructs a ModelGrouper with the given base model path.
    */
   public ModelGroupingUtils {
   }

   /**
    * Groups model URIs by namespace and version, setting the existing field as specified.
    *
    * @param uriStream a stream of model URIs
    * @param onlyAspectModels get only Aspect Models with Aspects as namespace list.
    * @return a map where the keys are namespaces and the values are lists of maps containing versions and their associated models
    */
   public Map<String, List<Version>> groupModelsByNamespaceAndVersion( final Stream<URI> uriStream, final boolean onlyAspectModels ) {
      return uriStream.map( File::new )
            .map( file -> {
               final Optional<KnownVersion> metaModelVersionFromFile = Try.of(
                           () -> AspectModelFileLoader.load( file ).sourceModel().getNsPrefixMap().get( SammNs.SAMM.getShortForm() ) )
                     .flatMap( AspectModelUrn::from )
                     .toJavaOptional()
                     .map( AspectModelUrn::getVersion )
                     .flatMap( KnownVersion::fromVersionString );
               final AspectModel loadedModel = aspectModelLoader.load( file );
               return new AbstractMap.SimpleEntry<>( loadedModel, metaModelVersionFromFile );
            } )
            .flatMap( entry ->
                  entry.getKey().files().stream()
                        .flatMap( file -> extractModelElement( file, onlyAspectModels ) )
                        .map( modelElement -> createModel( modelElement, entry.getValue().orElse( null ) ) )
            )
            .collect( Collectors.groupingBy(
                  model -> model.getAspectModelUrn().getNamespaceMainPart()
            ) )
            .entrySet().stream()
            .sorted( Map.Entry.comparingByKey() )
            .collect( Collectors.toMap(
                  Map.Entry::getKey,
                  entry -> groupByVersion( entry.getValue() ),
                  ( v1, v2 ) -> {
                     throw new RuntimeException( String.format( "Duplicate key for values %s and %s", v1, v2 ) );
                  },
                  LinkedHashMap::new
            ) );
   }

   private Stream<ModelElement> extractModelElement( final AspectModelFile file, final boolean onlyAspectModels ) {
      if ( onlyAspectModels ) {
         return file.aspects().stream().map( ModelElement.class::cast ).findFirst().stream();
      }

      return file.aspects().stream().map( ModelElement.class::cast ).findFirst()
            .or( () -> file.elements().stream().filter( element -> !element.isAnonymous() ).findAny() ).stream();
   }

   private Model createModel( final ModelElement element, final KnownVersion version ) {
      final String filename = element.getSourceFile().filename().orElse( "unnamed file" );
      return new Model( filename, element.urn(), version.toVersionString(), true );
   }

   private List<Version> groupByVersion( final List<Model> models ) {
      return models.stream().collect( Collectors.groupingBy( model -> model.getAspectModelUrn().getVersion() ) ).entrySet().stream()
            .sorted( Map.Entry.comparingByKey() ).map( entry -> new Version( entry.getKey(),
                  entry.getValue().stream().sorted( Comparator.comparing( Model::getModel ) ).toList() ) ).toList();
   }
}
