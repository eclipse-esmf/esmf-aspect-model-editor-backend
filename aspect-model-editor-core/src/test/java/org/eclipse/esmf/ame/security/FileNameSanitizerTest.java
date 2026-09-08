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

package org.eclipse.esmf.ame.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class FileNameSanitizerTest {

   private FileNameSanitizer sanitizer;

   @BeforeEach
   void setUp() {
      sanitizer = new FileNameSanitizer();
   }

   @Test
   void testSanitizeValidFileName() {
      assertEquals( "Movement.ttl", sanitizer.sanitize( "Movement.ttl" ) );
      assertEquals( "valid-file_name.123.ttl", sanitizer.sanitize( "valid-file_name.123.ttl" ) );
      assertEquals( "urn:samm:org.eclipse.esmf.example:1.0.0#Movement", sanitizer.sanitize( "urn:samm:org.eclipse.esmf.example:1.0.0#Movement" ) );
   }

   @ParameterizedTest
   @NullAndEmptySource
   @ValueSource( strings = { "   ", "\t", "\n" } )
   void testSanitizeNullOrBlankThrowsIllegalArgumentException( final String input ) {
      final IllegalArgumentException ex = assertThrows( IllegalArgumentException.class, () -> sanitizer.sanitize( input ) );
      assertTrue( ex.getMessage().contains( "must not be null or empty" ) );
   }

   @ParameterizedTest
   @ValueSource( strings = {
         "../test.ttl",
         "folder/test.ttl",
         "folder\\test.ttl",
         "..\\test.ttl",
         "test\0null.ttl",
         "test%20encoded.ttl"
   } )
   void testSanitizePathTraversalThrowsIllegalArgumentException( final String maliciousInput ) {
      final IllegalArgumentException ex = assertThrows( IllegalArgumentException.class, () -> sanitizer.sanitize( maliciousInput ) );
      assertTrue( ex.getMessage().contains( "must not contain directory separators" ) );
   }
}

