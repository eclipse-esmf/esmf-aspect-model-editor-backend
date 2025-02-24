package org.eclipse.esmf.ame.model;

import io.micronaut.serde.annotation.SerdeImport;

@SerdeImport( Error.class )
public record Error( String message, String path, int code ) {
}