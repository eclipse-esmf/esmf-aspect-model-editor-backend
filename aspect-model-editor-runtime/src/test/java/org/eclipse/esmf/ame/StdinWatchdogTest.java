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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

class StdinWatchdogTest {
   @Test
   void callsBackWhenTheStreamIsClosed() throws Exception {
      final PipedOutputStream desktopApp = new PipedOutputStream();
      final AtomicBoolean called = new AtomicBoolean();
      final CompletableFuture<Void> watch = StdinWatchdog.watch( new PipedInputStream( desktopApp ), () -> called.set( true ) );

      desktopApp.write( 'x' );
      Thread.sleep( 300 );
      assertFalse( watch.isDone(), "the stream is still open" );

      desktopApp.close();
      watch.get( 10, TimeUnit.SECONDS );
      assertTrue( called.get() );
   }

   @Test
   void callsBackWhenReadingFails() throws Exception {
      final InputStream broken = new InputStream() {
         @Override
         public int read() throws IOException {
            throw new IOException( "broken pipe" );
         }
      };
      final AtomicBoolean called = new AtomicBoolean();
      StdinWatchdog.watch( broken, () -> called.set( true ) ).get( 10, TimeUnit.SECONDS );
      assertTrue( called.get() );
   }

   @Test
   void watchesNothingUnlessEnabled() {
      try ( ApplicationContext context = ApplicationContext.run() ) {
         assertTrue( StdinWatchdog.watch( context, () -> {} ).isEmpty() );
      }
      try ( ApplicationContext context = ApplicationContext.run( Map.of( StdinWatchdog.EXIT_ON_STDIN_EOF_PROPERTY, false ) ) ) {
         assertTrue( StdinWatchdog.watch( context, () -> {} ).isEmpty() );
      }
   }
}
