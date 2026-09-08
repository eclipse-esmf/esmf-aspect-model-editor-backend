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

import java.net.URI;

import io.micronaut.serde.annotation.Serdeable;
import org.jspecify.annotations.Nullable;

/**
 * Response object containing aspect model data.
 *
 * @param content The turtle content of the aspect model
 * @param sourceLocation Optional source location of the model file
 */
@Serdeable
public record AspectModelResponse(
      String content,
      @Nullable URI sourceLocation
) {}

