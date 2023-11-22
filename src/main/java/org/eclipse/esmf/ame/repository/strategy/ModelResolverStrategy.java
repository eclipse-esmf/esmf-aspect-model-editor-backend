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

import io.vavr.Tuple2;

public interface ModelResolverStrategy {
   /**
    * Checks if a model exists in the specified namespace and filename.
    *
    * @param namespace The namespace used to extract the file path.
    * @param filename The name of the file to check.
    * @return {@code true} if the model exists, {@code false} otherwise.
    */
   Boolean checkModelExist( @Nonnull final String namespace, @Nonnull final String filename );

   /**
    * Retrieves a model as a string based on the given namespace and filename.
    *
    * @param namespace The namespace used to extract the file path.
    * @param filename The name of the file.
    * @return The model data in turtle format as a {@link String}.
    */
   String getModelAsString( @Nonnull final String namespace, @Nonnull final String filename );

   /**
    * Retrieves the file path of a model based on the given namespace and filename.
    *
    * @param namespace The namespace used to extract the file path.
    * @param filename The name of the file.
    * @return The {@link File} object representing the location of the model.
    */
   File getModelAsFile( @Nonnull final String namespace, @Nonnull final String filename );

   /**
    * Saves the provided turtleData into the repository. The file path is determined based on the URN if it exists,
    * or extracted from the turtleData.
    *
    * @param namespace The optional namespace used to extract the file path.
    * @param fileName The optional file name of the Aspect Model.
    * @param turtleData The content of the file to be saved.
    * @return The file location of the saved turtleData.
    */
   String saveModel( Optional<String> namespace, Optional<String> fileName, @Nonnull final String turtleData );

   /**
    * Deletes the model from the specified namespace.
    *
    * @param namespace The namespace used to extract the file path.
    * @param fileName The file name of the Aspect Model to be deleted.
    */
   void deleteModel( @Nonnull final String namespace, final String fileName );

   /**
    * Retrieves a mapping of namespaces to a list of turtle files present in each namespace.
    *
    * @param shouldRefresh A boolean indicating whether the list should be refreshed.
    * @return A map where each key is a combination of namespace and version, and the value is a list of turtle files.
    */
   Map<String, List<String>> getAllNamespaces( boolean shouldRefresh );

   /**
    * Gets information about all imported Aspect Models.
    *
    * @return A list of {@link AspectModelInformation} containing information about each imported Aspect Model.
    */
   List<AspectModelInformation> getImportedAspectModelInformation();

   /**
    * Converts the provided file to a tuple representing the Aspect Model URN.
    *
    * @param inputFile The file from the workspace to be converted.
    * @return A {@link Tuple2} where the first element is the namespace and the second element is the version.
    */
   Tuple2<String, String> convertFileToTuple( final File inputFile );

   /**
    * Locks the specified file within the given namespace.
    * This method attempts to acquire a lock on the file identified by the
    * namespace and fileName parameters. If the lock is successfully acquired,
    * the method returns {@code true}. If the file is already locked,
    * the method should return {@code false}, indicating that the lock could
    * not be acquired.
    *
    * @param namespace The namespace in which the file resides. This parameter cannot be {@code null}.
    * @param fileName The name of the file to be locked. This parameter cannot be {@code null}.
    * @return {@code true} if the file was successfully locked,
    *       {@code false} otherwise.
    *
    * @throws IllegalArgumentException if either namespace or fileName is {@code null} or not valid.
    */
   boolean lockFile( @Nonnull final String namespace, @Nonnull final String fileName );

   /**
    * Unlocks the specified file within the given namespace.
    * This method attempts to release the lock on the file identified by the
    * namespace and fileName parameters. If the lock is successfully released,
    * the method returns {@code true}. If the file is not currently locked,
    * or if the lock is held by another entity, the method should return
    * {@code false}, indicating that the unlock operation was unsuccessful.
    *
    * @param namespace The namespace in which the file resides. This parameter cannot be {@code null}.
    * @param fileName The name of the file to be unlocked. This parameter cannot be {@code null}.
    * @return {@code true} if the file was successfully unlocked, {@code false} otherwise.
    *
    * @throws IllegalArgumentException if either namespace or fileName is {@code null} or not valid.
    */
   boolean unlockFile( @Nonnull final String namespace, @Nonnull final String fileName );
}
