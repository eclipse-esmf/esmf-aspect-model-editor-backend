#!/usr/bin/env bash
HERE=${BASH_SOURCE%/*}
exec "$HERE/jdk-25.0.3+9-jre/bin/java" --enable-native-access=ALL-UNNAMED --sun-misc-unsafe-memory-access=allow -Dpolyglotimpl.DisableMultiReleaseCheck=true -jar "$HERE/aspect-model-editor-runtime-DEV-SNAPSHOT.jar" "$@"
