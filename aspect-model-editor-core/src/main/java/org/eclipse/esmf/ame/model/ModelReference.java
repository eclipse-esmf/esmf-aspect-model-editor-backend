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

import java.util.List;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;

/**
 * A workspace file that uses elements of a namespace or of an Aspect Model file that is about to be deleted.
 *
 * @param namespace the namespace of the referencing file
 * @param version the version of the referencing file
 * @param fileName the name of the referencing file
 * @param referencedElements the URNs of the used elements, sorted
 */
@Serdeable
@Introspected
public record ModelReference(
      String namespace,
      String version,
      String fileName,
      List<String> referencedElements
) {}
