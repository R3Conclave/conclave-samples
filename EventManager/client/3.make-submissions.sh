#!/bin/bash

# Make a bunch of submissions
# Assumes one of the clients is built. See 1.reset-client.sh
# We assume the host process is running. If not, run ./gradlew host:run from the top-level root

CLIENT=./event-client.sh

$CLIENT ALICE submit-value AvgComp 10
$CLIENT ALICE submit-value AvgComp 20  # SHOULD SUPERSEDE PREVIOUS
$CLIENT BOB submit-value AvgComp 40
$CLIENT CHARLEY submit-value AvgComp 100
$CLIENT DENISE submit-value AvgComp 20

$CLIENT DENISE submit-value MaxComp 10 # SHOULD BE REJECTED
$CLIENT ALICE submit-value MaxComp 5
## Result should be non-quorate

$CLIENT ALICE submit-value MinComp 2
$CLIENT BOB submit-value MinComp 4
$CLIENT CHARLEY submit-value MinComp 8
$CLIENT DENISE submit-value MinComp 1

$CLIENT ALICE submit-value KeyMatcher KEYABC "Alice first message"
$CLIENT ALICE submit-value KeyMatcher KEYABC "Alice second message"  # should both be visible in final results
$CLIENT BOB submit-value KeyMatcher KEYABC "Bob message"
$CLIENT CHARLEY submit-value KeyMatcher KEYABC "Charley message"
$CLIENT DENISE submit-value KeyMatcher KEYXYZ "Denise message"
$CLIENT CHARLEY submit-value KeyMatcher KEYXYZ "Charley message"