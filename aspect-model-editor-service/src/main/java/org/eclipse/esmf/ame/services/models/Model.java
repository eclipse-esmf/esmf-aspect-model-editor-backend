/*
 * Copyright (c) 2025 Robert Bosch Manufacturing Solutions GmbH
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
 * Represents a single model with its name or properties.
 *
 * @param model the model name or content
 * @param aspectModelUrn the URN of the aspect model
 * @param version the version of the model
 * @param existing indicates whether the model already exists
 */
@Serdeable
@Introspected
public record Model(
      String model,
      AspectModelUrn aspectModelUrn,
      String version,
      boolean existing
) {}
