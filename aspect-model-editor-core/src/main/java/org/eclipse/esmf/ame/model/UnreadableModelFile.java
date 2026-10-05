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

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;

/**
 * A workspace file that could not be read while checking references. It might use the elements to be deleted,
 * so deleting is not allowed until it is fixed or removed.
 *
 * @param namespace the namespace of the file
 * @param version the version of the file
 * @param fileName the name of the file
 * @param message why the file could not be read
 */
@Serdeable
@Introspected
public record UnreadableModelFile(
      String namespace,
      String version,
      String fileName,
      String message
) {}
