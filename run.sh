#!/usr/bin/env bash
set -e
cd "$(dirname $0)"
./gradlew --continuous run
