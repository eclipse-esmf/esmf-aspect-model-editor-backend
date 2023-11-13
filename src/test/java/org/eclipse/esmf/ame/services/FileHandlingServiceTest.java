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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith( SpringExtension.class )
@SpringBootTest
@DirtiesContext( classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD )
@ActiveProfiles( "test" )
class FileHandlingServiceTest {

   @Autowired
   private FileHandlingService fileHandlingService;

   private static final String VERSION = "1.0.0";
   private static final String EXAMPLE_NAMESPACE = "org.eclipse.esmf.example";
   private static final String NAMESPACE_VERSION = EXAMPLE_NAMESPACE + ":" + VERSION;

   private static final Path RESOURCE_PATH = Path.of( "src", "test", "resources", "services" );
   private static final Path TEST_NAMESPACE_PATH = Path.of( RESOURCE_PATH.toString(), NAMESPACE_VERSION );

   private static final String TEST_MODEL = "AspectModelForService.ttl";

   @Test
   void testLockAndUnlockFile() {
      String lockResult = fileHandlingService.lockFile( NAMESPACE_VERSION, TEST_MODEL );
      assertEquals( lockResult, "File is locked" );

      String unlockResult = fileHandlingService.unlockFile( NAMESPACE_VERSION, TEST_MODEL );
      assertEquals( unlockResult, "File is unlocked" );
   }

   @Test()
   void testCannotBeUnlockFile() {
      String unlockResult = fileHandlingService.unlockFile( NAMESPACE_VERSION, TEST_MODEL );
      assertEquals( unlockResult, "File cannot be unlocked" );
   }
}
