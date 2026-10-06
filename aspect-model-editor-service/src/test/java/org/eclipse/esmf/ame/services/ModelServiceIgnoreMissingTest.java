/*
 * Copyright (c) 2026 Robert Bosch Manufacturing Solutions GmbH
 *
 * See the AUTHORS file(s) distributed with this work for
 * additional information regarding authorship.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package org.eclipse.esmf.ame.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.eclipse.esmf.ame.exceptions.AspectModelBatchLoadException;
import org.eclipse.esmf.ame.services.models.AspectModelResult;
import org.eclipse.esmf.ame.services.models.FileEntry;
import org.eclipse.esmf.ame.services.models.FileInformation;
import org.eclipse.esmf.aspectmodel.loader.AspectModelLoader;
import org.eclipse.esmf.aspectmodel.resolver.FileSystemStrategy;
import org.eclipse.esmf.aspectmodel.urn.AspectModelUrn;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Requires;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;

/**
 * Loading the files of referenced elements when the workspace contains unresolvable references.
 */
@MicronautTest
@Property( name = "test.config", value = "missing-references" )
class ModelServiceIgnoreMissingTest {
   private static final String PROVIDER = "urn:samm:org.eclipse.esmf.provider:1.0.0#";
   private static final String PROPERTY_WITH_MISSING_CHARACTERISTIC = PROVIDER + "propertyWithMissingCharacteristic";
   private static final String RESOLVABLE_PROPERTY = PROVIDER + "resolvableProperty";
   private static final String COMPLETE_PROPERTY = "urn:samm:org.eclipse.esmf.complete:1.0.0#completeProperty";
   private static final String BROKEN_PROPERTY = "urn:samm:org.eclipse.esmf.broken:1.0.0#brokenProperty";

   @Inject
   private ModelService modelService;

   @Factory
   @Requires( property = "test.config", value = "missing-references" )
   static class TestConfigOverride {
      @Bean
      @Singleton
      @Replaces( bean = AspectModelLoader.class )
      public AspectModelLoader aspectModelLoader() {
         return new AspectModelLoader( new FileSystemStrategy( modelPath() ) );
      }

      @Bean
      @Singleton
      @Replaces( bean = Path.class )
      public Path modelPath() {
         return Path.of( "src", "test", "resources", "services", "workspace-with-missing-references" ).toAbsolutePath();
      }
   }

   private static List<FileEntry> request( final String... urns ) {
      return Arrays.stream( urns ).map( urn -> new FileEntry( null, null, urn, null ) ).toList();
   }

   @Test
   void resolvableElementIsLoadedInBothModes() {
      for ( final boolean ignoreMissing : List.of( false, true ) ) {
         final List<FileInformation> result = modelService.getModels( request( COMPLETE_PROPERTY ), ignoreMissing );

         assertEquals( 1, result.size() );
         assertEquals( "org.eclipse.esmf.complete:1.0.0:Complete.ttl", result.get( 0 ).absoluteName() );
         assertEquals( COMPLETE_PROPERTY, result.get( 0 ).aspectModelUrn() );
      }
   }

   @Test
   void fileWithUnresolvableReferencesFailsByDefault() {
      final AspectModelBatchLoadException exception = assertThrows( AspectModelBatchLoadException.class,
            () -> modelService.getModels( request( RESOLVABLE_PROPERTY ) ) );

      assertEquals( 422, exception.getHttpStatusCode() );
   }

   @Test
   void fileWithUnresolvableReferencesIsReturnedUnresolvedWhenIgnoringMissingElements() {
      for ( final String urn : List.of( PROPERTY_WITH_MISSING_CHARACTERISTIC, RESOLVABLE_PROPERTY ) ) {
         final List<FileInformation> result = modelService.getModels( request( urn ), true );

         assertEquals( 1, result.size() );
         final FileInformation file = result.get( 0 );
         assertEquals( "org.eclipse.esmf.provider:1.0.0:Provider.ttl", file.absoluteName() );
         assertEquals( "Provider.ttl", file.fileName() );
         assertEquals( urn, file.aspectModelUrn() );
         assertEquals( "2.2.0", file.modelVersion() );
         assertTrue( file.aspectModel().contains( "samm:characteristic missing:MissingCharacteristic" ),
               "The original file content is returned" );
      }
   }

   @Test
   void missingElementsAreLeftOutWhenIgnoringMissingElements() {
      final List<FileInformation> result = modelService.getModels(
            request( PROVIDER + "unknownProperty", COMPLETE_PROPERTY, "urn:samm:org.eclipse.esmf.unknown:1.0.0#unknownProperty" ), true );

      assertEquals( List.of( COMPLETE_PROPERTY ), result.stream().map( FileInformation::aspectModelUrn ).toList() );
   }

   @Test
   void missingElementsFailTheRequestByDefault() {
      final AspectModelBatchLoadException exception = assertThrows( AspectModelBatchLoadException.class,
            () -> modelService.getModels( request( COMPLETE_PROPERTY, PROVIDER + "unknownProperty" ), false ) );

      assertTrue( exception.getMessage().contains( "unknownProperty" ) );
   }

   @Test
   void syntaxErrorsStillFailTheRequestWhenIgnoringMissingElements() {
      final AspectModelBatchLoadException exception = assertThrows( AspectModelBatchLoadException.class,
            () -> modelService.getModels( request( COMPLETE_PROPERTY, BROKEN_PROPERTY ), true ) );

      assertEquals( 422, exception.getHttpStatusCode() );
   }

   @Test
   void unresolvedModelReturnsTheRawDefiningFile() {
      final Optional<AspectModelResult> result = modelService.getUnresolvedModel( AspectModelUrn.fromUrn( RESOLVABLE_PROPERTY ) );

      assertTrue( result.isPresent() );
      assertEquals( "Provider.ttl", result.get().filename().orElseThrow() );
      assertTrue( result.get().content().contains( "MissingCharacteristic" ) );
   }

   @Test
   void unresolvedModelIsEmptyIfNoFileDefinesTheElement() {
      assertTrue( modelService.getUnresolvedModel( AspectModelUrn.fromUrn( PROVIDER + "unknownProperty" ) ).isEmpty() );
   }
}
