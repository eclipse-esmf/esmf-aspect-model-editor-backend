/*
 * Copyright (c) 2023 Robert Bosch Manufacturing Solutions GmbH
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

package org.eclipse.esmf.ame.repository.strategy;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nonnull;

import org.eclipse.esmf.ame.model.repository.AspectModelInformation;
import org.eclipse.esmf.aspectmodel.urn.AspectModelUrn;

import io.vavr.Tuple2;

public interface ModelResolverStrategy {

   /**
    * Returns true or false if model exist.
    *
    * @param namespace - used to extract file path.
    * @param filename - file name of the file.y
    */
   Boolean checkModelExist( @Nonnull final String namespace, @Nonnull final String filename );

   /**
    * Returns turtleData based on received namespace.
    * ex: org.eclipse.esmf.samm:1.0.0:AspectDefault.ttl
    *
    * @param namespace - used to extract file path.
    * @param filename - file name of the file.
    * @return the Aspect Model turtleData as {@link String}.
    */
   String getModelAsString( @Nonnull final String namespace, @Nonnull final String filename );

   /**
    * Returns the path of the Aspect Model.
    * ex: org.eclipse.esmf/1.0.0/AspectDefault.ttl
    *
    * @param namespace - used to extract file path.
    * @param filename - file name of the file.
    * @return the file location of the saved turtleData.
    */
   File getModelAsFile( @Nonnull final String namespace, @Nonnull final String filename );

   /**
    * Save given turtleData into repository. File path will be decided based on urn if exists, if not it will be
    * extracted from turtleData.
    *
    * @param namespace - used to extract file path.
    * @param fileName - file name of the Aspect Model.
    * @param turtleData - content of the saved file.
    * @return the file location of the saved turtleData.
    */
   String saveModel( Optional<String> namespace, Optional<String> fileName, @Nonnull final String turtleData );

   /**
    * Deletes the folder from the given namespace.
    * ex: org.eclipse.esmf.samm:1.0.0:AspectDefault.ttl
    *
    * @param namespace - used to extract filePath.
    * @param fileName - file name of the Aspect Model.
    */
   void deleteModel( @Nonnull final String namespace, final String fileName );

   /**
    * Returns a map of key = namespace + version and value = list of turtle files that are present in that namespace.
    *
    * @param shouldRefresh - boolean when the list have to refresh.
    */
   Map<String, List<String>> getAllNamespaces( boolean shouldRefresh );

   /**
    * Returns an {@link List<AspectModelInformation>} which have the information of all Aspect Models which will be
    * imported.
    */
   List<AspectModelInformation> getImportedAspectModelInformation();

   /**
    * Returns the converted {@link AspectModelUrn} from the file that is provided.
    *
    * @param inputFile - file from workspace.
    */
   Tuple2<String, String> convertFileToTuple( final File inputFile );
}
