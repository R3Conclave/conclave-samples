#!/bin/bash

CLIENT="java -jar build/libs/client-1.0-SNAPSHOT-all.jar -s=http://104.211.35.50:9999"
#CLIENT="./build/graal/EventManagerClient"

$CLIENT "$@"
