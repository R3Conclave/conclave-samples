## Conclave Sample: Database Enclave

Steps to run this sample

./gradlew clean host:shadowJar -PenclaveMode=simulation
./gradlew client:shadowJar
./gradlew enclave:setupLinuxExecEnvironment
docker run -it --rm -p 8080:8080 -v ${PWD}:/project -w /project conclave-build /bin/bash
java -jar host/build/libs/host-simulation.jar --filesystem.file=host/scratch.txt

java -jar client/build/libs/client.jar --role ADD 
--constraint "S:4347BEBFAAE82EFF55235323F57C63EFA9CF71A4B9C3B5E4258DF91B5DED3C65 PROD:1 SEC:INSECURE" 
--url "http://localhost:8080"  Sneha password123

java -jar client/build/libs/client.jar --role VERIFY 
--constraint "S:4347BEBFAAE82EFF55235323F57C63EFA9CF71A4B9C3B5E4258DF91B5DED3C65 PROD:1 SEC:INSECURE" 
--url "http://localhost:8080" Sneha password123


