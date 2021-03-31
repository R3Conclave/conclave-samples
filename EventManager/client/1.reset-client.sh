#!/bin/bash

# This script creates a fresh set of four identities to support the sample
# operations in the other numbered scripts in this directory.
# Assumes the client fat jar has already been produced and/or native-client built
# Run ./gradlew client:shadowJar from the top-level root to create client fat JAR
# We assume the host process is running. If not, run ./gradlew host:run from the top-level root

rm ALICE BOB CHARLEY DENISE
rm *.conclave

CLIENT=./event-client.sh

$CLIENT ALICE share-identity
$CLIENT BOB share-identity
$CLIENT CHARLEY share-identity
$CLIENT DENISE share-identity
