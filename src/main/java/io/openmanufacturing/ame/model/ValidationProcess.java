package io.openmanufacturing.ame.model;

import java.nio.file.Path;
import java.util.Arrays;

import io.openmanufacturing.ame.config.ApplicationSettings;

public enum ValidationProcess {
   VALIDATION( ApplicationSettings.getMetaModelStoragePath() ),
   EXPORT( ApplicationSettings.getExportPackageStoragePath() ),
   IMPORT( ApplicationSettings.getImportPackageStoragePath() ),
   GENERATE( ApplicationSettings.getMetaModelStoragePath() ),
   MIGRATE( ApplicationSettings.getMetaModelStoragePath() ),
   MODELS( ApplicationSettings.getMetaModelStoragePath() );

   private final Path path;

   ValidationProcess( final Path path ) {
      this.path = path;
   }

   public Path getPath() {
      return path;
   }

   public static ValidationProcess getEnum( final String value ) {
      return Arrays.stream( values() )
                   .filter( v -> v.getPath().toString().equalsIgnoreCase( value ) )
                   .findFirst()
                   .orElseThrow( IllegalArgumentException::new );
   }
}
