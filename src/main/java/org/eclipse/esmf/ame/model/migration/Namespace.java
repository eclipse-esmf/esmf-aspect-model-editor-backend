package org.eclipse.esmf.ame.model.migration;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Namespace {
   @JsonInclude( JsonInclude.Include.NON_NULL )
   @JsonProperty( "namespace" )
   public String versionedNamespace;

   @JsonInclude( JsonInclude.Include.NON_NULL )
   @JsonProperty( "files" )
   public List<FileInformation> files = new ArrayList<>();

   public Namespace( final String versionedNamespace ) {
      this.versionedNamespace = versionedNamespace;
   }

   public void addAspectModelFile( final FileInformation file ) {
      files.add( file );
   }
}
