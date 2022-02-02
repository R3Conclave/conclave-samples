## Conclave Sample: Database Enclave

TEE like Intel SGX can be used for secure query processing. Sensitive data stored in the database can be encrypted when
at rest or in transit. But this data needs to be decrypted when in use. When data is being used by an application it is
vulnerable to a variety of attacks. Attacking applications while sensitive data is exposed has become a preferred
strategy for security hackers looking to leverage vulnerabilities. With the new 3rd Gen Intel Xeon Scalable Processors
supporting 1 TB of enclave memory, setting up database inside and enclave is very well possible.

Conclave 1.2 supports persistence. File I/O operations can now be mapped to a persistent memory which is represented by
a file created on the host.

## Steps to enable persistence

Add the below property to build.gradle's conclave block.

    persistentFileSystemSize = "64m"

A file on the host filesystem will be used for persisting the enclave database. To let the enclave know about this file,
specify the path to this file to the command when starting host as shown in the below step 4. For this sample I have
created a file name scratch.txt in the host directory.

## Steps to run this sample on Mac

1. Open a new terminal for host. Build the host in simulation mode

        ./gradlew clean host:shadowJar -PenclaveMode=simulation

2. Build the client jar

        ./gradlew client:shadowJar

3. Setup Linux docker environment for the client to run

        ./gradlew enclave:setupLinuxExecEnvironment
        docker run -it --rm -p 8080:8080 -v ${PWD}:/project -w /project conclave-build /bin/bash

Please note this step is required to setup a linux environment for the enclave to load into. If you are on linux, you
can skip this step.

4. Start the host in the docker container by specifying the persistent file name

        java -jar host/build/libs/host-simulation.jar --filesystem.file=host/scratch.txt

5. Open a new terminal for Client1. Pass in the command "CREATE". This will create a table named users with username and
   password as columns in this table in the database.

        java -jar client/build/libs/client.jar --command CREATE --constraint "S:3907A61908C1F7FAE2DFDE48C5EE948BAE2A45E711F959E231577B0AA357A8C8 PROD:1 SEC:INSECURE" --url "http://localhost:8080"

Please note, to run the client, you will need the Enclave constraint. The enclave constraint will include acceptable
enclave code signing key hash, acceptable signers, etc. Please refer to the Class EnclaveConstraint in the docs for more
details. The Enclave code signing key hash can be found printed when you build the host jar above in step 4. It looks
something like this -

      Code signing key hash: 3907A61908C1F7FAE2DFDE48C5EE948BAE2A45E711F959E231577B0AA357A8C8, 

In this case the hash 3907A61908C1F7FAE2DFDE48C5EE948BAE2A45E711F959E231577B0AA357A8C8 is the enclave constraint.

5. Open a new terminal for Client1. Pass in the command "ADD" with username and password as specified at the very end of
   this line. This will create a table named users in the database and will create the username and password in the
   database.

        java -jar client/build/libs/client.jar --command ADD --constraint "S:3907A61908C1F7FAE2DFDE48C5EE948BAE2A45E711F959E231577B0AA357A8C8 PROD:1 SEC:INSECURE" --url "http://localhost:8080"  Sneha password123

6. Open another terminal for Client2. Pass in the command "VERIFY" with will retrieve the values from the persisted
   database and give the earlier saved values.

        java -jar client/build/libs/client.jar --command VERIFY --constraint "S:3907A61908C1F7FAE2DFDE48C5EE948BAE2A45E711F959E231577B0AA357A8C8 PROD:1 SEC:INSECURE" --url "http://localhost:8080" Sneha

7. Stop the host server by pressing Command+C. Start the host again. Hit the Verify command from the client terminal
   again. This should give you the saved user records.

This sample shows how persistence is used to create a database inside an enclave and save data into it, how to create a
table/insert records into it/select records from the table. This also shows how persisted records can be retrieved by
the enclave once the host is re-started.

