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

package org.eclipse.esmf.ame.model;

import java.nio.file.Path;
import java.util.Arrays;

import org.eclipse.esmf.ame.config.ApplicationSettings;

import lombok.Getter;

@Getter
public enum StoragePath {
   AspectModel( ApplicationSettings.getAspectModelEditorStoragePath() ),
   MetaModel( ApplicationSettings.getMetaModelStoragePath() );

   private final Path path;

   StoragePath( final Path path ) {
      this.path = path;
   }

   public static StoragePath getEnum( final String value ) {
      return Arrays.stream( values() )
                   .filter( v -> v.getPath().toString().equalsIgnoreCase( value ) )
                   .findFirst()
                   .orElseThrow( IllegalArgumentException::new );
   }
}
