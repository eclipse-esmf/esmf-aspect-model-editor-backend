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

package org.eclipse.esmf.ame.api;

import java.util.Map;
import java.util.Optional;

import org.eclipse.esmf.ame.exceptions.FileNotFoundException;
import org.eclipse.esmf.ame.services.FileHandlingService;
import org.eclipse.esmf.ame.utils.ModelUtils;
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
public class FileHandlingController {

   public static final String FILE_NAME = "file-name";
   public static final String NAMESPACE = "namespace";
   private final FileHandlingService fileHandlingService;

   public FileHandlingController( final FileHandlingService fileHandlingService ) {
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
      String sanitizedNamespace = ModelUtils.sanitizeFileInformation( headers.get( NAMESPACE ) );
      String sanitizedFileName = ModelUtils.sanitizeFileInformation( headers.get( FILE_NAME ) );

      return processFileOperation( sanitizedNamespace, sanitizedFileName, true );
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
      String sanitizedNamespace = ModelUtils.sanitizeFileInformation( headers.get( NAMESPACE ) );
      String sanitizedFileName = ModelUtils.sanitizeFileInformation( headers.get( FILE_NAME ) );

      return processFileOperation( sanitizedNamespace, sanitizedFileName, false );
   }

   /**
    * Processes file operations (lock/unlock) based on the provided file namespace and filename.
    * This method uses the namespace and filename obtained from HTTP headers to determine
    * the specific file for the lock/unlock operation.
    *
    * @param sanitizedNamespace The namespace associated with the file, extracted from HTTP headers.
    * @param sanitizedFileName The name of the file, extracted from HTTP headers.
    * @param isLocking Boolean flag to determine the operation type (true for lock, false for unlock).
    * @return ResponseEntity with the result of the lock/unlock operation.
    *
    * @throws FileNotFoundException if the filename is not provided in the headers.
    * @throws IllegalArgumentException if either the namespace or filename parameters are invalid.
    */
   private ResponseEntity<String> processFileOperation( String sanitizedNamespace, String sanitizedFileName,
         boolean isLocking )
         throws FileNotFoundException {
      final String namespace = Optional.ofNullable( sanitizedNamespace ).orElse( "" );
      final String fileName = Optional.ofNullable( sanitizedFileName )
                                      .orElseThrow( () -> new FileNotFoundException( "Please specify a file name" ) );

      if ( isValidParam( namespace ) && isValidParam( fileName ) ) {
         throw new IllegalArgumentException( "Invalid headers parameter" );
      }

      return ResponseEntity.ok( isLocking ? fileHandlingService.lockFile( namespace, fileName )
            : fileHandlingService.unlockFile( namespace, fileName ) );
   }

   private boolean isValidParam( String fileName ) {
      return fileName != null && !fileName.isEmpty() && !fileName.contains( ".." ) && fileName.matches(
            "[A-Za-z0-9_\\-\\.]+" );
   }
}
