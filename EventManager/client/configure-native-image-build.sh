#!/bin/bash

# By default, the event-client.sh script in this folder executes the client fat JAR
# You can build the client fat JAR with ./gradlew client:shadowJar (from the top-level root)
#
# However, you can also compile the client to a native binary using ./gradlew client:nativeImage from the top-level root
# To enable this, we provide the necessary configuration files (src/main/resources/META-INF/native-image/*.json).
# The first time you run client:nativeImage it will download the necessary GraalVM components.
#
# However, if you modify the source to the client you may need to regenerate the configuration files.
# And that is what this script is for.
#
# This script runs the client fat Jar through a series of representative operations, with the
# GraalVM native-image instrumentation agent enabled. This results in the creation of an updated
# set of configuration files. (This means you must first build the client fat Jar and it assumes you have
# previously triggered the necessary GraalVM downloads by running client:nativeImage at some point)
#
# Once this script has been run successfully, you can (re)run ./gradlew client:nativeImage from the top-level root

CLIENT="$HOME/.gradle/caches/com.palantir.graal/20.2.0/8/graalvm-ce-java8-20.2.0/Contents/Home/bin/java -agentlib:native-image-agent=config-output-dir=src/main/resources/META-INF/native-image -jar build/libs/client-all.jar"
rm XALICE XBOB XALICE.conclave XBOB.conclave
rm src/main/resources/META-INF/native-image/*.json
mkdir -p src/main/resources/META-INF/native-image
$CLIENT XBOB share
$CLIENT XALICE share
$CLIENT XALICE configure-reflection
rm XALICE XBOB XALICE.conclave XBOB.conclave
echo now run ./gradlew client:nativeImage from top level root
