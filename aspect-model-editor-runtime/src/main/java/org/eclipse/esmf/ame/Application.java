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

import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.esmf.aspectmodel.urn.AspectModelUrn;

import io.micronaut.context.ApplicationContext;
import io.micronaut.runtime.Micronaut;
import io.micronaut.serde.annotation.SerdeImport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SerdeImport( AspectModelUrn.class )
public class Application {
   private static final Logger LOG = LoggerFactory.getLogger( Application.class );

   static void main( final String[] args ) {
      final ApplicationContext context = Micronaut.run( Application.class, args );
      final AtomicBoolean stopping = new AtomicBoolean();

      ParentProcessWatchdog.watch( context, () -> {
         LOG.info( "The desktop app has ended, stopping the backend." );
         stop( context, stopping );
      } );
      StdinWatchdog.watch( context, () -> {
         LOG.info( "The desktop app requested a shutdown, stopping the backend." );
         stop( context, stopping );
      } );
   }

   private static void stop( final ApplicationContext context, final AtomicBoolean stopping ) {
      if ( stopping.compareAndSet( false, true ) ) {
         context.close();
         System.exit( 0 );
      }
   }
}

