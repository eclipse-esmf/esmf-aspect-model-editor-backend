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

import com.fasterxml.jackson.annotation.JsonInclude;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;

/**
 * Represents information about an Aspect Model file.
 *
 * @param absoluteName the absolute path of the file
 * @param aspectModelUrn the URN of the aspect model
 * @param modelVersion the version of the model
 * @param aspectModel the aspect model content
 * @param fileName the name of the file
 */
@Serdeable
@Introspected
@JsonInclude( JsonInclude.Include.ALWAYS )
public record FileInformation(
      String absoluteName,
      String aspectModelUrn,
      String modelVersion,
      String aspectModel,
      String fileName
) {}