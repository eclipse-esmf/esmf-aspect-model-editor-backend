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

import java.nio.file.Path;

import io.micronaut.serde.annotation.Serdeable;

/**
 * Response object containing the models storage path.
 *
 * @param path the absolute storage path
 * @param storagePath alias for path
 */
@Serdeable
public record StoragePathResponse(
      String path,
      String storagePath
) {
   public StoragePathResponse( final Path modelPath ) {
      this( modelPath.toAbsolutePath().toString(), modelPath.toAbsolutePath().toString() );
   }
}
