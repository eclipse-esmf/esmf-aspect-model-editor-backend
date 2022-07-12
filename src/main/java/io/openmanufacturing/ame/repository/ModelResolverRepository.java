/*
 * Copyright (c) 2022 Robert Bosch Manufacturing Solutions GmbH
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

package io.openmanufacturing.ame.repository;

import java.util.List;

import org.springframework.stereotype.Service;

import io.openmanufacturing.ame.repository.strategy.ModelResolverStrategy;

@Service
public class ModelResolverRepository {

    private final List<ModelResolverStrategy> modelRetrievingStrategies;

    public ModelResolverRepository(final List<ModelResolverStrategy> modelStrategies) {
        modelRetrievingStrategies = modelStrategies;
    }

    public ModelResolverStrategy getStrategy( final Class<? extends ModelResolverStrategy> modelClass) {
        return modelRetrievingStrategies.stream()
                .filter(modelStrategy -> modelStrategy.getClass() == modelClass)
                .findFirst().orElseThrow(() -> new RuntimeException("Repository not implemented"));
    }
}
