#!/bin/bash

# Set up some calculations to play with
# Assumes one of the clients is built. See 1.reset-client.sh
# We assume the host process is running. If not, run ./gradlew host:run from the top-level root

CLIENT=./event-client.sh

$CLIENT ALICE create-computation AvgComp avg 3 ALICE,BOB,CHARLEY,DENISE
$CLIENT ALICE create-computation MaxComp max 2 ALICE,BOB,CHARLEY
$CLIENT ALICE create-computation MinComp min 3 BOB,CHARLEY,DENISE
$CLIENT ALICE create-computation KeyMatcher key 2 ALICE,BOB,CHARLEY,DENISE
