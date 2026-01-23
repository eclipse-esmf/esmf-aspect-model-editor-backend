#!/bin/bash -x

#
# Copyright (c) 2025 Robert Bosch Manufacturing Solutions GmbH
#
# See the AUTHORS file(s) distributed with this work for
# additional information regarding authorship.
#
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at https://mozilla.org/MPL/2.0/.
#
# SPDX-License-Identifier: MPL-2.0
#

INPUT=$1
ENTITLEMENTS=$2
NEEDS_UNZIP=false

# if folder, zip it
if [ -d "${INPUT}" ]; then
    NEEDS_UNZIP=true
    zip -r -q -y unsigned.zip "${INPUT}"
    rm -rf "${INPUT}"
    INPUT=unsigned.zip
fi

# sign with curl
curl -o "../signed_dir/${INPUT}" -F file=@"${INPUT}" -F entitlements=@"${ENTITLEMENTS}" https://cbi.eclipse.org/macos/codesign/sign

# if unzip needed
if [ "$NEEDS_UNZIP" = true ]; then
    unzip -qq "${INPUT}"

    if [ $? -ne 0 ]; then
        # echo contents if unzip failed
        output=$(cat "${INPUT}")
        echo "$output"
        exit 1
    fi

    rm -f "${INPUT}"
fi
