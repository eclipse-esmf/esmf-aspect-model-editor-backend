/*
 * Copyright (c) 2022 Robert Bosch Manufacturing Solutions GmbH
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

package io.openmanufacturing.ame;

import java.lang.reflect.Field;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.util.ReflectionUtils;

import io.openmanufacturing.sds.aspectmodel.shacl.constraint.JsConstraint;
import io.openmanufacturing.sds.aspectmodel.versionupdate.MigratorService;
import io.openmanufacturing.sds.aspectmodel.versionupdate.MigratorServiceLoader;

@SpringBootApplication
public class Application {
   public static void main( final String[] args ) {
      // Since we have no rights with the local service in the class path we have to use the MigratorService ourselves.
      final MigratorServiceLoader migratorService = new MigratorServiceLoader();
      final Field migratorServiceRef = ReflectionUtils.findField( MigratorServiceLoader.class, "migratorService" );
      final Field instanceRef = ReflectionUtils.findField( MigratorServiceLoader.class, "instance" );
      migratorServiceRef.setAccessible( true );
      ReflectionUtils.setField( migratorServiceRef, migratorService, new MigratorService() );
      instanceRef.setAccessible( true );
      ReflectionUtils.setField( instanceRef, migratorService, migratorService );

      // Spring and GraalVM cannot launch Javascript engines at the moment, so this must be disabled for now.
      JsConstraint.setEvaluateJavaScript( false );

      SpringApplication.run( Application.class, args );
   }
}
