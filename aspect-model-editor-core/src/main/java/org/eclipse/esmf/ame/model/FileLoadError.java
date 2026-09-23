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

/**
 * Represents an error encountered when loading an Aspect Model file.
 *
 * @param fileIdentifier the identifier of the file being loaded
 * @param sourceDocument the source document location (URI or path) where the error occurred
 * @param message the detailed error message
 */
public record FileLoadError(
      String fileIdentifier,
      String sourceDocument,
      String message
) {}
