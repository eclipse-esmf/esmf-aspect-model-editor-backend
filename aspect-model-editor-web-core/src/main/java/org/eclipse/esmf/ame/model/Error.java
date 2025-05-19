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

import io.micronaut.serde.annotation.SerdeImport;

/**
 * Represents an error object containing details about an error.
 * <p>
 * This record is used to encapsulate error information such as the error message,
 * the path where the error occurred, and the error code.
 * </p>
 *
 * @param message The error message providing details about the error.
 * @param path The path or location associated with the error.
 * @param code The error code representing the type or category of the error.
 */
@SerdeImport( Error.class )
public record Error( String message, String path, int code ) {
}