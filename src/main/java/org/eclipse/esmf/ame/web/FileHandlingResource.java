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

package org.eclipse.esmf.ame.web;

import java.util.Map;
import java.util.Optional;

import org.eclipse.esmf.ame.exceptions.FileNotFoundException;
import org.eclipse.esmf.ame.services.FileHandlingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller class where all the requests are mapped RequestMapping for the class is "aspect" generates a response
 * based on the mapping by calling the particular request handler methods
 */
@RestController
@RequestMapping( "file-handling" )
public class FileHandlingResource {

   public static final String FILE_NAME = "file-name";
   public static final String NAMESPACE = "namespace";
   private final FileHandlingService fileHandlingService;

   public FileHandlingResource( final FileHandlingService fileHandlingService ) {
      this.fileHandlingService = fileHandlingService;
   }

   /**
    * Locks a file based on the provided namespace and filename.
    *
    * @param headers HTTP headers that contain the namespace and filename.
    * @return ResponseEntity with operation result.
    *
    * @throws FileNotFoundException if the filename is not provided.
    */
   @GetMapping( "lock" )
   public ResponseEntity<String> lockFile( @RequestHeader final Map<String, String> headers )
         throws FileNotFoundException {
      return processFileOperation( headers, true );
   }

   /**
    * Unlocks a file based on the provided namespace and filename.
    *
    * @param headers HTTP headers that contain the namespace and filename.
    * @return ResponseEntity with operation result.
    *
    * @throws FileNotFoundException if the filename is not provided.
    */
   @GetMapping( "unlock" )
   public ResponseEntity<String> unlockFile( @RequestHeader final Map<String, String> headers )
         throws FileNotFoundException {
      return processFileOperation( headers, false );
   }

   /**
    * Processes file operations (lock/unlock) based on the provided namespace and filename.
    *
    * @param headers HTTP headers that contain the namespace and filename.
    * @param isLocking Boolean flag to determine the operation type (lock/unlock).
    * @return ResponseEntity with operation result.
    *
    * @throws FileNotFoundException if the filename is not provided.
    */
   private ResponseEntity<String> processFileOperation( Map<String, String> headers, boolean isLocking )
         throws FileNotFoundException {
      final String namespace = Optional.ofNullable( headers.get( NAMESPACE ) ).orElse( "" );
      final String filename = Optional.ofNullable( headers.get( FILE_NAME ) )
                                      .orElseThrow( () -> new FileNotFoundException( "Please specify a file name" ) );

      if ( isValidParam( namespace ) && isValidParam( filename ) ) {
         throw new IllegalArgumentException( "Invalid headers parameter" );
      }

      return ResponseEntity.ok( isLocking ? fileHandlingService.lockFile( namespace, filename )
            : fileHandlingService.unlockFile( namespace, filename ) );
   }

   private boolean isValidParam( String fileName ) {
      return fileName != null && !fileName.isEmpty() && !fileName.contains( ".." ) && fileName.matches(
            "[A-Za-z0-9_\\-\\.]+" );
   }
}
