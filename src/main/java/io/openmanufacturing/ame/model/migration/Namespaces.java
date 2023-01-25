package io.openmanufacturing.ame.model.migration;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Namespaces {

   @JsonInclude( JsonInclude.Include.NON_NULL )
   @JsonProperty( "namespaces" )
   public List<Namespace> namespaces;
}
