#!/bin/bash

# Get results for all calcs and check inboxes
# Assumes one of the clients is built. See 1.reset-client.sh
# We assume the host process is running. If not, run ./gradlew host:run from the top-level root

CLIENT=./event-client.sh

ACTORS="ALICE BOB CHARLEY DENISE"
for actor in $ACTORS; do
  echo "Results for $actor"
  $CLIENT $actor get-result AvgComp
  $CLIENT $actor get-result MaxComp
  $CLIENT $actor get-result MinComp
  $CLIENT $actor get-result KeyMatcher
  $CLIENT $actor get-result NO_SUCH_COMP
  $CLIENT $actor get-matches
done


