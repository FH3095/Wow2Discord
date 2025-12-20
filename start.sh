#!/usr/bin/env bash

set -u

OWN_DIR=$(dirname "$0")
pushd "$OWN_DIR" || return
java -jar "Wow2Discord-1.0-all.jar"
popd || return
