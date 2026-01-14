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

package org.eclipse.esmf.ame.model;

import io.micronaut.serde.annotation.Serdeable;

/**
 * Represents a response containing error details.
 * <p>
 * This record encapsulates an {@link Error} object, which provides information
 * about an error that occurred during the application's execution.
 * </p>
 *
 * @param error The {@link Error} object containing details about the error.
 */
@Serdeable
public record ErrorResponse( Error error ) {}