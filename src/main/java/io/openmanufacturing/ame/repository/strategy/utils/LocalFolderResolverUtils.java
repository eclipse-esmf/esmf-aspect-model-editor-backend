package io.openmanufacturing.ame.repository.strategy.utils;

import java.io.File;
import java.io.IOException;

import javax.annotation.Nonnull;

import org.apache.commons.io.FileUtils;

import lombok.AllArgsConstructor;
import lombok.Data;

public class LocalFolderResolverUtils {

   private LocalFolderResolverUtils() {
   }

   public static final String NAMESPACE_VERSION_NAME_SEPARATOR = ":";

   /**
    * This method will extract namespace, version and name from path based on ':'.
    */
   public static FolderStructure extractFilePath( @Nonnull final String path ) {
      final String[] splitNamespace = path.split( NAMESPACE_VERSION_NAME_SEPARATOR );

      return switch ( splitNamespace.length ) {
         case 1 -> new FolderStructure( path );
         case 2 -> extractNamespaceVersion( splitNamespace );
         case 3 -> extractNamespaceVersionName( splitNamespace );
         default -> new FolderStructure();
      };
   }

   /**
    * Split the given path in path, version and filename based on ':'.
    * ex: io.openmanufacturing:1.0.0
    *
    * @param path - path of the current ttl.
    */
   private static FolderStructure extractNamespaceVersion( final String[] path ) {
      return new FolderStructure( path[0], path[1] );
   }

   /**
    * Split the given path in path, version and filename based on ':'.
    * ex: io.openmanufacturing:1.0.0:AspectDefault.ttl
    *
    * @param path - path of the current ttl.
    */
   private static FolderStructure extractNamespaceVersionName( final String[] path ) {
      return new FolderStructure( path[0], path[1], path[2] );
   }

   @Data
   @AllArgsConstructor
   public static class FolderStructure {
      private String fileRootPath;
      private String version;
      private String fileName;

      FolderStructure() {
      }

      FolderStructure( final String fileRootPath ) {
         this.fileRootPath = fileRootPath;
      }

      public FolderStructure( final String fileRootPath, final String version ) {
         this.fileRootPath = fileRootPath;
         this.version = version;
      }

      @Override
      public String toString() {
         if ( fileName != null ) {
            return fileRootPath + File.separator + version + File.separator + fileName;
         }

         if ( version != null ) {
            return fileRootPath + File.separator + version;
         }

         return fileRootPath;
      }
   }

   public static void deleteDirectory( final String storagePath ) throws IOException {
      FileUtils.forceDelete( new File( storagePath ) );
   }
}
