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

package org.eclipse.esmf.ame;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ReadyAnnouncerTest {
   @Test
   void readyLineContainsThePort() {
      // The desktop app parses exactly this format, see backend.rs in the editor
      assertEquals( "AME_BACKEND_READY port=30123", ReadyAnnouncer.readyLine( 30123 ) );
   }
}
