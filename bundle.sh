#!/usr/bin/env bash
set -e
cd "$(dirname $0)"
./gradlew runDceKotlin || true
./gradlew browserWebpack
