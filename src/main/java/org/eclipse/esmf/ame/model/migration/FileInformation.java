package org.eclipse.esmf.ame.model.migration;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FileInformation {

   @JsonInclude( JsonInclude.Include.NON_NULL )
   @JsonProperty( "name" )
   public String name;

   @JsonInclude( JsonInclude.Include.NON_NULL )
   @JsonProperty( "success" )
   public Boolean success;
}
