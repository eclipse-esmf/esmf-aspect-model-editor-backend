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

package org.eclipse.esmf.ame.resolver.strategy;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.RiotException;
import org.apache.jena.vocabulary.RDF;
import org.eclipse.esmf.ame.exceptions.FileReadException;
import org.eclipse.esmf.ame.exceptions.InvalidAspectModelException;
import org.eclipse.esmf.ame.exceptions.UrnNotFoundException;
import org.eclipse.esmf.ame.model.repository.AspectModelInformation;
import org.eclipse.esmf.aspectmodel.resolver.AbstractResolutionStrategy;
import org.eclipse.esmf.aspectmodel.resolver.services.TurtleLoader;
import org.eclipse.esmf.aspectmodel.urn.AspectModelUrn;
import org.eclipse.esmf.aspectmodel.vocabulary.SAMM;
import org.eclipse.esmf.aspectmodel.vocabulary.SAMMC;
import org.eclipse.esmf.aspectmodel.vocabulary.SAMME;
import org.eclipse.esmf.samm.KnownVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vavr.NotImplementedError;
import io.vavr.control.Try;
import lombok.Getter;

/**
 * Represents a resolution strategy for handling aspect models.
 *
 * <p>This abstract class provides common methods and behaviors to resolve and load aspect models, and should be
 * extended to create specific resolution strategies.</p>
 */
@Getter
public abstract class ResolutionStrategy extends AbstractResolutionStrategy {
   private static final Logger LOG = LoggerFactory.getLogger( ResolutionStrategy.class );

   private final Path processingRootPath;
   private final Model currentAspectModel;

   private final String currentFileName;

   /**
    * Constructs a new ResolutionStrategy with the given parameters.
    *
    * @param aspectModel The aspect model file content as string.
    * @param processingRootPath The root path where processing should occur.
    */
   public ResolutionStrategy( final String aspectModel, final Path processingRootPath ) {
      this.processingRootPath = processingRootPath;
      this.currentFileName = "NEW LOADED FILE";
      this.currentAspectModel = loadTurtleFromString( aspectModel );
   }

   /**
    * Constructs a new ResolutionStrategy with the given parameters.
    *
    * @param aspectModelInformation The information about the aspect model.
    * @param processingRootPath The root path where processing should occur.
    */
   public ResolutionStrategy( final AspectModelInformation aspectModelInformation, final Path processingRootPath ) {
      this.processingRootPath = processingRootPath;
      this.currentFileName = aspectModelInformation.getFileName();
      this.currentAspectModel = loadTurtleFromString( aspectModelInformation.getAspectModel() );
   }

   /**
    * Attempts to apply the given aspect model URN.
    * <p>The method will resolve the model from the filesystem or fallback to other strategies based on the URN.</p>
    *
    * @param aspectModelUrn The aspect model URN to be processed.
    * @return A try containing the resolved model, or a failure if the model cannot be resolved.
    */
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

      return Try.success( currentAspectModel );
   }

   private Try<Model> tryOnFailure( final AspectModelUrn aspectModelUrn ) {
      final StmtIterator stmtIterator = currentAspectModel.listStatements(
            ResourceFactory.createResource( aspectModelUrn.toString() ), null, (RDFNode) null );

      if ( stmtIterator.hasNext() ) {
         return Try.success( currentAspectModel );
      }

      return Try.failure( new UrnNotFoundException( String.format( "%s cannot be resolved correctly.", aspectModelUrn ),
            aspectModelUrn ) );
   }

   protected abstract Try<Model> getModelFromFileSystem( AspectModelUrn aspectModelUrn, Path rootPath );

   protected Model loadTurtleFromString( final String aspectModel ) {
      try ( ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
            aspectModel.getBytes( StandardCharsets.UTF_8 ) ) ) {
         Try<Model> resultTry = TurtleLoader.loadTurtle( byteArrayInputStream );

         if ( resultTry.isFailure() ) {
            Throwable cause = resultTry.getCause();
            String errorMessage = cause != null ? cause.getMessage() : "Unknown Error";
            throw new RiotException( errorMessage, cause );
         }

         return resultTry.get();
      } catch ( IOException e ) {
         LOG.error( "Cannot read file." );
         throw new FileReadException( "Error reading the Aspect Model file.", e );
      }
   }

   protected Try<Model> loadTurtleFromFile( final File aspectModel ) {
      try ( final InputStream inputStream = new FileInputStream( aspectModel ) ) {
         return TurtleLoader.loadTurtle( inputStream );
      } catch ( final IOException exception ) {
         return Try.failure( exception );
      }
   }

   /**
    * Retrieves the aspect model URN for the current aspect model.
    *
    * @return The aspect model URN.
    */
   public AspectModelUrn getAspectModelUrn() {
      return AspectModelUrn.fromUrn( getEsmfStatements( currentAspectModel ).orElseThrow(
            () -> new InvalidAspectModelException( String.format(
                  "Unable to find a recognized version of SAMM (with the identifier 'urn:samm:org.eclipse.esmf.samm') in the file: %s.",
                  this.currentFileName ) ) ).next().getSubject().getURI() );
   }

   /**
    * Retrieves the ESMF statements for the given aspect model.
    *
    * @param aspectModel The aspect model to get statements from.
    * @return An optional containing the statement iterator if available, or empty otherwise.
    */
   public static Optional<StmtIterator> getEsmfStatements( final Model aspectModel ) {
      final List<StmtIterator> stmtIterators = new ArrayList<>();
      KnownVersion.getVersions()
                  .forEach( version -> addAllStatementsFromVersion( aspectModel, stmtIterators, version ) );
      return stmtIterators.isEmpty() ? Optional.empty() : stmtIterators.stream().findFirst();
   }

   private static void addAllStatementsFromVersion( final Model aspectModel, final List<StmtIterator> stmtIterators,
         final KnownVersion version ) {
      stmtIterators.addAll( getListOfAllSAMMElements( version ).stream().filter(
                                                                     resource -> aspectModel.listStatements( null, RDF.type, resource ).hasNext() )
                                                               .map( resource -> aspectModel.listStatements( null,
                                                                     RDF.type, resource ) ).toList() );
   }

   /**
    * Retrieves a list of all SAMM elements for the given known version.
    *
    * @param version The known version to get SAMM elements from.
    * @return A list of resources representing the SAMM elements.
    */
   private static List<Resource> getListOfAllSAMMElements( final KnownVersion version ) {
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
