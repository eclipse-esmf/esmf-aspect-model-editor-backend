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

package io.openmanufacturing.ame.resolver.inmemory;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.InputStream;
import java.nio.file.Path;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.StmtIterator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import io.openmanufacturing.ame.config.ApplicationSettings;
import io.openmanufacturing.sds.aspectmodel.resolver.services.TurtleLoader;
import io.openmanufacturing.sds.aspectmodel.urn.AspectModelUrn;
import io.vavr.control.Try;

@RunWith(MockitoJUnitRunner.class)
public class InMemoryOmpStrategyTest {
    private InMemoryStrategy inMemoryStrategy;

    @Mock
    private Try<Model> tryModel;

    @Mock
    private Model modelMock;

    @Mock
    private AspectModelUrn aspectModelUrnMock;

    @Mock
    private Resource resourceMock;

    @Mock
    private StmtIterator stmtIteratorMock;

    @Mock
    private Try<Model> isDirectory;

    @Before
    public void setUp() {
        try (final MockedStatic<TurtleLoader> turtleLoaderUtilities = Mockito.mockStatic(TurtleLoader.class)) {
            turtleLoaderUtilities.when(() -> TurtleLoader.loadTurtle(any(InputStream.class))).thenReturn(tryModel);

            when(tryModel.get()).thenReturn(modelMock);
            inMemoryStrategy = Mockito.spy(new InMemoryStrategy("turtle file content",
                    Path.of(ApplicationSettings.getMetaModelStoragePath())));
            doReturn(isDirectory).when( inMemoryStrategy ).getModelFromFileSystem(aspectModelUrnMock);
            when(isDirectory.isSuccess()).thenReturn(false);
        }
    }

    @Test
    public void testApplySuccess() {
        try (final MockedStatic<ResourceFactory> resourceFactoryUtilities = Mockito.mockStatic(ResourceFactory.class)) {
            resourceFactoryUtilities.when(() -> ResourceFactory.createResource(any(String.class))).thenReturn(resourceMock);

            when(ResourceFactory.createResource(any())).thenReturn(resourceMock);
            when(modelMock.listStatements(resourceMock, null, (RDFNode) null)).thenReturn(stmtIteratorMock);
            when(stmtIteratorMock.hasNext()).thenReturn(true);

            final Try<Model> result = inMemoryStrategy.apply(aspectModelUrnMock);

            assertEquals(result, Try.success(modelMock));
        }
    }

    @Test
    public void testApplyFailureNullAspectModelUrn() {
        final Try<Model> result = inMemoryStrategy.apply(null);

        assertTrue(result.isFailure());
    }

    @Test
    public void testApplyFailure() {
        try (final MockedStatic<ResourceFactory> resourceFactoryUtilities = Mockito.mockStatic(ResourceFactory.class)) {
            resourceFactoryUtilities.when(() -> ResourceFactory.createResource(any(String.class))).thenReturn(resourceMock);

            when(ResourceFactory.createResource(any())).thenReturn(resourceMock);
            when(modelMock.listStatements(resourceMock, null, (RDFNode) null)).thenReturn(stmtIteratorMock);
            when(stmtIteratorMock.hasNext()).thenReturn(false);

            final Try<Model> result = inMemoryStrategy.apply(aspectModelUrnMock);

            assertTrue(result.isFailure());
        }
    }
}
