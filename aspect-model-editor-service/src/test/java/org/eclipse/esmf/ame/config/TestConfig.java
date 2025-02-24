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

package org.eclipse.esmf.ame.config;

import java.nio.file.Path;

import org.eclipse.esmf.aspectmodel.shacl.constraint.JsConstraint;
import org.eclipse.esmf.aspectmodel.validation.services.AspectModelValidator;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Factory
public class TestConfig {
   @Inject
   private ApplicationSettings applicationSettings;

   @Bean
   @Singleton
   public AspectModelValidator getAspectModelValidator() {
      JsConstraint.setEvaluateJavaScript( false );
      return new AspectModelValidator();
   }

   @Bean
   @Singleton
   public String modelPath() {
      return Path.of( "src", "test", "resources", "services" ).toAbsolutePath().toString();
   }
}
