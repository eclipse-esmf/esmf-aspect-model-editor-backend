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

package org.eclipse.esmf.ame.services;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
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
import org.eclipse.esmf.aspectmodel.edit.ChangeGroup;
import org.eclipse.esmf.aspectmodel.edit.change.MoveRenameAspectModelFile;
import org.eclipse.esmf.aspectmodel.generator.zip.AspectModelNamespacePackageCreator;
import org.eclipse.esmf.aspectmodel.loader.AspectModelLoader;
import org.eclipse.esmf.aspectmodel.resolver.exceptions.ModelResolutionException;
import org.eclipse.esmf.aspectmodel.serializer.AspectSerializer;
import org.eclipse.esmf.aspectmodel.urn.AspectModelUrn;
import org.eclipse.esmf.metamodel.AspectModel;

import io.micronaut.http.multipart.StreamingFileUpload;
import jakarta.inject.Singleton;
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

   public Map<String, List<Version>> checkImportPackage( final StreamingFileUpload zipFile, final Path storagePath ) {
      try ( final InputStream inputStream = zipFile.asInputStream() ) {
         final AspectModel aspectModel = loadNamespacePackage( inputStream );
         final List<AspectModelFile> filesToProcess = new ArrayList<>( aspectModel.files() );

         final Stream<URI> uriStream = filesToProcess.stream()
               .map( file -> resolveFileURI( file, storagePath ) );

         return new ModelGroupingUtils( this.modelPath ).groupModelsByNamespaceAndVersion( uriStream );
      } catch ( final IOException e ) {
         throw new ModelResolutionException( "Error reading the ZIP file for import package", e );
      }
   }

   public Map<String, List<Version>> importPackage( final StreamingFileUpload zipFile, final List<String> filesToImport,
         final Path storagePath ) {
      try ( final InputStream inputStream = zipFile.asInputStream() ) {
         final AspectModel aspectModel = loadNamespacePackage( inputStream );
         final AspectChangeManager changeManager = createAspectChangeManager( aspectModel );
         final List<AspectModelFile> filesToModify = new ArrayList<>( aspectModel.files() );

         final List<MoveRenameAspectModelFile> changes = createMoveRenameAspectModelFileList( filesToModify, storagePath );

         changeManager.applyChange( new ChangeGroup( (List) changes ) );
         final Stream<AspectModelFile> aspectModelFileStream = changeManager.aspectModelFiles()
               .filter( aspectModelFile -> filesToImport.stream()
                     .anyMatch( filePath -> aspectModelFile.sourceLocation().toString().contains( filePath ) ) );

         final Stream<URI> uriStream = aspectModelFileStream.map( aspectModelFile -> {
                  //TODO adjust this but this is the way
                  Paths.get( aspectModelFile.sourceLocation().get() ).toFile().getParentFile().mkdirs();

                  AspectSerializer.INSTANCE.write( aspectModelFile );
                  return aspectModelFile.sourceLocation();
               } )
               .filter( Optional::isPresent )
               .map( Optional::get );

         return new ModelGroupingUtils( this.modelPath ).groupModelsByNamespaceAndVersion( uriStream );
      } catch ( final IOException e ) {
         throw new ModelResolutionException( "Could not read from input", e );
      }
   }

   private AspectModel loadNamespacePackage( final InputStream inputStream ) {
      final Collection<File> aspectModelFiles = new ArrayList<>();

      try ( final ZipInputStream zis = new ZipInputStream( inputStream ) ) {
         ZipEntry entry;
         final Path tempDir = Files.createTempDirectory( "aspectModel" );

         while ( ( entry = zis.getNextEntry() ) != null ) {
            final Path tempFilePath = tempDir.resolve( entry.getName() );
            Files.createDirectories( tempFilePath.getParent() );

            try ( final FileOutputStream fos = new FileOutputStream( tempFilePath.toFile() ) ) {
               final byte[] buffer = new byte[1024];
               int len;
               while ( ( len = zis.read( buffer ) ) > 0 ) {
                  fos.write( buffer, 0, len );
               }
            }

            aspectModelFiles.add( tempFilePath.toFile() );
         }

         zis.closeEntry();
      } catch ( final IOException exception ) {
         LOG.error( "Error reading the Archive input stream", exception );
         throw new ModelResolutionException( "Error reading the Archive input stream", exception );
      }

      return aspectModelLoader.load( aspectModelFiles );
   }

   private AspectChangeManager createAspectChangeManager( final AspectModel aspectModel ) {
      final AspectChangeManagerConfig config = AspectChangeManagerConfigBuilder.builder().build();
      return new AspectChangeManager( config, aspectModel );
   }

   private URI resolveFileURI( final AspectModelFile file, final Path storagePath ) {
      final AspectModelUrn urn = extractAspectModelUrn( file );
      final String fileName = extractFileName( file );

      return ModelUtils.createFilePath( urn, fileName, storagePath ).toFile().toURI();
   }

   private List<MoveRenameAspectModelFile> createMoveRenameAspectModelFileList(
         final List<AspectModelFile> filesToProcess, final Path storagePath ) {
      return filesToProcess.stream()
            .map( file -> {
               final AspectModelUrn urn = extractAspectModelUrn( file );
               final String fileName = extractFileName( file );
               final URI uri = ModelUtils.createFilePath( urn, fileName, storagePath ).toFile().toURI();
               return new MoveRenameAspectModelFile( file, uri );
            } )
            .toList();
   }

   private AspectModelUrn extractAspectModelUrn( final AspectModelFile file ) {
      return file.elements().stream()
            .findFirst()
            .orElseThrow( () -> new FileNotFoundException( "Invalid file detected - No Aspect Model URN defined" ) )
            .urn();
   }

   private String extractFileName( final AspectModelFile file ) {
      return file.sourceLocation()
            .map( location -> Paths.get( location ).getFileName().toString() )
            .orElse( "" );
   }

   public void backupWorkspace() {
      try {
         final SimpleDateFormat sdf = new SimpleDateFormat( "yyyy.MM.dd-HH.mm.ss" );
         final String timestamp = sdf.format( new Timestamp( System.currentTimeMillis() ) );
         final String zipFileName = modelPath.resolve( "backup" + timestamp + ".zip" ).toString();

         try ( final ZipOutputStream zos = new ZipOutputStream( new FileOutputStream( zipFileName ) );
               final Stream<Path> paths = Files.walk( modelPath ) ) {

            paths.filter( Files::isRegularFile )
                  .filter( path -> path.toString().endsWith( ".ttl" ) )
                  .forEach( filePath -> {
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
