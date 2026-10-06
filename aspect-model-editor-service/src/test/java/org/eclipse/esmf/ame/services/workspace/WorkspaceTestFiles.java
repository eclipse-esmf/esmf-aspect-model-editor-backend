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

/**
 * Writes small Turtle files into a temporary workspace.
 */
final class WorkspaceTestFiles {
   static final String PREFIXES = """
         @prefix samm: <urn:samm:org.eclipse.esmf.samm:meta-model:2.2.0#> .
         @prefix samm-c: <urn:samm:org.eclipse.esmf.samm:characteristic:2.2.0#> .
         @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
         """;

   private WorkspaceTestFiles() {
   }

   static Path write( final Path workspace, final String namespace, final String version, final String fileName, final String body ) {
      final Path file = workspace.resolve( namespace ).resolve( version ).resolve( fileName );
      try {
         Files.createDirectories( file.getParent() );
         Files.writeString( file, PREFIXES + body );
      } catch ( final IOException e ) {
         throw new UncheckedIOException( e );
      }
      return file;
   }

   /** A file in namespace A that defines a property and a characteristic. */
   static Path providerA( final Path workspace ) {
      return write( workspace, "org.example.a", "1.0.0", "Provider.ttl", """
            @prefix : <urn:samm:org.example.a:1.0.0#> .
            :sharedProperty a samm:Property ; samm:characteristic :SharedCharacteristic .
            :SharedCharacteristic a samm:Characteristic ; samm:dataType xsd:string .
            """ );
   }

   /** A file in namespace B that uses the property of namespace A. */
   static Path consumerB( final Path workspace ) {
      return write( workspace, "org.example.b", "1.0.0", "Consumer.ttl", """
            @prefix : <urn:samm:org.example.b:1.0.0#> .
            @prefix a: <urn:samm:org.example.a:1.0.0#> .
            :Consumer a samm:Aspect ; samm:properties ( a:sharedProperty ) ; samm:operations ( ) .
            """ );
   }
}
