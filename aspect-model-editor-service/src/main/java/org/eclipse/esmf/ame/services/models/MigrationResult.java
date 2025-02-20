package org.eclipse.esmf.ame.services.models;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Represents a single migration result with success and erros.
 */
@Data
@AllArgsConstructor
@JsonInclude( JsonInclude.Include.NON_EMPTY )
public class MigrationResult {
   private boolean success;
   private List<String> errors;
}