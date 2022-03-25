<p align="center">
  <img src="https://conclave.net/wp-content/uploads/2020/12/Conclave_logo_master.png" alt="Conclave" width="500">
</p>
<br>

# Conclave Samples

This repository contains multiple Conclave sample applications which is intended to help
developers to get started with Conclave and understand different features of
the platform. To learn more about the Conclave platform please visit our official
[documentation](https://docs.conclave.net/).

### [EventManager](./EventManager):
Event Manager implements the idea of an enclave that can host a collection of simultaneous 'computations'. These computations are initiated by a Conclave client, have a name and a type, have a defined set of 'participants', and a 'quorum' that must be reached before results can be obtained. Supported types are average (the average of the submissions is returned), maximum and minimum (the identity of the submitter of the highest or lowest value is returned), and 'key matcher' (described below).
The idea is that an enclave of this type could be used to host simple one-off multi-party computations (e.g., five employees wanting to know the average of their salaries, or ten firms wanting to know who had the lowest sales last month without revealing what that figure was).

### [Column Profiling](./column-profiling):
This sample does Column Profiling on the dataset and
returns the frequency distribution as an output. Column profiling is one of the methods used in data profiling.
This [article](https://www.alooma.com/blog/what-is-data-profiling) explains what data profiling and column profiling means
and explains its uses and applications.

### [Conclave Auction](./conclave-auction):
This sample allows bidders to confidentially submit their bids to an enclave (A protected region of memory which cannot be accessed by the OS, Kernel or BIOS). The bids are processed confidentially in the enclave and the result is returned to concerned parties.

### [Conclave Corda Trade](./conclave-corda-trade):
This application serves as a demo for building a confidential trading system based on Corda and Conclave. An exchange Corda node would serve as a host which runs the enclave, while broker nodes serve as clients which send encrypted orders from their end-clients which are matched in the enclave and trades generated are recorded in all relevant participants ledgers.

### [Psi](./psi-sample):
PSI problem refers to the problem of determining the common elements from the intersection of two sets without leaking or disclosing any additional information of the remaining elements of either sets.
Using this sample will demonstrate how Conclave (based on Intel SGX) can be a new tool to solve the private set intersection (PSI) problem.

### [Tribuo Classification](./tribuo-classification):
Using this sample will show how Conclave (based on Intel SGX) can be used in training a ML model. We will use Tribuo Java ML library to load the AI model.
For example, hospitals have patients' data which can be used to determine whether a tumour is malignant or benign.
Such data can be used to train an AI model. Once a model is trained, this model can be used to predict if a given tumor is malignant
or begnin given certain input attributes.

### [Tribuo Tutorials](./tribuo-tutorials):
Tribuo is a Java machine learning library, which makes it well suited to run with Conclave. This sample provides tools for classification, regression, clustering, model development, and more.
This sample shows you how you can use the [Tribuo Java ML](https://tribuo.org/learn/4.0/tutorials/) library to load and train models like classification models, regression models, clustering models etc.

### [Database Enclave](./psi-sample):
With the new 3rd Gen Intel Xeon Scalable Processors supporting 1 TB of enclave memory, 
setting up a database inside an enclave is very well possible. This sample shows how persistence is used to create a database inside an enclave and save data into it as well as how to create a
table, insert records into it, and select records from the table. This also shows how persisted records can be retrieved by
the enclave once the host is re-started.

# Write your first Conclave application in 5 mins

This tutorial can be used to get started with Conclave in 5 mins. We will create a sample in which the client can send
a string to the enclave, the enclave will reverse it and send it back to the client and the machine on which the enclave 
is running will not be able to discover what the string was, even though the reversal logic is executing on that machine!

#Step 1: Prerequisites
You need [JDK](https://www.oracle.com/java/technologies/downloads/) 8 or 11 installed in order to build and run the app.<br />
Grab a copy of Conclave-SDK from [conclave.net](https://www.conclave.net/get-conclave/)
We will use the `conclave init` tool for bootstrapping Conclave projects, reducing the amount of boilerplate you need to write.
</br>You will find conclave-init.jar in the tools directory of the Conclave SDK.

# Step 2: Create the project template using the conclave-init tool

To create a new Conclave project, run the following command:

    java -jar /path/to/conclave-sdk/tools/conclave-init.jar 
    --package com.megacorp 
    --enclave-class-name ReverseEnclave 
    --target ./reverse-conclave-app

A new project would have been created in the reverse-conclave-app directory, as specified by --target ./reverse-conclave-app.
</br>This generates three modules

1. The enclave module contains the ReverseEnclave class. Your enclave code will go here.
2. The host directory has been created, too.
   It contains only a build.gradle file, since that's all that's required for the [Conclave web host](https://docs.conclave.net/conclave-web-host.html).
3. The client directory contains a basic client which can interact with the web host.

To understand more about the different files generated by this tool jump to the below section named Configurations generated by the conclave-init tool.

# Step 3: Add the Enclave code

Copy paste the code below to `receiveMail` method in `ReverseEnclave` class. 
This logic reverses the string received by the client and sends it back to the client.

      String inputString = new String(mail.getBodyAsBytes());
      StringBuilder outputString = new StringBuilder();
      outputString.append(inputString);
      outputString.reverse();
      byte[] response = postOffice(mail).encryptMail(outputString.toString().getBytes());
      postMail(response, routingHint);

# Step 4: Run this sample

There are different [modes](https://docs.conclave.net/enclave-modes.html) in which you can run your Conclave application.
For this tutorial we will run the conclave application in mock mode. To learn more about the different modes, please
read the below section named Enclave Modes.

Open two terminals for client and host pointing to the root of our `reverse-conclave-app`.

1. Build the host using below command on terminal one.
   
         ./gradlew clean host:shadowJar -PenclaveMode=mock
   
2. Run the host on the same terminal
   
         java -jar host/build/libs/host-mock.jar

3. Build the client using below command on a new terminal two. 
      
         ./gradlew client:shadowJar
   
4. Run the client on the new terminal two
         
         java -jar client/build/libs/client-all.jar "S:0000000000000000000000000000000000000000000000000000000000000000 PROD:1 SEC:INSECURE" ReverseMe

Using above 4 steps you should be able to have a conclave application running on your system.

# Appendix

# 1. Configurations generated by the conclave-init tool

The majority of the files generated by Conclave Init contain boilerplate code which configures the project's Gradle build.
Below is the generated file structure by the conclave init tool.

         .
         ├── build.gradle
         ├── client
         │   ├── build.gradle
         │   └── src/main/java/com/example/tutorial/client/ReverseEnclaveClient.java
         ├── enclave
         │   ├── build.gradle
         │   └── src/main/java/com/example/tutorial/enclave/ReverseEnclave.java
         │   └── src/test/java/com/example/tutorial/enclave/ReverseEnclaveTest.java
         ├── gradle
         │   └── wrapper
         │       ├── gradle-wrapper.jar
         │       └── gradle-wrapper.properties
         ├── gradle.properties
         ├── gradlew
         ├── gradlew.bat
         ├── host
         │   └── build.gradle
         ├── README.md
         └── settings.gradle      

Let's discuss the contents of some of these configuration files that Conclave init tool has generated.</br>

## Root directory
The Conclave samples use Gradle properties to import the SDK libraries into Conclave projects.
The properties in question are conclaveRepo and conclaveVersion.
The recommended place to set the conclaveRepo and conclaveVersion properties is in the user-wide gradle.properties file.
By default, this file is located at $HOME/.gradle/gradle.properties
This approach means that you only have to set the properties once, rather than for every project.

</br>To override conclave sdk repo path you can pass in the path value via command line arguments to the host as shown below.
./gradlew host:run -PconclaveRepo=<path-to-conclave-sdk>

####build.gradle
This is the top-level build.gradle file which provides common config that applies to the entire project.

It imports the Conclave SDK repository into our project using the conclaveRepo Gradle property.
See Gradle Properties for more information on this.

          ...
    repositories {
        maven {
            url = rootProject.file(conclaveRepo)
        }
        mavenCentral()
    }
    ...

####gradle.properties
Defines some versions of dependencies.
Note, we assume that the required conclaveRepo and conclaveVersion properties are set in the user-wide gradle.properties file.

## Host Module

You can get away with the boilerplate host code by referencing conclave-web-host as a runtime dependency in host's build.gradle.

      dependencies {
         runtimeOnly project(path: ":enclave", configuration: mode)
         runtimeOnly "com.r3.conclave:conclave-web-host:$conclaveVersion"
      }

## Enclave Module

We can configure the enclave's runtime environment using the conclave section in the enclave's build.gradle.
</br>The productID is used to distinguish enclaves produced by the same organisation, by default its value is 1.
</br>The revocationLevel should be incremented when a weakness or vulnerability in the enclave code is discovered and fixed,
this has a default value of 0.

For enclave to be loaded it must be signed

      conclave {
         productID = 1
         revocationLevel = 0
         simulation {
            signingType = privateKey
            signingKey = file("../signing/sample_private_key.pem")
         }
      }   

For an enclave to be loaded by the Intel SGX CPU, the enclave must be signed. You can specify which key needs to be used
for the different enclave modes. For example in the above config I am using sample_private_key to sign my enclave built
in simulation mode.
</br>To read more about the different modes in which you can build your enclave read the below section named
Enclave Modes.
</br>To read more about enclave signing read [here](https://docs.conclave.net/signing.html).

## Client Module

Similar to the host, you can get away with the boilerplate client code by referencing conclave-web-client as a runtime dependency in client's build.gradle.

      dependencies {
         implementation "com.r3.conclave:conclave-web-client:$conclaveVersion"
         runtimeOnly "org.slf4j:slf4j-simple:$slf4jVersion"
      }

## 2. Enclave Modes

You can build and run in either of the following modes:
1. Mock Mode: I would recommend you to build your enclave in this mode if you are using Conclave for the first time.
   Real Intel SGX hardware is not required. This is good for rapid dev/test when you want to quikly change,deploy and test
   your changes in secs.
   Usually an enclave is loaded in a sub JVM of the JVM in which the host is loaded. But with Mock mode, the enclave is
   loaded in the same JVM as that of the host. This leads to short build times and you can debug through your enclave code
   just like any othet Java function.

2. Simulation Mode: This also does not require a real Intel SGX CPU. Build and run your enclave in this mode, when you want
   to be as close as the Release mode, but do not have an Intel SGX CPU.

3. Debug Mode: This requires a real Intel SGX CPU. Enclave can be debbuged in this mode.
   To read more about building and running enclaves in debug mode go [here](https://docs.conclave.net/enclave-modes.html#debug-and-release-mode).

4. Release Mode: This requires a real Intel SGX CPU. This is the enclave running in actual production environment.
   To read more about building and running enclaves in release mode go [here](https://docs.conclave.net/enclave-modes.html#debug-and-release-mode).

## 3. How to build/run an enclave in simulation mode

1. Open a new terminal for host. Build the host in simulation mode

        ./gradlew clean host:shadowJar -PenclaveMode=simulation

2. Build the client jar

        ./gradlew client:shadowJar

3. Setup Linux docker environment for the client to run

        ./gradlew enclave:setupLinuxExecEnvironment
        docker run -it --rm -p 8080:8080 -v ${PWD}:/project -w /project conclave-build /bin/bash

Please note this step is required to setup a linux environment for the enclave to load into. If you are on linux, you
can skip this step.

4. Start the host in the docker container.

         java -jar host/build/libs/host-simulation.jar

5. Open a new terminal for Client1. Pass in the constraints and the required parameters to be sent to the enclave.

        java -jar client/build/libs/client.jar --constraint "S:3907A61908C1F7FAE2DFDE48C5EE948BAE2A45E711F959E231577B0AA357A8C8 PROD:1 SEC:INSECURE" ReverseMe

To read more about constraints read below section named Enclave Constraints.

## 4. Remote Attestation

Before the client starts sending the enclave his secret and sensitive data request, the client wants to make sure that
it is infact connected to an enclave running on a real intel sgx cpu.
</br>This is achieved using remote attestation process, where a host sends a report attested by Intel which confirms if the
code is running on a real Intel SGX enabled CPU patched with the latest security updates.
</br>This is handled by Conclave SDK. Conclave SDK also adds values like the MRENCLAVE or MRSIGNER which are essentially
hash values of who signed the enclave and whats the hash of the enclave code.

## 5. Enclave Constraints

Clients can verify the different values present in the remote attestation object on the client side before connecting to the enclave.
When you build your enclave in a simulation mode, the build process outputs a below values which can be verified by the cleints.

      Enclave code hash:   C504BD2F826870B93873E9A4F9863E586CAAFCCF3E0F084CAEBD0219DEAE2B44
      Enclave code signer: 9D491EBFAB6FEEF6E32C1D06DDB6DE723CA07BB207459B0646DAA467819940C1
      Enclave mode:        SIMULATION (INSECURE)

Clients can pass in the expected constraints via the command line when running the web client.

      --constraint="S:9D491EBFAB6FEEF6E32C1D06DDB6DE723CA07BB207459B0646DAA467819940C1 PROD:1 SEC:INSECURE"

When you build your enclave in a mock mode, the signing key hash consisting of all zeros.

Clients can pass in the expected constraints via the command line when running the web client.

      --constraint="S:0000000000000000000000000000000000000000000000000000000000000000 PROD:1 SEC:INSECURE"

The Enclave code hash value is the MRENCLAVE and the Enclave code signer is the MRSIGNER.

MRENCLAVE: This is the enclave’s identity, a SHA-256 hash of the log that records all activity while the enclave is being built.
</br>This log consists of code, data, heap, stack, and other attributes of an enclave.
</br>Once the build is complete, the final value of MRENCLAVE represents the identity of the enclave.
</br>In short, it’s the hash of the enclave code and initial data.

MRSIGNER: Each enclave signed by its author. MRSIGNER contains the hash of the public key of the author.

You can read more about constraints [here](https://docs.conclave.net/constraints.html).

## 6. Client-Host-Enclave Communication - Conclave Mail
Conclave adds the public key of the enclave to the remote attestation object when it sends this object to the client.
</br>Clients can be rest assured and send their data to enclaves using Conclave Mail, which encrypts the client requests/data using enclave's public key,
so that only enclave will be able to decrypt this request.
</br>Enclave does the same thing, uses the client public key to encrypt and wrap the response in a Mail object so that only
the concerned client can read the response.

## 8. Quick Recap - What we learnt

We used the conclave-init tool to quickly get started with Conclave. Typically, a client will request a remote attestation object from the enclave loaded
on a host.
</br>Host will request the remote attestation object from the enclave and forward it to the client. The client after verifying the values
from this object will connect to the enclave and send encrypted requests to the enclave using the Mail API.
</br>The enclave will execute the processing logic
and send the encrypted reply back to the client. Enclave's can be build and run either in simulation or mock mode for day to day development.

Now you have developed and tested your enclave, you can take a final step, to achieve the privacy promise of Conclave.
To do this, you can run the enclave in 'RELEASE' mode. See [here](https://docs.conclave.net/enclave-modes.html#debug-and-release-mode) for more information.

## License
The source code files are available under the Apache License, Version 2.0.
The licence file can be found [here](https://github.com/R3Conclave/conclave-samples/blob/master/LICENSE).