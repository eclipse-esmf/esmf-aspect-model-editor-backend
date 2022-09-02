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

package io.openmanufacturing.ame.resolver.inmemory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;

import io.openmanufacturing.ame.exceptions.UrnNotFoundException;
import io.openmanufacturing.sds.aspectmetamodel.KnownVersion;
import io.openmanufacturing.sds.aspectmodel.resolver.FileSystemStrategy;
import io.openmanufacturing.sds.aspectmodel.resolver.services.TurtleLoader;
import io.openmanufacturing.sds.aspectmodel.urn.AspectModelUrn;
import io.openmanufacturing.sds.aspectmodel.vocabulary.BAMM;
import io.vavr.NotImplementedError;
import io.vavr.control.Try;

public class InMemoryStrategy extends FileSystemStrategy {
   public final Model model;

   public InMemoryStrategy( final String aspectModel, final Path modelsRoot ) {
      super( modelsRoot );
      final InputStream aspectModelStream = new ByteArrayInputStream( aspectModel.getBytes( StandardCharsets.UTF_8 ) );
      model = TurtleLoader.loadTurtle( aspectModelStream ).get();
   }

   @Override
   public Try<Model> apply( final AspectModelUrn aspectModelUrn ) {
      if ( aspectModelUrn == null ) {
         return Try.failure( new NotImplementedError( "AspectModelUrn is not set" ) );
      }

      final Try<Model> isOnDirectory = getModelFromFileSystem( aspectModelUrn );

      if ( isOnDirectory.isSuccess() && !getAspectModelUrn().equals( aspectModelUrn ) ) {
         return isOnDirectory;
      }

      final StmtIterator stmtIterator = model
            .listStatements( ResourceFactory.createResource( aspectModelUrn.toString() ), null, (RDFNode) null );

      if ( stmtIterator.hasNext() ) {
         return Try.success( model );
      }

      return Try.failure(
            new UrnNotFoundException( String.format( "%s cannot be resolved correctly.", aspectModelUrn ),
                  aspectModelUrn ) );
   }

   /**
    * Method needed for mocking
    */
   protected Try<Model> getModelFromFileSystem( final AspectModelUrn aspectModelUrn ) {
      return super.apply( aspectModelUrn );
   }

   public AspectModelUrn getAspectModelUrn() {
      final Optional<StmtIterator> sdsStatements = getSdsStatements();

      final String aspectModelUrn = sdsStatements.orElseThrow(
                                                       () -> new NotImplementedError( "AspectModelUrn cannot be found." ) )
                                                 .next().getSubject().getURI();

      return AspectModelUrn.fromUrn( aspectModelUrn );
   }

   private Optional<StmtIterator> getSdsStatements() {
      final BAMM bamm = new BAMM( KnownVersion.getLatest() );
      final List<Resource> resources = List.of( bamm.Aspect(), bamm.Property(), bamm.Entity(), bamm.Characteristic() );
      return resources.stream().filter( resource -> model.listStatements( null, RDF.type, resource ).hasNext() )
                      .map( resource -> model.listStatements( null, RDF.type, resource ) )
                      .findFirst();
   }
}
