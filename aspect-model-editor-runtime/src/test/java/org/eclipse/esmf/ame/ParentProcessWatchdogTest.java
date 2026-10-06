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

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ParentProcessWatchdogTest {
   private Process parent;

   @AfterEach
   void stopParent() {
      if ( parent != null ) {
         parent.destroyForcibly();
      }
   }

   @Test
   void callsBackWhenTheParentProcessEnds() throws Exception {
      parent = startSleepingProcess();
      final CompletableFuture<Void> watch = ParentProcessWatchdog.watch( parent.pid(), () -> {} );

      Thread.sleep( 300 );
      assertFalse( watch.isDone(), "the parent is still running" );

      parent.destroyForcibly();
      watch.get( 10, TimeUnit.SECONDS );
      assertTrue( watch.isDone() );
   }

   @Test
   void callsBackImmediatelyWhenTheParentProcessDoesNotExist() throws Exception {
      final Process ended = startSleepingProcess();
      final long pid = ended.pid();
      ended.destroyForcibly().waitFor( 10, TimeUnit.SECONDS );

      final AtomicBoolean called = new AtomicBoolean();
      ParentProcessWatchdog.watch( pid, () -> called.set( true ) ).get( 10, TimeUnit.SECONDS );
      assertTrue( called.get() );
   }

   @Test
   void watchesTheParentProcessFromTheConfiguration() throws Exception {
      parent = startSleepingProcess();
      final AtomicBoolean called = new AtomicBoolean();

      try ( ApplicationContext context = ApplicationContext.run( Map.of( ParentProcessWatchdog.PARENT_PID_PROPERTY, parent.pid() ) ) ) {
         final CompletableFuture<Void> watch = ParentProcessWatchdog.watch( context, () -> called.set( true ) ).orElseThrow();
         assertFalse( called.get() );

         parent.destroyForcibly();
         watch.get( 10, TimeUnit.SECONDS );
         assertTrue( called.get() );
      }
   }

   @Test
   void watchesNothingWithoutParentProcessId() {
      try ( ApplicationContext context = ApplicationContext.run() ) {
         assertTrue( ParentProcessWatchdog.watch( context, () -> {} ).isEmpty() );
      }
   }

   private static Process startSleepingProcess() throws Exception {
      final String java = Path.of( System.getProperty( "java.home" ), "bin", "java" ).toString();
      return new ProcessBuilder( java, "-cp", System.getProperty( "java.class.path" ), Sleeper.class.getName() ).start();
   }

   /** Stands in for the desktop app. */
   public static class Sleeper {
      public static void main( final String[] args ) throws InterruptedException {
         Thread.sleep( 60_000 );
      }
   }
}
