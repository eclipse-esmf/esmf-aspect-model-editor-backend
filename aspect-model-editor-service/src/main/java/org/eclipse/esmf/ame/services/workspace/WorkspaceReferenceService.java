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
package org.eclipse.esmf.ame.services.workspace;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.eclipse.esmf.ame.constants.ApplicationConstants;
import org.eclipse.esmf.ame.model.ModelReference;
import org.eclipse.esmf.ame.model.ReferenceReport;
import org.eclipse.esmf.ame.model.UnreadableModelFile;

import jakarta.inject.Singleton;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Finds the workspace files that use the elements of a namespace or of a single Aspect Model file.
 * <p>
 * Only incoming references count: a file that uses elements of the namespace/file to be deleted. Elements that
 * another remaining file defines as well stay available and therefore do not count. Every workspace
 * file is read as plain RDF (without resolving its imports), so broken or outdated models are checked as well.
 * Files that cannot be read at all are reported separately because they might contain references.
 */
@Singleton
public class WorkspaceReferenceService {
   private static final Logger LOG = LoggerFactory.getLogger( WorkspaceReferenceService.class );

   private final Path modelPath;

   public WorkspaceReferenceService( final Path modelPath ) {
      this.modelPath = modelPath;
   }

   /**
    * A Turtle file at the standard workspace location {@code <namespace>/<version>/<file>.ttl}.
    */
   public record WorkspaceFile( Path path, String namespace, String version, String fileName ) {
      boolean isIn( final String otherNamespace, final String otherVersion ) {
         return namespace.equals( otherNamespace ) && version.equals( otherVersion );
      }
   }

   /**
    * Lists all Turtle files of the workspace at their standard location, sorted by namespace, version and name.
    * Other files (e.g. backups) are ignored.
    *
    * @return the workspace files
    */
   public List<WorkspaceFile> listWorkspaceFiles() {
      if ( !Files.isDirectory( modelPath ) ) {
         return List.of();
      }
      try ( final Stream<Path> paths = Files.walk( modelPath, 3 ) ) {
         return paths.filter( Files::isRegularFile )
               .filter(
                     path -> path.getFileName().toString().toLowerCase( Locale.ROOT ).endsWith( ApplicationConstants.FileExtensions.TTL ) )
               .map( modelPath::relativize )
               .filter( relative -> relative.getNameCount() == 3 )
               .map( relative -> new WorkspaceFile( modelPath.resolve( relative ), relative.getName( 0 ).toString(),
                     relative.getName( 1 ).toString(), relative.getName( 2 ).toString() ) )
               .sorted( Comparator.comparing( WorkspaceFile::namespace ).thenComparing( WorkspaceFile::version )
                     .thenComparing( WorkspaceFile::fileName ) )
               .toList();
      } catch ( final IOException e ) {
         throw new UncheckedIOException( "Could not list the workspace files", e );
      }
   }

   /**
    * Finds the files of other namespaces (or other versions of the same namespace) that use elements of the given
    * namespace version.
    *
    * @param namespace the namespace, e.g. {@code org.eclipse.example}
    * @param version the version, e.g. {@code 1.0.0}
    * @return the references found
    */
   public ReferenceReport findNamespaceReferences( final String namespace, final String version ) {
      final String elementPrefix = "urn:samm:" + namespace + ":" + version + "#";
      final List<WorkspaceFile> others = listWorkspaceFiles().stream().filter( file -> !file.isIn( namespace, version ) ).toList();
      return findReferences( others, uri -> uri.startsWith( elementPrefix ) );
   }

   /**
    * Finds the other workspace files (of any namespace) that use elements defined in the given file.
    *
    * @param file the Aspect Model file to be deleted
    * @return the references found
    */
   public ReferenceReport findFileReferences( final Path file ) {
      final Path target = file.toAbsolutePath().normalize();
      final Set<String> definedElements = definedUrisOrEmpty( target );
      final List<WorkspaceFile> others = listWorkspaceFiles().stream()
            .filter( other -> !other.path().toAbsolutePath().normalize().equals( target ) )
            .toList();
      return findReferences( others, definedElements::contains );
   }

   private ReferenceReport findReferences( final List<WorkspaceFile> files, final Predicate<String> isTargetElement ) {
      final Map<WorkspaceFile, Model> models = new LinkedHashMap<>();
      final List<UnreadableModelFile> unreadable = new ArrayList<>();
      for ( final WorkspaceFile file : files ) {
         try {
            models.put( file, parse( file.path() ) );
         } catch ( final RuntimeException e ) {
            LOG.warn( "Could not check references in {}: {}", file.path(), e.getMessage() );
            unreadable.add( new UnreadableModelFile( file.namespace(), file.version(), file.fileName(),
                  e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage() ) );
         }
      }

      // Elements that remaining files define themselves are still available after the deletion.
      final Set<String> stillDefined = new HashSet<>();
      models.values().forEach( model -> stillDefined.addAll( definedUris( model ) ) );
      final Predicate<String> isRemovedElement = isTargetElement.and( uri -> !stillDefined.contains( uri ) );

      final List<ModelReference> references = new ArrayList<>();
      models.forEach( ( file, model ) -> {
         final Set<String> usedElements = usedUris( model, isRemovedElement );
         if ( !usedElements.isEmpty() ) {
            references.add( new ModelReference( file.namespace(), file.version(), file.fileName(), List.copyOf( usedElements ) ) );
         }
      } );
      return ReferenceReport.of( references, unreadable );
   }

   private static Set<String> definedUrisOrEmpty( final Path file ) {
      try {
         return definedUris( parse( file ) );
      } catch ( final RuntimeException e ) {
         // Elements of an unreadable file are unknown; files using them are broken already.
         LOG.warn( "Could not read the file to delete {}: {}", file, e.getMessage() );
         return Set.of();
      }
   }

   /**
    * The elements a file defines itself: named resources with an {@code rdf:type}.
    */
   private static Set<String> definedUris( final Model model ) {
      final Set<String> result = new TreeSet<>();
      model.listResourcesWithProperty( RDF.type ).forEachRemaining( resource -> {
         if ( resource.isURIResource() ) {
            result.add( resource.getURI() );
         }
      } );
      return result;
   }

   private static Set<String> usedUris( final Model model, final Predicate<String> isRemovedElement ) {
      final Set<String> result = new TreeSet<>();
      model.listStatements().forEachRemaining( statement ->
            Stream.of( statement.getSubject(), statement.getPredicate(), statement.getObject() )
                  .filter( RDFNode::isURIResource )
                  .map( node -> node.asResource().getURI() )
                  .filter( isRemovedElement )
                  .forEach( result::add ) );
      return result;
   }

   private static Model parse( final Path file ) {
      final Model model = ModelFactory.createDefaultModel();
      RDFParser.source( file ).lang( Lang.TURTLE ).parse( model );
      return model;
   }
}
