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

package org.eclipse.esmf.ame.services;

import org.eclipse.esmf.ame.repository.ModelResolverRepository;
import org.eclipse.esmf.ame.repository.strategy.LocalFolderResolverStrategy;
import org.eclipse.esmf.ame.repository.strategy.ModelResolverStrategy;
import org.springframework.stereotype.Service;

@Service
public class FileHandlingService {
   private final ModelResolverRepository modelResolverRepository;

   public FileHandlingService( final ModelResolverRepository modelResolverRepository ) {
      this.modelResolverRepository = modelResolverRepository;
   }

   public String lockFile( String namespace, String filename ) {
      final ModelResolverStrategy strategy = modelResolverRepository.getStrategy( LocalFolderResolverStrategy.class );

      return strategy.lockFile( namespace, filename ) ? "File is locked" : "File cannot be locked";
   }

   public String unlockFile( String namespace, String filename ) {
      final ModelResolverStrategy strategy = modelResolverRepository.getStrategy( LocalFolderResolverStrategy.class );

      return strategy.unlockFile( namespace, filename ) ? "File is unlocked" : "File cannot be unlocked";
   }
}
