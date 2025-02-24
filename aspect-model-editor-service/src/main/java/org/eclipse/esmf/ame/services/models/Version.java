package org.eclipse.esmf.ame.services.models;

import java.util.List;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;

/**
 * Represents a version containing a list of models.
 */
@Serdeable
@Introspected
public class Version {
   private String version;
   private List<Model> models;

   public Version() {
   }

   public Version( final String version, final List<Model> models ) {
      this.version = version;
      this.models = models;
   }

   public String getVersion() {
      return version;
   }

   public void setVersion( final String version ) {
      this.version = version;
   }

   public List<Model> getModels() {
      return models;
   }

   public void setModels( final List<Model> models ) {
      this.models = models;
   }
}