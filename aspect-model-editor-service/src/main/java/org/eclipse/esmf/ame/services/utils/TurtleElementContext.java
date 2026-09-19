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

package org.eclipse.esmf.ame.services.utils;

import java.util.Optional;

/**
 * Contextual information resolved from a Turtle document location.
 *
 * @param elementUrn the fully-qualified Aspect Model URN of the enclosing subject, if resolved
 * @param predicate the predicate/property name at or enclosing the location, if resolved
 * @param line the 1-based line number
 * @param column the 1-based column number
 */
public record TurtleElementContext(
      Optional<String> elementUrn,
      Optional<String> predicate,
      long line,
      long column
) {}
