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
package org.eclipse.esmf.ame.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.eclipse.esmf.ame.model.ModelReference;
import org.eclipse.esmf.ame.model.ReferenceReport;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import org.junit.jupiter.api.Test;

class ModelReferencedExceptionHandlerTest {

   @Test
   void respondsWithConflictAndTheReferenceReport() {
      final ReferenceReport report = ReferenceReport.of(
            List.of( new ModelReference( "org.other", "1.0.0", "B.ttl", List.of( "urn:samm:org.example:1.0.0#Prop" ) ) ), List.of() );

      final HttpResponse<ReferenceReport> response = new ModelReferencedExceptionHandler()
            .handle( HttpRequest.DELETE( "/ame/api/models" ), new ModelReferencedException( "used", report ) );

      assertEquals( HttpStatus.CONFLICT, response.getStatus() );
      assertEquals( report, response.body() );
   }
}
