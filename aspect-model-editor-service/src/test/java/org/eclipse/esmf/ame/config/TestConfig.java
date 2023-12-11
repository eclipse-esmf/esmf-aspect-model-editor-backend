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

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.List;

import org.eclipse.esmf.ame.repository.ModelResolverRepository;
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

   @SneakyThrows
   @Bean
   public ModelResolverRepository modelResolverRepository() {
      FileSystem fileSystem = MemoryFileSystemBuilder.newEmpty().build();
      String rootPath = Path.of( "src", "test", "resources", "services" ).toAbsolutePath().toString();

      LocalFolderResolverStrategy localFolderResolverStrategy = new LocalFolderResolverStrategy( applicationSettings,
            fileSystem, rootPath );
      return new ModelResolverRepository( List.of( localFolderResolverStrategy ) );
   }
}
