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

import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.runtime.server.event.ServerStartupEvent;
import jakarta.inject.Singleton;

/**
 * Tells the desktop app on stdout that the server accepts requests and on which port. The desktop app starts the backend
 * with port {@code -1}, so the operating system assigns a free port without any race between choosing and binding it.
 */
@Singleton
public class ReadyAnnouncer implements ApplicationEventListener<ServerStartupEvent> {
   public static final String READY_PREFIX = "AME_BACKEND_READY port=";

   static String readyLine( final int port ) {
      return READY_PREFIX + port;
   }

   @Override
   public void onApplicationEvent( final ServerStartupEvent event ) {
      System.out.println( readyLine( event.getSource().getPort() ) );
      System.out.flush();
   }
}
