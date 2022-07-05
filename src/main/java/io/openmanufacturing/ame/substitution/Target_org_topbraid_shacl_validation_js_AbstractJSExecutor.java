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

package io.openmanufacturing.ame.substitution;

import java.util.Collection;

import org.apache.jena.rdf.model.RDFNode;
import org.topbraid.shacl.engine.Constraint;
import org.topbraid.shacl.validation.ValidationEngine;
import org.topbraid.shacl.validation.js.AbstractJSExecutor;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * This is a GraalVM substitution class (see https://blog.frankel.ch/coping-incompatible-code-graalvm-compilation/#substitutions)
 * for {@link AbstractJSExecutor}.
 * Reason: The original class has a dynamic discovery of JavaScript engines, including Nashorn on JDK &lt; 15. These
 * will not work in GraalVM, so we exclude it here.
 * TODO Should be removed after a new SDS-SDK - shacl update.
 */
@TargetClass( AbstractJSExecutor.class )
@SuppressWarnings( {
      "unused",
      "squid:S00101" // Class name uses GraalVM substitution class naming schema, see
      // https://github.com/oracle/graal/tree/master/substratevm/src/com.oracle.svm.core/src/com/oracle/svm/core/jdk
} )
public final class Target_org_topbraid_shacl_validation_js_AbstractJSExecutor {
   private Target_org_topbraid_shacl_validation_js_AbstractJSExecutor() {
   }

   @Substitute
   private void executeConstraint(
         final Constraint constraint, final ValidationEngine validationEngine, final Collection<RDFNode> focusNodes ) {
   }
}
