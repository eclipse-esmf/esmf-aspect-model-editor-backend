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

package org.eclipse.esmf.ame.api.model.response;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

class ErrorResponseTest {

   private final ObjectMapper objectMapper = new ObjectMapper();

   @Test
   void testErrorResponseWithoutFocusNode() throws JsonProcessingException {
      final Error error = new Error( "Not found", "/ame/api/models", 404 );
      final ErrorResponse response = new ErrorResponse( error );

      assertEquals( error, response.error() );
      assertNull( response.error().focusNode() );

      final String json = objectMapper.writeValueAsString( response );
      assertFalse( json.contains( "focusNode" ) );
   }

   @Test
   void testErrorResponseWithFocusNode() throws JsonProcessingException {
      final String urn = "urn:samm:org.eclipse.esmf.example:1.0.0#speed";
      final Error error = new Error( "Parsing failed", "/ame/api/models/validate", 400, urn );
      final ErrorResponse response = new ErrorResponse( error );

      assertEquals( error, response.error() );
      assertEquals( urn, response.error().focusNode() );

      final String json = objectMapper.writeValueAsString( response );
      assertTrue( json.contains( "\"focusNode\":\"urn:samm:org.eclipse.esmf.example:1.0.0#speed\"" ) );
   }
}
