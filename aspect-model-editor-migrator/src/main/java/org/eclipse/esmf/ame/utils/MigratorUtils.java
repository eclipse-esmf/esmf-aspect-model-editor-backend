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

package org.eclipse.esmf.ame.utils;

import org.eclipse.esmf.ame.exceptions.InvalidAspectModelException;
import org.eclipse.esmf.ame.resolver.strategy.FileSystemStrategy;
import org.eclipse.esmf.ame.resolver.strategy.utils.ResolverUtils;
import org.eclipse.esmf.aspectmodel.resolver.services.VersionedModel;
import org.eclipse.esmf.aspectmodel.versionupdate.MigratorService;

import io.vavr.control.Try;

public class MigratorUtils {
   /**
    * Migrates a model to its latest version.
    *
    * @param aspectModel as a string.
    * @return migrated Aspect Model as a string.
    */
   public static String migrateModel( final String aspectModel ) throws InvalidAspectModelException {
      final FileSystemStrategy fileSystemStrategy = new FileSystemStrategy( aspectModel );

      final Try<VersionedModel> migratedFile = new MigratorService().updateMetaModelVersion(
            ResolverUtils.loadModelFromStoragePath( fileSystemStrategy ) );

      final VersionedModel versionedModel = migratedFile.getOrElseThrow(
            error -> new InvalidAspectModelException( "Aspect Model cannot be migrated.", error ) );

      return ResolverUtils.getPrettyPrintedVersionedModel( versionedModel,
            fileSystemStrategy.getAspectModelUrn().getUrn() );
   }
}
