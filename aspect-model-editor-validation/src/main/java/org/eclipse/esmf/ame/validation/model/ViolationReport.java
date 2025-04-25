package org.eclipse.esmf.ame.validation.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotNull;

@Serdeable
@JsonInclude( JsonInclude.Include.ALWAYS )
public record ViolationReport
      ( @JsonProperty( "violationErrors" ) List<ViolationError> violationErrors ) {
   public ViolationReport() {
      this( new ArrayList<>() );
   }

   public @NotNull List<ViolationError> getViolationErrors() {
      return violationErrors;
   }
}
