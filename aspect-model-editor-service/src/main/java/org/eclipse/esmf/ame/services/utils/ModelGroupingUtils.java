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
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.eclipse.esmf.ame.services.models.Model;
import org.eclipse.esmf.ame.services.models.Version;
import org.eclipse.esmf.aspectmodel.AspectModelFile;
import org.eclipse.esmf.aspectmodel.loader.AspectModelLoader;
import org.eclipse.esmf.aspectmodel.resolver.AspectModelFileLoader;
import org.eclipse.esmf.aspectmodel.resolver.modelfile.RawAspectModelFile;
import org.eclipse.esmf.aspectmodel.urn.AspectModelUrn;
import org.eclipse.esmf.metamodel.ModelElement;
import org.eclipse.esmf.metamodel.vocabulary.SAMM;
import org.eclipse.esmf.metamodel.vocabulary.SAMMC;
import org.eclipse.esmf.metamodel.vocabulary.SAMME;
import org.eclipse.esmf.metamodel.vocabulary.SammNs;
import org.eclipse.esmf.samm.KnownVersion;

import io.vavr.control.Try;

/**
 * A utility class for grouping model URIs by namespace and version.
 *
 * @param aspectModelLoader the loader for aspect models
 */
public record ModelGroupingUtils(AspectModelLoader aspectModelLoader) {
   /**
    * Constructs a ModelGrouper with the given base model path.
    */
   public ModelGroupingUtils {
   }

   /**
    * Groups model URIs by namespace and version, setting the existing field as specified.
    *
    * @param uriStream        a stream of model URIs
    * @param onlyAspectModels get only Aspect Models with Aspects as namespace list.
    * @return a map where the keys are namespaces and the values are lists of maps containing versions and their associated models
    */
   public Map<String, List<Version>> groupModelsByNamespaceAndVersion( final Stream<URI> uriStream, final boolean onlyAspectModels ) {
      return this.groupModelsByNamespaceAndVersion( uriStream.map( File::new ).toList(), onlyAspectModels );
   }

   /**
    * Groups model URIs by namespace and version, setting the existing field as specified.
    *
    * @param files            a List of model Files
    * @param onlyAspectModels get only Aspect Models with Aspects as namespace list.
    * @return a map where the keys are namespaces and the values are lists of maps containing versions and their associated models
    */
   public Map<String, List<Version>> groupModelsByNamespaceAndVersion( final List<File> files, final boolean onlyAspectModels ) {
      final List<Model> allModels = loadAndExtractModels( files, onlyAspectModels );
      final Map<String, List<Model>> modelsByNamespace = groupByNamespace( allModels );

      return modelsByNamespace.entrySet().stream()
              .sorted( Map.Entry.comparingByKey() )
              .collect( Collectors.toMap(
                      Map.Entry::getKey,
                      entry -> groupByVersion( entry.getValue() ),
                      this::throwOnDuplicateKey,
                      LinkedHashMap::new ) );
   }

   private List<Model> loadAndExtractModels( final List<File> files, final boolean onlyAspectModels ) {
      return files.stream()
              .map( this::loadModelWithVersion )
              .flatMap( entry -> extractModelsFromEntry( entry, onlyAspectModels ) )
              .toList();
   }

   private Map.Entry<RawAspectModelFile, Optional<KnownVersion>> loadModelWithVersion( final File file ) {
      final RawAspectModelFile rawFile = AspectModelFileLoader.load( file );
      final Optional<KnownVersion> metaModelVersion = extractMetaModelVersion( rawFile );

      return new AbstractMap.SimpleEntry<>( rawFile, metaModelVersion );
   }

   private Optional<KnownVersion> extractMetaModelVersion( final RawAspectModelFile rawFile ) {
      return Try.of( () -> rawFile.sourceModel().getNsPrefixMap().get( SammNs.SAMM.getShortForm() ) )
              .flatMap( AspectModelUrn::from )
              .toJavaOptional()
              .map( AspectModelUrn::getVersion )
              .flatMap( KnownVersion::fromVersionString );
   }

   private Stream<Model> extractModelsFromEntry(final Map.Entry<RawAspectModelFile, Optional<KnownVersion>> entry, final boolean onlyAspectModels) {
      final KnownVersion version = entry.getValue()
              .orElseThrow(() -> new IllegalStateException("Meta model version is required"));
      final RawAspectModelFile rawFile = entry.getKey();

      final String filename = extractFilename(rawFile);
      final List<Resource> resources = collectMetaModelResources(version);
      final Resource firstNonBlankSubject = findFirstNonBlankSubject(rawFile.sourceModel(), resources, filename);

      final Model model = new Model(filename, AspectModelUrn.fromUrn(firstNonBlankSubject.getURI()),
              version.toVersionString(), true);

      return Stream.of(model);
   }

   private String extractFilename(final RawAspectModelFile rawFile) {
      return rawFile.sourceLocation()
              .map(uri -> Path.of(uri).getFileName().toString())
              .orElse("unnamed file");
   }

   private List<Resource> collectMetaModelResources(final KnownVersion version) {
      final SAMM samm = new SAMM(version);
      final SAMMC sammc = new SAMMC(version);
      final SAMME samme = new SAMME(version, samm);

      return Stream.of(
              Stream.of(samm.Aspect(), samm.Property(), samm.Operation(), samm.Event(),
                      samm.Entity(), samm.Value(), samm.Characteristic(), samm.Constraint(),
                      samm.AbstractEntity(), samm.AbstractProperty()),
              samme.allEntities(),
              sammc.allCharacteristics(),
              sammc.allConstraints(),
              sammc.allCollections()
      ).flatMap(s -> s).toList();
   }

   private Resource findFirstNonBlankSubject(final org.apache.jena.rdf.model.Model sourceModel, final List<Resource> resources, final String filename) {
      return resources.stream()
              .flatMap(resource -> sourceModel.listStatements(null, RDF.type, resource).toList().stream())
              .map(Statement::getSubject)
              .filter(subject -> !subject.isAnon())
              .findFirst()
              .orElseThrow(() -> new IllegalStateException("No non-blank subject found in " + filename));
   }

   private Map<String, List<Model>> groupByNamespace( final List<Model> models ) {
      return models.stream()
              .collect( Collectors.groupingBy( model ->
                      model.aspectModelUrn().getNamespaceMainPart() ) );
   }

   private Stream<ModelElement> extractModelElement( final AspectModelFile file, final boolean onlyAspectModels ) {

      final Optional<ModelElement> aspectElement = file.aspects().stream()
              .map( ModelElement.class::cast )
              .findFirst();

      if ( onlyAspectModels ) {
         return aspectElement.stream();
      }

      return aspectElement
              .or( () -> findFirstNonAnonymousElement( file ) )
              .stream();
   }

   private Optional<ModelElement> findFirstNonAnonymousElement( final AspectModelFile file ) {
      return file.elements().stream()
              .filter( element -> !element.isAnonymous() )
              .findAny();
   }

   private List<Version> groupByVersion( final List<Model> models ) {
      final Map<AspectModelUrn, Model> uniqueModels = removeDuplicateModels( models );
      final Map<String, List<Model>> modelsByVersion = groupModelsByVersionString( uniqueModels );

      return modelsByVersion.entrySet().stream()
              .sorted( Map.Entry.comparingByKey() )
              .map( this::createVersionEntry )
              .toList();
   }

   private Map<AspectModelUrn, Model> removeDuplicateModels( final List<Model> models ) {
      return models.stream()
              .collect( Collectors.toMap(
                      Model::aspectModelUrn,
                      model -> model,
                      ( existing, duplicate ) -> existing,
                      LinkedHashMap::new ) );
   }

   private Map<String, List<Model>> groupModelsByVersionString(
           final Map<AspectModelUrn, Model> uniqueModels ) {

      return uniqueModels.values().stream()
              .collect( Collectors.groupingBy( model ->
                      model.aspectModelUrn().getVersion() ) );
   }

   private Version createVersionEntry( final Map.Entry<String, List<Model>> entry ) {
      final List<Model> sortedModels = entry.getValue().stream()
              .sorted( Comparator.comparing( Model::model ) )
              .toList();
      return new Version( entry.getKey(), sortedModels );
   }

   private <T> T throwOnDuplicateKey( final T v1, final T v2 ) {
      throw new RuntimeException(
              String.format( "Duplicate key for values %s and %s", v1, v2 ) );
   }

}
