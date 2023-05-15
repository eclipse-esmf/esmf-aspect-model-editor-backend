package org.eclipse.esmf.ame.resolver.strategy;

import io.vavr.NotImplementedError;
import io.vavr.control.Try;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.RiotException;
import org.apache.jena.vocabulary.RDF;
import org.eclipse.esmf.ame.exceptions.FileReadException;
import org.eclipse.esmf.ame.exceptions.UrnNotFoundException;
import org.eclipse.esmf.aspectmodel.resolver.AbstractResolutionStrategy;
import org.eclipse.esmf.aspectmodel.resolver.services.TurtleLoader;
import org.eclipse.esmf.aspectmodel.urn.AspectModelUrn;
import org.eclipse.esmf.aspectmodel.vocabulary.SAMM;
import org.eclipse.esmf.aspectmodel.vocabulary.SAMMC;
import org.eclipse.esmf.aspectmodel.vocabulary.SAMME;
import org.eclipse.esmf.samm.KnownVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class resolutionStrategy extends AbstractResolutionStrategy {
   private static final Logger LOG = LoggerFactory.getLogger( resolutionStrategy.class );

   public final Path processingRootPath;
   public final Model aspectModel;

   public resolutionStrategy( final String aspectModel, final Path processingRootPath) throws RiotException {
      this.processingRootPath = processingRootPath;
      this.aspectModel = loadTurtleFromString(aspectModel);
   }

   @Override
   public Try<Model> apply( final AspectModelUrn aspectModelUrn) {
      if (aspectModelUrn == null) {
         return Try.failure(new NotImplementedError("AspectModelUrn is not set"));
      }

      final Try<Model> modelFromFileSystem = getModelFromFileSystem(aspectModelUrn, processingRootPath);

      return modelFromFileSystem.isSuccess() ?
              tryOnSuccess(aspectModelUrn, modelFromFileSystem) :
              tryOnFailure(aspectModelUrn);
   }

   private Try<Model> tryOnSuccess(final AspectModelUrn aspectModelUrn, final Try<Model> modelFromFileSystem) {
      if (!getAspectModelUrn().equals(aspectModelUrn)) {
         return modelFromFileSystem;
      }

      return Try.success(aspectModel);
   }

   private Try<Model> tryOnFailure(final AspectModelUrn aspectModelUrn) {
      final StmtIterator stmtIterator = aspectModel.listStatements(
              ResourceFactory.createResource(aspectModelUrn.toString()), null, (RDFNode) null);

      if (stmtIterator.hasNext()) {
         return Try.success(aspectModel);
      }

      return Try.failure(new UrnNotFoundException(
              String.format("%s cannot be resolved correctly.", aspectModelUrn), aspectModelUrn));
   }

   protected abstract Try<Model> getModelFromFileSystem(AspectModelUrn aspectModelUrn, Path rootPath);

   protected Model loadTurtleFromString( final String aspectModel) {
      try ( ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(aspectModel.getBytes( StandardCharsets.UTF_8))) {
         return TurtleLoader.loadTurtle(byteArrayInputStream).getOrElseThrow(
                 error -> new RiotException(error.getCause().getMessage(), error.getCause()));
      } catch ( IOException e) {
         LOG.error("Cannot read file.");
         throw new FileReadException("Error reading the Aspect Model file.", e);
      }
   }

   protected Try<Model> loadTurtleFromFile( final File aspectModel ) {
      try ( final InputStream inputStream = new FileInputStream( aspectModel ) ) {
         return TurtleLoader.loadTurtle( inputStream );
      } catch ( final IOException exception ) {
         return Try.failure( exception );
      }
   }
   public AspectModelUrn getAspectModelUrn() {
      return AspectModelUrn.fromUrn(
              getEsmfStatements( aspectModel ).orElseThrow(
                      () -> new NotImplementedError( "AspectModelUrn cannot be found." ) ).next().getSubject().getURI() );
   }

   public static Optional<StmtIterator> getEsmfStatements( final Model aspectModel ) {
      final List<StmtIterator> stmtIterators = new ArrayList<>();

      KnownVersion.getVersions()
              .forEach( version -> stmtIterators.addAll( getListOfAllSAMMElements( version )
                      .stream()
                      .filter( resource -> aspectModel.listStatements( null, RDF.type, resource ).hasNext() )
                      .map( resource -> aspectModel.listStatements( null, RDF.type, resource ) )
                      .toList() ) );

      return stmtIterators.isEmpty() ? Optional.empty() : stmtIterators.stream().findFirst();
   }
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
