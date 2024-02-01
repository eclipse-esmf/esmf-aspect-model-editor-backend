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

package org.eclipse.esmf.ame.repository;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;

import org.eclipse.esmf.ame.repository.config.TestConfig;
import org.eclipse.esmf.ame.repository.strategy.LocalFolderResolverStrategy;
import org.eclipse.esmf.ame.repository.strategy.ModelResolverStrategy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith( SpringExtension.class )
@SpringBootTest( classes = ModelResolverRepository.class )
@Import( TestConfig.class )
@ActiveProfiles( "test" )
class ModelResolverRepositoryTest {

   @Autowired
   private ModelResolverRepository repository;
   
   @Test
   void testGetStrategy() {
      final ModelResolverStrategy result = repository.getStrategy( LocalFolderResolverStrategy.class );

      assertInstanceOf( LocalFolderResolverStrategy.class, result );
   }

   @Test()
   void testGetStrategyHasEmptyStrategyList() {
      repository = new ModelResolverRepository( Collections.emptyList() );

      assertThrows( RuntimeException.class, () -> repository.getStrategy( LocalFolderResolverStrategy.class ) );
   }
}
