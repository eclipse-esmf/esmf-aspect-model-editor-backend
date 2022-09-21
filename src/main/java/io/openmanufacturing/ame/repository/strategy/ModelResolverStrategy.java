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

package io.openmanufacturing.ame.repository.strategy;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nonnull;

import io.openmanufacturing.ame.repository.model.LocalPackageInfo;
import io.openmanufacturing.sds.aspectmodel.urn.AspectModelUrn;

public interface ModelResolverStrategy {

   /**
    * Returns true or false if model exist.
    *
    * @param namespace - used to extract file path.
    * @param storagePath - path to storage files.
    */
   Boolean checkModelExist( @Nonnull final String namespace, final String storagePath );

   /**
    * Returns turtleData based on received namespace.
    * ex: io.openmanufacturing:1.0.0:AspectDefault.ttl
    *
    * @param namespace - used to extract file path.
    * @param storagePath - path to storage files.
    * @return the Aspect Model turtleData as {@link String}.
    */
   String getModelAsString( @Nonnull final String namespace, final String storagePath );

   /**
    * Returns the path of the Aspect Model.
    * ex: io.openmanufacturing/1.0.0/AspectDefault.ttl
    *
    * @param namespace - used to extract file path.
    * @param storagePath - path to storage files.
    * @return the file location of the saved turtleData.
    */
   File getModelAsFile( @Nonnull final String namespace, final String storagePath );

   /**
    * Save given turtleData into repository. File path will be decided based on urn if exists, if not it will be
    * extracted from turtleData.
    *
    * @param urn - used to extract file path.
    * @param turtleData - content of the saved file.
    * @param storagePath - path to storage files.
    * @return the file location of the saved turtleData.
    */
   String saveModel( Optional<String> urn, @Nonnull final String turtleData, final String storagePath );

   /**
    * Deletes the folder from the given namespace.
    * ex: io.openmanufacturing:1.0.0:AspectDefault.ttl
    *
    * @param namespace - used to extract filePath.
    * @param storagePath - path to storage files.
    */
   void deleteModel( @Nonnull final String namespace, final String storagePath );

   /**
    * Returns a map of key = namespace + version and value = list of turtle files that are present in that namespace.
    *
    * @param shouldRefresh - boolean when the list have to refresh.
    * @param storagePath - path to storage files.
    */
   Map<String, List<String>> getAllNamespaces( boolean shouldRefresh, final String storagePath );

   /**
    * Returns the {@link LocalPackageInfo} which have the information of valid and un-valid files in the package.
    *
    * @param storagePath - path to storage files.
    */
   LocalPackageInfo getLocalPackageInformation( final String storagePath );

   /**
    * Returns the converted {@link AspectModelUrn} from the file that is provided.
    *
    * @param inputFile - file from workspace.
    */
   AspectModelUrn convertFileToUrn( final File inputFile );
}
