#!/bin/bash
export PATH="$PWD:$PATH"
./gradlew clean lint test
