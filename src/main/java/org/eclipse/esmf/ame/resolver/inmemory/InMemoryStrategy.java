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

package org.eclipse.esmf.ame.resolver.inmemory;

import io.vavr.NotImplementedError;
import io.vavr.control.Try;
import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.RiotException;
import org.apache.jena.vocabulary.RDF;
import org.eclipse.esmf.ame.exceptions.UrnNotFoundException;
import org.eclipse.esmf.ame.model.ValidationProcess;
import org.eclipse.esmf.aspectmodel.resolver.AbstractResolutionStrategy;
import org.eclipse.esmf.aspectmodel.resolver.AspectModelResolver;
import org.eclipse.esmf.aspectmodel.resolver.services.TurtleLoader;
import org.eclipse.esmf.aspectmodel.urn.AspectModelUrn;
import org.eclipse.esmf.aspectmodel.vocabulary.SAMM;
import org.eclipse.esmf.aspectmodel.vocabulary.SAMMC;
import org.eclipse.esmf.aspectmodel.vocabulary.SAMME;
import org.eclipse.esmf.samm.KnownVersion;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.apache.jena.http.auth.AuthEnv.LOG;

public class InMemoryStrategy extends AbstractResolutionStrategy {
   public final Path processingRootPath;
   public final Model aspectModel;

   public final ValidationProcess validationProcess;

   public InMemoryStrategy( final String aspectModel, final ValidationProcess validationProcess ) throws RiotException {
      processingRootPath = validationProcess.getPath();
      this.aspectModel = loadTurtle( aspectModel );
      this.validationProcess = validationProcess;
   }

   private Model loadTurtle( final String aspectModel ) {
      final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
            aspectModel.getBytes( StandardCharsets.UTF_8 ) );

       Model model = TurtleLoader.loadTurtle( byteArrayInputStream ).getOrElseThrow(
               error -> new RiotException( error.getCause().getMessage(), error.getCause() ) );

       IOUtils.closeQuietly( byteArrayInputStream );
       return model;
   }

   @Override
   public Try<Model> apply( final AspectModelUrn aspectModelUrn ) {
      if ( aspectModelUrn == null ) {
         return Try.failure( new NotImplementedError( "AspectModelUrn is not set" ) );
      }

      final Try<Model> modelFromFileSystem = getModelFromFileSystem( aspectModelUrn, processingRootPath );

      return modelFromFileSystem.isSuccess() ?
            tryOnSuccess( aspectModelUrn, modelFromFileSystem ) :
            tryOnFailure( aspectModelUrn );
   }

   private Try<Model> tryOnSuccess( final AspectModelUrn aspectModelUrn, final Try<Model> modelFromFileSystem ) {
      if ( !getAspectModelUrn().equals( aspectModelUrn ) ) {
         return modelFromFileSystem;
      }

      return Try.success( aspectModel );
   }

   private Try<Model> tryOnFailure( final AspectModelUrn aspectModelUrn ) {
      // Special case on importing models, ref. Aspect Model can be already imported
      if ( validationProcess.equals( ValidationProcess.IMPORT ) ) {
         return getModelFromFileSystem( aspectModelUrn, ValidationProcess.MODELS.getPath() );
      }

      final StmtIterator stmtIterator = aspectModel.listStatements(
            ResourceFactory.createResource( aspectModelUrn.toString() ), null, (RDFNode) null );

      if ( stmtIterator.hasNext() ) {
         return Try.success( aspectModel );
      }

      return Try.failure( new UrnNotFoundException( String.format( "%s cannot be resolved correctly.", aspectModelUrn ),
            aspectModelUrn ) );
   }

   protected Try<Model> getModelFromFileSystem( final AspectModelUrn aspectModelUrn, final Path rootPath ) {
      final Path directory = rootPath.resolve( aspectModelUrn.getNamespace() ).resolve( aspectModelUrn.getVersion() );

      final File namedResourceFile = directory.resolve( aspectModelUrn.getName() + ".ttl" ).toFile();
      if ( namedResourceFile.exists() ) {
         return loadTurtle( new File( namedResourceFile.toURI() ) );
      }

      LOG.warn( "Looking for {}, but no {}.ttl was found. Inspecting files in {}", aspectModelUrn.getName(),
            aspectModelUrn.getName(), directory );

      return Arrays.stream( Optional.ofNullable( directory.toFile().listFiles() ).orElse( new File[] {} ) )
                   .filter( File::isFile ).filter( file -> file.getName().endsWith( ".ttl" ) ).map( File::toURI )
                   .sorted().map( this::loadFromUri ).filter(
                  tryModel -> tryModel.map( model -> AspectModelResolver.containsDefinition( model, aspectModelUrn ) )
                                      .getOrElse( false ) ).findFirst().orElse( Try.failure( new FileNotFoundException(
                  "No model file containing " + aspectModelUrn + " could be found in directory: " + directory ) ) );
   }

   private Try<Model> loadTurtle( final File aspectModel ) {
      try ( final InputStream inputStream = new FileInputStream( aspectModel ) ) {
         final Try<Model> model = TurtleLoader.loadTurtle( inputStream );
         IOUtils.closeQuietly( inputStream );
         return model;
      } catch ( final IOException exception ) {
         return Try.failure( exception );
      }
   }

   public AspectModelUrn getAspectModelUrn() {
      return AspectModelUrn.fromUrn(
            getEsmfStatements().orElseThrow( () -> new NotImplementedError( "AspectModelUrn cannot be found." ) ).next()
                               .getSubject().getURI() );
   }

   private Optional<StmtIterator> getEsmfStatements() {
      final List<StmtIterator> stmtIterators = new ArrayList<>();

      KnownVersion.getVersions()
                  .forEach( version -> stmtIterators.addAll( getListOfAllSAMMElements( version )
                        .stream()
                        .filter( resource -> aspectModel.listStatements( null, RDF.type, resource ).hasNext() )
                        .map( resource -> aspectModel.listStatements( null, RDF.type, resource ) )
                        .toList() ) );

      return stmtIterators.isEmpty() ? Optional.empty() : stmtIterators.stream().findFirst();
   }

   private List<Resource> getListOfAllSAMMElements( final KnownVersion version ) {
      final SAMM samm = new SAMM( version );
      final SAMMC sammc = new SAMMC( version );
      final SAMME samme = new SAMME( version, samm );

      final List<Resource> resources = new ArrayList<>();
      resources.add( samm.Aspect() );
      resources.add( samm.Property() );
      resources.add( samm.Operation() );
      resources.add( samm.Event() );
      resources.add( samm.Entity() );
      resources.add( samm.Characteristic() );
      resources.add( samm.Constraint() );
      resources.add( samm.AbstractEntity() );
      resources.add( samm.AbstractProperty() );
      resources.addAll( samme.allEntities().toList() );
      resources.addAll( sammc.allCharacteristics().toList() );
      resources.addAll( sammc.allConstraints().toList() );
      resources.addAll( sammc.allCollections().toList() );

      return resources;
   }
}
