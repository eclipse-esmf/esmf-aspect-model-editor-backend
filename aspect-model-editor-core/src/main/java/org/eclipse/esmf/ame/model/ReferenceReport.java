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
package org.eclipse.esmf.ame.model;

import java.util.List;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;

/**
 * Result of the check whether a namespace or an Aspect Model file can be deleted. Only incoming references count:
 * files that use the elements to be deleted. What the deleted files use themselves does not matter.
 *
 * @param deletable true if no other file uses the elements and all other files could be checked
 * @param references the files that use the elements
 * @param unreadableFiles the files that could not be checked
 */
@Serdeable
@Introspected
public record ReferenceReport(
      boolean deletable,
      List<ModelReference> references,
      List<UnreadableModelFile> unreadableFiles
) {
   public static ReferenceReport empty() {
      return of( List.of(), List.of() );
   }

   public static ReferenceReport of( final List<ModelReference> references, final List<UnreadableModelFile> unreadableFiles ) {
      return new ReferenceReport( references.isEmpty() && unreadableFiles.isEmpty(), List.copyOf( references ),
            List.copyOf( unreadableFiles ) );
   }
}
