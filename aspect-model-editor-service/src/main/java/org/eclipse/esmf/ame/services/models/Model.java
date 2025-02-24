package org.eclipse.esmf.ame.services.models;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;

/**
 * Represents a single model with its name or properties.
 */
@Serdeable
@Introspected
public class Model {
   private String model;
   private boolean existing;

   public Model() {
   }

   public Model( final String model, final boolean existing ) {
      this.model = model;
      this.existing = existing;
   }

   public String getModel() {
      return model;
   }

   public boolean isExisting() {
      return existing;
   }
}