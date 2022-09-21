package io.openmanufacturing.ame.services.model.migration;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AspectModelFile {

   @JsonInclude( JsonInclude.Include.NON_NULL )
   @JsonProperty( "name" )
   public String name;

   @JsonInclude( JsonInclude.Include.NON_NULL )
   @JsonProperty( "success" )
   public Boolean success;

   public AspectModelFile( final String name ) {
      this.name = name;
   }
}
