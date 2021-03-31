#!/bin/bash

CLIENT="java -jar build/libs/client-1.0-SNAPSHOT-all.jar"
#CLIENT="./build/graal/EventManagerClient"

$CLIENT "$@"
