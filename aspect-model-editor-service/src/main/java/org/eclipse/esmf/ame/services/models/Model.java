package org.eclipse.esmf.ame.services.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single model with its name or properties.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Model {
   @JsonProperty( "model" )
   private String model;

   @JsonProperty( "existing" )
   private boolean existing;
}