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

package org.eclipse.esmf.ame.repository.config;

import java.nio.file.FileSystem;
import java.nio.file.Path;

import org.eclipse.esmf.ame.config.ApplicationSettings;
import org.eclipse.esmf.ame.repository.strategy.LocalFolderResolverStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import com.github.marschall.memoryfilesystem.MemoryFileSystemBuilder;

import lombok.SneakyThrows;

@TestConfiguration
@Import( ApplicationSettings.class )
public class TestConfig {
   @Autowired
   private ApplicationSettings applicationSettings;

   @Bean
   public LocalFolderResolverStrategy localFolderResolverStrategy() {
      return new LocalFolderResolverStrategy( applicationSettings, importFileSystem(), modelPath() );
   }

   @SneakyThrows
   @Bean
   public FileSystem importFileSystem() {
      return MemoryFileSystemBuilder.newEmpty().build();
   }

   @Bean
   public String modelPath() {
      return Path.of( "src", "test", "resources", "strategy" ).toAbsolutePath().toString();
   }
}
