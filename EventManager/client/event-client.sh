#!/bin/bash

CLIENT="java -jar build/libs/client-all.jar"
#CLIENT="./build/graal/EventManagerClient"

$CLIENT "$@"
