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

package org.eclipse.esmf.ame.services.utils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.jena.rdf.model.Resource;
import org.eclipse.esmf.aspectmodel.ValueParsingException;
import org.jspecify.annotations.Nullable;

/**
 * Utility for resolving Turtle subjects and element URNs from source text locations (line and column)
 * and formatting descriptive value parsing error messages.
 */
public final class TurtleElementResolver {

   private static final Pattern PREFIX_PATTERN = Pattern.compile(
         "(?i)(?:@prefix\\s+|PREFIX\\s+)([^\\s:]*:)\\s*<([^>]+)>"
   );

   private static final Pattern PREDICATE_PATTERN = Pattern.compile(
         "^\\s*([a-zA-Z_0-9\\-]+:[a-zA-Z_0-9\\-]+|<[^>]+>)"
   );

   private TurtleElementResolver() {}

   /**
    * Resolves the enclosing Aspect Model element URN and property predicate from Turtle source content
    * given a 1-based line and column number.
    *
    * @param documentContent the Turtle document source text
    * @param line the 1-based line number of the error
    * @param column the 1-based column number of the error
    * @return resolved context containing optional URN, predicate, line, and column
    */
   public static TurtleElementContext resolveContext(
         @Nullable final String documentContent, final long line, final long column ) {
      if ( documentContent == null || documentContent.isBlank() || line <= 0 ) {
         return new TurtleElementContext( Optional.empty(), Optional.empty(), line, column );
      }

      final Map<String, String> prefixMap = extractPrefixMap( documentContent );
      final String[] lines = documentContent.split( "\\r?\\n" );

      final int targetIndex = (int) line - 1;
      if ( targetIndex >= lines.length ) {
         return new TurtleElementContext( Optional.empty(), Optional.empty(), line, column );
      }

      final Optional<String> predicate = extractPredicate( lines[targetIndex] );
      final Optional<String> subjectUrn = findSubjectUrn( lines, targetIndex, prefixMap );

      return new TurtleElementContext( subjectUrn, predicate, line, column );
   }

   /**
    * Formats a user-friendly error message from a {@link ValueParsingException} with resolved context.
    *
    * @param vpe the value parsing exception
    * @param context the resolved Turtle element context
    * @return formatted error message
    */
   public static String formatValueParsingMessage(
         final ValueParsingException vpe, @Nullable final TurtleElementContext context ) {
      final StringBuilder sb = new StringBuilder();

      if ( context != null && context.elementUrn().isPresent() ) {
         sb.append( "Element '" ).append( context.elementUrn().get() ).append( "'" );
         if ( context.predicate().isPresent() ) {
            sb.append( " (" ).append( context.predicate().get() ).append( ")" );
         }
         sb.append( ": " );
      }

      final String typeName = formatDataType( vpe.getType() );
      final String valueStr = vpe.getValue() != null ? String.valueOf( vpe.getValue() ) : "";

      if ( vpe.getLine() > 0 ) {
         sb.append( String.format( "Invalid value \"%s\" for type %s at line %d, column %d",
               valueStr, typeName, vpe.getLine(), vpe.getColumn() ) );
      } else {
         sb.append( String.format( "Invalid value \"%s\" for type %s", valueStr, typeName ) );
      }

      final String causeMessage = extractRootCauseMessage( vpe );
      if ( causeMessage != null && !causeMessage.isBlank() ) {
         sb.append( " (" ).append( causeMessage ).append( ")" );
      }

      return sb.toString();
   }

   /**
    * Resolves context from a {@link ValueParsingException}, reading the source location if the source document is not set.
    *
    * @param vpe the value parsing exception
    * @return resolved context
    */
   public static TurtleElementContext resolveContext( final ValueParsingException vpe ) {
      String documentContent = vpe.getSourceDocument();
      if ( ( documentContent == null || documentContent.isBlank() ) && vpe.getSourceLocation() != null ) {
         try {
            final Path path = Paths.get( vpe.getSourceLocation() );
            if ( Files.isRegularFile( path ) ) {
               documentContent = Files.readString( path );
            }
         } catch ( final Exception ignored ) {
            // Ignore fallback
         }
      }
      return resolveContext( documentContent, vpe.getLine(), vpe.getColumn() );
   }

   /**
    * Resolves the Aspect Model element URN associated with a {@link ValueParsingException}.
    *
    * @param vpe the value parsing exception
    * @return the optional element URN
    */
   public static Optional<String> resolveElementUrn( final ValueParsingException vpe ) {
      return resolveContext( vpe ).elementUrn();
   }

   /**
    * Formats a {@link ValueParsingException} using its own source document if available.
    *
    * @param vpe the value parsing exception
    * @return formatted error message
    */
   public static String formatValueParsingException( final ValueParsingException vpe ) {
      final TurtleElementContext context = resolveContext( vpe );
      return formatValueParsingMessage( vpe, context );
   }

   /**
    * Formats a Jena {@link Resource} data type into a standard prefixed notation (e.g. "xsd:int").
    *
    * @param type the Jena resource datatype
    * @return formatted data type string
    */
   public static String formatDataType( @Nullable final Resource type ) {
      if ( type == null ) {
         return "literal";
      }
      final String uri = type.getURI();
      if ( uri != null ) {
         if ( uri.startsWith( "http://www.w3.org/2001/XMLSchema#" ) ) {
            return "xsd:" + uri.substring( "http://www.w3.org/2001/XMLSchema#".length() );
         }
         if ( uri.startsWith( "urn:samm:org.eclipse.esmf.samm:" ) ) {
            final int hashIndex = uri.lastIndexOf( '#' );
            if ( hashIndex >= 0 ) {
               return "samm:" + uri.substring( hashIndex + 1 );
            }
         }
      }
      return type.getLocalName() != null ? type.getLocalName() : ( uri != null ? uri : "literal" );
   }

   private static Map<String, String> extractPrefixMap( final String content ) {
      final Map<String, String> prefixMap = new HashMap<>();
      final Matcher matcher = PREFIX_PATTERN.matcher( content );
      while ( matcher.find() ) {
         final String prefix = matcher.group( 1 ).trim();
         final String uri = matcher.group( 2 ).trim();
         prefixMap.put( prefix, uri );
      }
      return prefixMap;
   }

   private static Optional<String> extractPredicate( final String line ) {
      final String cleanLine = stripCommentsAndStrings( line ).trim();
      final Matcher matcher = PREDICATE_PATTERN.matcher( cleanLine );
      if ( matcher.find() ) {
         final String candidate = matcher.group( 1 );
         if ( !"a".equals( candidate ) ) {
            return Optional.of( candidate );
         }
      }
      return Optional.empty();
   }

   private static Optional<String> findSubjectUrn(
         final String[] lines, final int targetIndex, final Map<String, String> prefixMap ) {
      // Find the start line of the statement containing targetIndex
      int statementStartIndex = 0;
      for ( int i = targetIndex - 1; i >= 0; i-- ) {
         final String cleanLine = stripCommentsAndStrings( lines[i] ).trim();
         if ( cleanLine.endsWith( "." ) ) {
            statementStartIndex = i + 1;
            break;
         }
      }

      // Starting from statementStartIndex up to targetIndex, find first line with a subject
      for ( int i = statementStartIndex; i <= targetIndex; i++ ) {
         final String cleanLine = stripCommentsAndStrings( lines[i] ).trim();
         if ( cleanLine.isEmpty() || cleanLine.startsWith( "#" )
               || cleanLine.startsWith( "@prefix" ) || cleanLine.toUpperCase().startsWith( "PREFIX" ) ) {
            continue;
         }

         final String[] tokens = cleanLine.split( "\\s+" );
         if ( tokens.length > 0 ) {
            String rawSubject = tokens[0].replaceAll( "[;,.]+$", "" );
            if ( !rawSubject.isEmpty() ) {
               final String expanded = expandToken( rawSubject, prefixMap );
               if ( expanded != null && !expanded.isBlank() ) {
                  return Optional.of( expanded );
               }
            }
         }
      }

      return Optional.empty();
   }

   private static String expandToken( final String token, final Map<String, String> prefixMap ) {
      if ( token.startsWith( "<" ) && token.endsWith( ">" ) ) {
         return token.substring( 1, token.length() - 1 );
      }

      if ( token.startsWith( ":" ) ) {
         final String prefixUri = prefixMap.get( ":" );
         if ( prefixUri != null ) {
            return prefixUri + token.substring( 1 );
         }
         return token;
      }

      final int colonIndex = token.indexOf( ':' );
      if ( colonIndex > 0 ) {
         final String prefix = token.substring( 0, colonIndex + 1 );
         final String localName = token.substring( colonIndex + 1 );
         final String prefixUri = prefixMap.get( prefix );
         if ( prefixUri != null ) {
            return prefixUri + localName;
         }
      }

      return token;
   }

   private static String stripCommentsAndStrings( final String line ) {
      if ( line == null || line.isBlank() ) {
         return "";
      }
      final StringBuilder sb = new StringBuilder();
      boolean inString = false;
      char stringChar = 0;
      boolean inIri = false;
      boolean escaped = false;

      for ( int i = 0; i < line.length(); i++ ) {
         final char c = line.charAt( i );
         if ( escaped ) {
            escaped = false;
            continue;
         }
         if ( c == '\\' ) {
            escaped = true;
            continue;
         }
         if ( inString ) {
            if ( c == stringChar ) {
               inString = false;
            }
            continue;
         }
         if ( inIri ) {
            sb.append( c );
            if ( c == '>' ) {
               inIri = false;
            }
            continue;
         }
         if ( c == '"' || c == '\'' ) {
            inString = true;
            stringChar = c;
            continue;
         }
         if ( c == '<' ) {
            inIri = true;
            sb.append( c );
            continue;
         }
         if ( c == '#' ) {
            break;
         }
         sb.append( c );
      }
      return sb.toString();
   }

   private static String extractRootCauseMessage( final Throwable throwable ) {
      Throwable current = throwable;
      String rootMsg = null;
      while ( current != null ) {
         if ( current.getCause() == null || current.getCause() == current ) {
            rootMsg = current.getMessage();
            break;
         }
         current = current.getCause();
      }
      if ( rootMsg == null || rootMsg.isBlank() ) {
         rootMsg = throwable.getMessage();
      }
      return rootMsg;
   }
}
