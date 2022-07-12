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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import io.openmanufacturing.ame.config.ApplicationSettings;
import io.openmanufacturing.ame.repository.strategy.LocalFolderResolverStrategy;
import io.openmanufacturing.ame.repository.strategy.ModelResolverStrategy;

@RunWith(MockitoJUnitRunner.class)
public class ModelResolverRepositoryTest {

    private ModelResolverRepository modelResolverRepository;

    @Test
    public void testGetStrategy() {

        final LocalFolderResolverStrategy localFolderResolverStrategy = new LocalFolderResolverStrategy(mock(ApplicationSettings.class));
        modelResolverRepository = new ModelResolverRepository(Collections.singletonList(localFolderResolverStrategy));

        final ModelResolverStrategy result = modelResolverRepository.getStrategy(LocalFolderResolverStrategy.class);

        assertEquals(result, localFolderResolverStrategy);
    }

    @Test(expected = RuntimeException.class)
    public void testGetStrategyHasEmptyStrategyList() {

        modelResolverRepository = new ModelResolverRepository(Collections.emptyList());

        modelResolverRepository.getStrategy(LocalFolderResolverStrategy.class);
    }
}
