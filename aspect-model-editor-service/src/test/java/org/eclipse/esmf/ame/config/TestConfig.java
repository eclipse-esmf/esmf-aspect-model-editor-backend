package org.eclipse.esmf.ame.config;

import java.nio.file.Path;

import org.eclipse.esmf.aspectmodel.loader.AspectModelLoader;
import org.eclipse.esmf.aspectmodel.resolver.FileSystemStrategy;
import org.eclipse.esmf.aspectmodel.shacl.constraint.JsConstraint;
import org.eclipse.esmf.aspectmodel.validation.services.AspectModelValidator;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import jakarta.inject.Singleton;

@Factory
public class TestConfig {

   @Bean
   @Singleton
   public AspectModelValidator getAspectModelValidator() {
      JsConstraint.setEvaluateJavaScript( false );
      return new AspectModelValidator();
   }

   @Bean
   @Singleton
   public AspectModelLoader aspectModelLoader() {
      return new AspectModelLoader( new FileSystemStrategy( modelPath() ) );
   }

   @Bean
   @Singleton
   public Path modelPath() {
      return Path.of( "src", "test", "resources", "services" ).toAbsolutePath();
   }
}