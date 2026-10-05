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
package org.eclipse.esmf.ame.services.models;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;

/**
 * Result of clearing the workspace.
 *
 * @param deletedFiles the number of deleted Aspect Model files
 * @param backupCreated true if a backup of the workspace was created before
 */
@Serdeable
@Introspected
public record ClearWorkspaceResult(
      int deletedFiles,
      boolean backupCreated
) {}
