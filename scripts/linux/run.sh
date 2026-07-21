#!/usr/bin/env bash
HERE=\${BASH_SOURCE%/*}
exec "\$HERE/jdk-25.0.3+9-jre/bin/java" ... "\$HERE/aspect-model-editor-runtime-DEV-SNAPSHOT.jar" "\$@"
