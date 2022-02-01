## Conclave Sample: Database Enclave

TEE like Intel SGX can be used for secure query processing.
Sensitive data stored in the database can be encrypted when at rest or in transit.
But this data needs to be decrypted when in use.
When data is being used by an application it is vulnerable to a variety of attacks.
Attacking applications while sensitive data is exposed has become a preferred strategy for security hackers looking to leverage vulnerabilities.
With the new 3rd Gen Intel Xeon Scalable Processors supporting 1 TB of enclave memory, setting up database inside and enclave is very well
possible.

Conclave 1.2 supports persistence. File I/O operations can now be mapped to a persistent memory which is represented by a file created on the host.

## Steps to enable persistence
Add the below property to build.gradle's conclave block.

    persistentFileSystemSize = "64m"

Create a file for persisting the database to this file on the host device. For this sample I have created a file name scratch.txt.

## Steps to run this sample

1. Open a new terminal for host. Build the host in simulation mode

        ./gradlew clean host:shadowJar -PenclaveMode=simulation
   
2. Build the client jar

        ./gradlew client:shadowJar

3. Setup linux docker environment for the client to run

        ./gradlew enclave:setupLinuxExecEnvironment
        docker run -it --rm -p 8080:8080 -v ${PWD}:/project -w /project conclave-build /bin/bash

4. Start the host in the docker container by specifying the persistent file name

        java -jar host/build/libs/host-simulation.jar --filesystem.file=host/scratch.txt

5. Open a new terminal for Client1. Pass in the command "CREATE".
   This will create a table named users with username and password as columns in this table in the database.

        java -jar client/build/libs/client.jar --command CREATE --constraint "S:5E7C34CEE313B69B2CEFF361C684A7EF39A63102A00C89A2FB7694524EC8F529 PROD:1 SEC:INSECURE" --url "http://localhost:8080"

5. Open a new terminal for Client1. Pass in the command "ADD" with username and password as specified at the very end of this line.
   This will create a table named users in the database and will create the username and password in the database.

        java -jar client/build/libs/client.jar --command ADD --constraint "S:5E7C34CEE313B69B2CEFF361C684A7EF39A63102A00C89A2FB7694524EC8F529 PROD:1 SEC:INSECURE" --url "http://localhost:8080"  Sneha password123

6. Open another terminal for Client2. Pass in the command "VERIFY" with will retrieve the values from the persisted database and give the earlier
   saved values.

        java -jar client/build/libs/client.jar --command VERIFY --constraint "S:5E7C34CEE313B69B2CEFF361C684A7EF39A63102A00C89A2FB7694524EC8F529 PROD:1 SEC:INSECURE" --url "http://localhost:8080" Sneha

7. Stop the host server by pressing Command+C. Start the host again. Hit the Verify command from the client terminal again. This should give you the saved user records.

This shows how a database can be used inside an enclave. How to create a table/insert records into it/select records from the table. This sample shows how persistence
is used to save this database inside an enclave on the host OS. This also shows how persisted records can be retrieved by the enclave once the host is restrated.

