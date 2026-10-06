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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.eclipse.esmf.ame.exceptions.FileNotFoundException;
import org.eclipse.esmf.ame.exceptions.ModelReferencedException;
import org.eclipse.esmf.ame.model.ReferenceReport;
import org.eclipse.esmf.ame.services.PackageService;
import org.eclipse.esmf.ame.services.file.FileOperations;
import org.eclipse.esmf.ame.services.file.FilePathResolver;
import org.eclipse.esmf.ame.services.models.ClearWorkspaceResult;
import org.eclipse.esmf.ame.services.workspace.WorkspaceReferenceService.WorkspaceFile;

import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Deletes complete namespaces or all Aspect Models of the workspace.
 * <p>
 * Only Turtle files at the standard location {@code <namespace>/<version>/<file>.ttl} are deleted, together with
 * directories that are empty afterwards. Other files, e.g. workspace backups, are kept.
 */
@Singleton
public class WorkspaceCleanupService {
   private static final Logger LOG = LoggerFactory.getLogger( WorkspaceCleanupService.class );

   private final Path modelPath;
   private final WorkspaceReferenceService referenceService;
   private final FileOperations fileOperations;
   private final FilePathResolver filePathResolver;
   private final PackageService packageService;

   public WorkspaceCleanupService( final Path modelPath, final WorkspaceReferenceService referenceService,
         final FileOperations fileOperations, final FilePathResolver filePathResolver, final PackageService packageService ) {
      this.modelPath = modelPath;
      this.referenceService = referenceService;
      this.fileOperations = fileOperations;
      this.filePathResolver = filePathResolver;
      this.packageService = packageService;
   }

   /**
    * Checks whether a namespace version can be deleted.
    *
    * @param namespace the namespace
    * @param version the version
    * @return the files of other namespaces or versions that use its elements
    * @throws IllegalArgumentException if namespace or version are invalid
    */
   public ReferenceReport checkNamespace( final String namespace, final String version ) {
      filePathResolver.resolveVersionDirectory( modelPath, namespace, version );
      return referenceService.findNamespaceReferences( namespace, version );
   }

   /**
    * Checks whether an Aspect Model file can be deleted.
    *
    * @param namespace the namespace of the file
    * @param version the version of the file
    * @param fileName the file name
    * @return the other files that use its elements
    * @throws IllegalArgumentException if a name is invalid
    * @throws FileNotFoundException if the file does not exist
    */
   public ReferenceReport checkFile( final String namespace, final String version, final String fileName ) {
      final Path file = filePathResolver.resolveModelFile( modelPath, namespace, version, fileName );
      if ( !Files.isRegularFile( file ) ) {
         throw new FileNotFoundException( "Aspect Model file not found: " + namespace + ":" + version + "/" + fileName );
      }
      return referenceService.findFileReferences( file );
   }

   /**
    * Deletes all Aspect Model files of a namespace version if no file of another namespace or version uses them.
    *
    * @param namespace the namespace
    * @param version the version
    * @return the number of deleted files
    * @throws ModelReferencedException if the namespace is still used
    * @throws FileNotFoundException if the namespace version does not exist
    */
   public int deleteNamespace( final String namespace, final String version ) {
      final Path versionDirectory = filePathResolver.resolveVersionDirectory( modelPath, namespace, version );
      if ( !Files.isDirectory( versionDirectory ) ) {
         throw new FileNotFoundException( "Namespace not found: " + namespace + ":" + version );
      }
      final ReferenceReport report = referenceService.findNamespaceReferences( namespace, version );
      if ( !report.deletable() ) {
         throw new ModelReferencedException( "Namespace '" + namespace + ":" + version + "' is still used by other files", report );
      }

      final List<WorkspaceFile> files = referenceService.listWorkspaceFiles().stream()
            .filter( file -> file.isIn( namespace, version ) )
            .toList();
      deleteFiles( files );
      fileOperations.deleteDirectoryIfEmpty( versionDirectory );
      fileOperations.deleteDirectoryIfEmpty( versionDirectory.getParent() );
      LOG.info( "Deleted namespace {}:{} with {} file(s)", namespace, version, files.size() );
      return files.size();
   }

   /**
    * Deletes all Aspect Model files of the workspace. No reference check is needed because everything is deleted.
    *
    * @param backup whether a backup of the workspace should be created first
    * @return the number of deleted files and whether a backup was created
    */
   public ClearWorkspaceResult clearWorkspace( final boolean backup ) {
      final List<WorkspaceFile> files = referenceService.listWorkspaceFiles();
      final boolean backupCreated = backup && !files.isEmpty();
      if ( backupCreated ) {
         packageService.backupWorkspace();
      }
      deleteFiles( files );
      // Also removes namespace/version folders left empty by earlier deletions; folders with other content are kept.
      fileOperations.listSubdirectories( modelPath ).forEach( namespaceDirectory -> {
         fileOperations.listSubdirectories( namespaceDirectory ).forEach( fileOperations::deleteDirectoryIfEmpty );
         fileOperations.deleteDirectoryIfEmpty( namespaceDirectory );
      } );
      LOG.info( "Cleared workspace, deleted {} file(s), backup created: {}", files.size(), backupCreated );
      return new ClearWorkspaceResult( files.size(), backupCreated );
   }

   private void deleteFiles( final List<WorkspaceFile> files ) {
      files.forEach( file -> fileOperations.deleteFileSafely( file.path() ) );
   }
}
