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

import java.net.URI;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import org.jspecify.annotations.Nullable;

/**
 * Response object containing an aspect model and its metadata.
 * <p>
 * This record encapsulates the content of an aspect model file along with
 * optional information about its source location.
 *
 * @param content the aspect model content as a string (e.g., in Turtle format)
 * @param sourceLocation a URI indicating the original location of the aspect model file
 */
@Serdeable
@Introspected
@JsonInclude( JsonInclude.Include.ALWAYS )
public record ModelResponse( String content, @Nullable URI sourceLocation ) {}

