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

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import io.micronaut.core.value.PropertyResolver;

/**
 * Stops the backend together with the desktop app which started it.
 * <p>
 * The desktop app passes its process id as {@value #PARENT_PID_PROPERTY}. When the app ends in any way
 * (normal exit, crash, killed, system shutdown), the backend would otherwise keep running without an owner.
 * Without the property (e.g. a standalone backend), nothing is watched.
 */
public final class ParentProcessWatchdog {
   public static final String PARENT_PID_PROPERTY = "ame.parent-pid";

   private ParentProcessWatchdog() {
   }

   /**
    * Watches the process given by {@value #PARENT_PID_PROPERTY}, if set.
    *
    * @param properties the configuration containing the optional parent process id
    * @param onParentExit called once the parent process has ended
    * @return the pending watch, or empty if no parent process id is configured
    */
   public static Optional<CompletableFuture<Void>> watch( final PropertyResolver properties, final Runnable onParentExit ) {
      return properties.get( PARENT_PID_PROPERTY, Long.class ).map( pid -> watch( pid, onParentExit ) );
   }

   /**
    * Calls {@code onParentExit} once the process with the given id has ended, immediately if it does not exist.
    *
    * @param parentPid the process id to watch
    * @param onParentExit called once the process has ended
    * @return the pending watch
    */
   public static CompletableFuture<Void> watch( final long parentPid, final Runnable onParentExit ) {
      return ProcessHandle.of( parentPid )
            .map( ProcessHandle::onExit )
            .orElseGet( () -> CompletableFuture.completedFuture( null ) )
            .thenRun( onParentExit );
   }
}
