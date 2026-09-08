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

import org.eclipse.esmf.aspectmodel.urn.AspectModelUrn;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;

/**
 * Represents a single aspect model within a namespace and version.
 *
 * @param name the model name or content
 * @param aspectModelUrn the URN of the aspect model
 * @param version the version of the model
 * @param exists indicates whether the model already exists in the workspace
 */
@Serdeable
@Introspected
public record NamespaceModel(
      String name,
      AspectModelUrn aspectModelUrn,
      String version,
      boolean exists
) {}
