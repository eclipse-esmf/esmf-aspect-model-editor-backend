/*
 * Copyright (c) 2024 Robert Bosch Manufacturing Solutions GmbH
 *
 * See the AUTHORS file(s) distributed with this work for additional
 * information regarding authorship.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package org.eclipse.esmf.ame.nativefeatures;

import static org.eclipse.esmf.nativefeatures.AssetAdministrationShellFeature.ADMINSHELL_PROPERTIES;

import java.util.List;

import org.eclipse.esmf.nativefeatures.EsmfFeature;
import org.eclipse.esmf.nativefeatures.Native;
import org.eclipse.esmf.substitution.AdminShellConfig;

import org.graalvm.nativeimage.hosted.Feature;

public class AmeFeature implements Feature {
   @Override
   public void beforeAnalysis( final BeforeAnalysisAccess access ) {
      Native.forClass( AdminShellConfig.class ).initializeAtBuildTime();
      // Native.forClass( AWTMeasureText.class ).initializeAtBuildTime(); TODO do this in pom.xml as at run time ...

      Native.addResource( "application.properties" );
      Native.addResource( "git.properties" );
      Native.addResource( "pom.properties" );
      Native.addResource( "logback.xml" );
      Native.addResource( ADMINSHELL_PROPERTIES );
   }

   @Override
   public List<Class<? extends Feature>> getRequiredFeatures() {
      return List.of( EsmfFeature.class );
   }
}
