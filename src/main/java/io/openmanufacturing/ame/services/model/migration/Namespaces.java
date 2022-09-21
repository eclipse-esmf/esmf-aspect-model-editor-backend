package io.openmanufacturing.ame.services.model.migration;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class Namespaces {

   @JsonInclude( JsonInclude.Include.NON_NULL )
   @JsonProperty( "namespaces" )
   public List<Namespace> namespaces;

   public Namespaces() {
      namespaces = new ArrayList<>();
   }

   public void addNamespace( final Namespace namespace ) {
      namespaces.add( namespace );
   }
}
