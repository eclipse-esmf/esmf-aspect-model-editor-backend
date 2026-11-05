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

package org.eclipse.esmf.ame;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import io.micronaut.core.value.PropertyResolver;

/**
 * Stops the backend when its stdin is closed. The desktop app closes the pipe to request a graceful shutdown, which works
 * on every platform (Windows has no SIGTERM) and lets the JVM run its exit tasks, e.g. writing the class data sharing
 * archive.
 */
public final class StdinWatchdog {
   public static final String EXIT_ON_STDIN_EOF_PROPERTY = "ame.exit-on-stdin-eof";

   private StdinWatchdog() {
   }

   public static Optional<CompletableFuture<Void>> watch( final PropertyResolver properties, final Runnable onEof ) {
      return properties.get( EXIT_ON_STDIN_EOF_PROPERTY, Boolean.class )
            .filter( Boolean::booleanValue )
            .map( enabled -> watch( System.in, onEof ) );
   }

   public static CompletableFuture<Void> watch( final InputStream input, final Runnable onEof ) {
      final CompletableFuture<Void> eof = new CompletableFuture<>();
      final Thread thread = new Thread( () -> {
         try {
            while ( input.read() != -1 ) {
               // the desktop app does not send any data, wait for the end of the stream
            }
         } catch ( final IOException exception ) {
            // a broken pipe also means that the desktop app is gone
         }
         eof.complete( null );
      }, "stdin-watchdog" );
      thread.setDaemon( true );
      thread.start();
      return eof.thenRun( onEof );
   }
}
