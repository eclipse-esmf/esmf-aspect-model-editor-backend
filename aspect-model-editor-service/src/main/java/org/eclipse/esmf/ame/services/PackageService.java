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

package org.eclipse.esmf.ame.services;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.eclipse.esmf.ame.exceptions.CreateFileException;
import org.eclipse.esmf.ame.exceptions.FileNotFoundException;
import org.eclipse.esmf.ame.services.models.Version;
import org.eclipse.esmf.ame.services.utils.ModelGroupingUtils;
import org.eclipse.esmf.ame.services.utils.ModelUtils;
import org.eclipse.esmf.aspectmodel.AspectModelFile;
import org.eclipse.esmf.aspectmodel.edit.AspectChangeManager;
import org.eclipse.esmf.aspectmodel.edit.AspectChangeManagerConfig;
import org.eclipse.esmf.aspectmodel.edit.AspectChangeManagerConfigBuilder;
import org.eclipse.esmf.aspectmodel.edit.Change;
import org.eclipse.esmf.aspectmodel.edit.ChangeGroup;
import org.eclipse.esmf.aspectmodel.edit.change.AddAspectModelFile;
import org.eclipse.esmf.aspectmodel.generator.zip.AspectModelNamespacePackageCreator;
import org.eclipse.esmf.aspectmodel.loader.AspectModelLoader;
import org.eclipse.esmf.aspectmodel.resolver.NamespacePackage;
import org.eclipse.esmf.aspectmodel.resolver.exceptions.ModelResolutionException;
import org.eclipse.esmf.aspectmodel.resolver.fs.ModelsRoot;
import org.eclipse.esmf.aspectmodel.resolver.fs.StructuredModelsRoot;
import org.eclipse.esmf.aspectmodel.resolver.modelfile.RawAspectModelFile;
import org.eclipse.esmf.aspectmodel.resolver.modelfile.RawAspectModelFileBuilder;
import org.eclipse.esmf.aspectmodel.serializer.AspectSerializer;
import org.eclipse.esmf.aspectmodel.urn.AspectModelUrn;
import org.eclipse.esmf.metamodel.AspectModel;

import io.micronaut.http.multipart.CompletedFileUpload;
import jakarta.inject.Singleton;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service class for handling package import and export operations for Aspect Models.
 */
@Singleton
public class PackageService {
   private static final Logger LOG = LoggerFactory.getLogger( PackageService.class );

   private final AspectModelLoader aspectModelLoader;
   private final Path modelPath;

   public PackageService( final AspectModelLoader aspectModelLoader, final Path modelPath ) {
      this.aspectModelLoader = aspectModelLoader;
      this.modelPath = modelPath;
   }

   public byte[] exportPackage( final String aspectModelUrn ) {
      final AspectModel aspectModel = aspectModelLoader.load( AspectModelUrn.fromUrn( aspectModelUrn ) );
      final AspectModelNamespacePackageCreator packageCreator = new AspectModelNamespacePackageCreator( aspectModel );

      return packageCreator.generate().findFirst()
            .orElseThrow( () -> new FileNotFoundException( String.format( "No file found for %s", aspectModelUrn ) ) ).getContent();
   }

   public List<Map<String, String>> validatePackage( final CompletedFileUpload zipFile ) {
      try {
         final byte[] zipContent = IOUtils.toByteArray( zipFile.getInputStream() );
         final NamespacePackage namespacePackage = new NamespacePackage( zipContent, null );

         return namespacePackage.loadContents().flatMap( file -> {
                  final ByteArrayInputStream inputStream =
                        ModelUtils.createInputStream( ( (RawAspectModelFile) file ).sourceRepresentation() );
                  final AspectModel aspectModel = aspectModelLoader.load( inputStream );
                  if ( aspectModel != null ) {
                     return aspectModel.files().stream()
                           .flatMap( aspectModelFile -> {
                              try {
                                 return aspectModelLoader.load( aspectModelFile.elements().getFirst().urn() )
                                       .files().stream().map( AspectModelFile::filename )
                                       .filter( Optional::isPresent ).map( Optional::get );
                              } catch ( final ModelResolutionException e ) {
                                 LOG.info( "Ignoring Exception" );
                                 return Stream.empty();
                              }
                           } );
                  }
                  return Stream.empty();
               } )
               .map( filename -> Map.of( "model", filename ) )
               .toList();
      } catch ( final IOException e ) {
         throw new ModelResolutionException( "Could not read from input", e );
      }
   }

   public Map<String, List<Version>> importPackage( final CompletedFileUpload zipFile ) {
      try {
         final ModelsRoot modelsRoot = new StructuredModelsRoot( this.modelPath );
         final byte[] zipContent = IOUtils.toByteArray( zipFile.getInputStream() );
         final NamespacePackage namespacePackage = new NamespacePackage( zipContent, null );

         final List<Change> fileChanges = namespacePackage.loadContents().<Change> map( file -> createAddChange( file, modelsRoot ) )
               .toList();

         final AspectChangeManager changeManager = initChangeManager();
         changeManager.applyChange( new ChangeGroup( fileChanges ) );

         final Stream<URI> savedUris = saveAspectModelFiles( changeManager.aspectModelFiles() );

         return new ModelGroupingUtils( this.aspectModelLoader, this.modelPath ).groupModelsByNamespaceAndVersion( savedUris );
      } catch ( final IOException e ) {
         throw new ModelResolutionException( "Could not read from input", e );
      }
   }

   private AddAspectModelFile createAddChange( final AspectModelFile file, final ModelsRoot modelsRoot ) {
      final URI targetLocation = modelsRoot.directoryForNamespace( file.namespaceUrn() ).resolve( file.filename().orElseThrow() ).toUri();

      final RawAspectModelFileBuilder builder = RawAspectModelFileBuilder.builder().sourceModel( file.sourceModel() )
            .sourceLocation( Optional.of( targetLocation ) ).headerComment( file.headerComment() );

      return new AddAspectModelFile( builder.build() );
   }

   private AspectChangeManager initChangeManager() {
      final AspectChangeManagerConfig config = AspectChangeManagerConfigBuilder.builder().build();
      return new AspectChangeManager( config, new AspectModelLoader().emptyModel() );
   }

   private Stream<URI> saveAspectModelFiles( final Stream<AspectModelFile> files ) {
      return files.peek( this::ensureParentDirectoryExists ).peek( AspectSerializer.INSTANCE::write ).map( AspectModelFile::sourceLocation )
            .filter( Optional::isPresent ).map( Optional::get );
   }

   private void ensureParentDirectoryExists( final AspectModelFile file ) {
      file.sourceLocation().map( Paths::get ).map( Path::toFile ).map( File::getParentFile ).ifPresent( parent -> {
         if ( !parent.exists() ) {
            parent.mkdirs();
         }
      } );
   }

   public void backupWorkspace() {
      try {
         final SimpleDateFormat sdf = new SimpleDateFormat( "yyyy.MM.dd-HH.mm.ss" );
         final String timestamp = sdf.format( new Timestamp( System.currentTimeMillis() ) );
         final String zipFileName = modelPath.resolve( "backup-" + timestamp + ".zip" ).toString();

         try ( final ZipOutputStream zos = new ZipOutputStream( new FileOutputStream( zipFileName ) );
               final Stream<Path> paths = Files.walk( modelPath ) ) {

            paths.filter( Files::isRegularFile ).filter( path -> path.toString().endsWith( ".ttl" ) ).forEach( filePath -> {
               try {
                  final ZipEntry zipEntry = new ZipEntry( modelPath.relativize( filePath ).toString() );
                  zos.putNextEntry( zipEntry );
                  Files.copy( filePath, zos );
                  zos.closeEntry();
               } catch ( final IOException e ) {
                  LOG.error( "Error while zipping file: {}", filePath, e );
                  throw new CreateFileException( "Error while zipping file: " + filePath, e );
               }
            } );
         }
      } catch ( final IOException e ) {
         LOG.error( "Failed to create the zip file." );
         throw new CreateFileException( "An error occurred while creating the zip file.", e );
      }
   }
}
