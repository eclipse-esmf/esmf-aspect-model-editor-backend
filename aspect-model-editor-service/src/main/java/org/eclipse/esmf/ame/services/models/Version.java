package org.eclipse.esmf.ame.services.models;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a version containing a list of models.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Version {
   @JsonProperty( "version" )
   private String version;

   @JsonProperty( "models" )
   private List<Model> models;
}