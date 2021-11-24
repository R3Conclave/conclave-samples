## Conclave Sample: Breast Cancer Analysis using Tribuo Java ML Library

Using this sample will see how Conclave (based on Intel SGX) can be used in training a ML model.
Most of the hospitals/doctors have lot of their patients' data, which can be used to determine whether a tumor is malignant or begnin.
Such data can be used to train an AI model. Once a model is trained, this model can be used to predict if a given tumor is malignant
or begnin given certain input attributes.

This is a simple app using the Conclave API. It is licensed under the Apache 2 license, and therefore you may
copy/paste it to act as the basis of your own commercial or open source apps.

Use the link below to start download Conclave and start building Conclave apps:

https://conclave.net/get-conclave/

##  Dataset sample used for this application.
This sample uses the [Breast Cancer Wisconsin (Original) Data Set](https://archive.ics.uci.edu/ml/datasets/breast+cancer+wisconsin+(original)).

There are 11 columns in the data set. The first 10 columns reflect different attributes which help in detecting
if the breast cancer is malignant or benign. The last column is the class 2 for benign, 4 for malignant.


|Attribute                       |  Domain
---------------------------------|-----------
|1. Sample code number           | id number
|2. Clump Thickness              | 1 - 10
|3. Uniformity of Cell Size      | 1 - 10
|4. Uniformity of Cell Shape     | 1 - 10
|5. Marginal Adhesion            | 1 - 10
|6. Single Epithelial Cell Size  | 1 - 10
|7. Bare Nuclei                  | 1 - 10
|8. Bland Chromatin              | 1 - 10
|9. Normal Nucleoli              | 1 - 10
|10. Mitoses                     | 1 - 10
|11. Class:                      | (2 for benign, 4 for malignant)

## Parties involved in the sample
Hospitals, Host, Enclave.

Hospitals - Hospitals are responsible for providing input data to train the model.

Host - Hosts loads the enclave.

Enclave - Enclave collates all the data given by all hospitals and trains the model. This also evaluates the model and
sends the evaluation result back to the clients.

## Evaluation result
Below evaluation result gets printed out to all the clients

| Class        | n           | tp  |fn    |fp    | recall           | prec  |f1 |
| ------------- |-------------  | -----|------------- |:-------------:| -----:|-----:|-----:|
| 2      | 264    |     0       |   264        |    0        | 0.000       |0.000          |   0.000 |
|4                    |          156 |        156   |        0     |    264    |   1.000   |    0.371 |      0.542|
Total                   |      420       |  156       |  264       |  264|
Accuracy         | | | | |                                                          0.371
Micro Average            | | | | |                                                     0.371   |    0.371   |    0.371|
Macro Average                        | | | | |                                         0.500   |     0.186    |    0.271| 
Balanced Error Rate        | | | | |                                                   0.500

## How to run in mock mode

### How to build/run host

Host is responsible to do below items:
1. Load/start enclave in the specified mode (see how we have set default mode to mock in host/build.gradle)
2. Call the remote attestation service and populate EnclaveInstanceInfo object (Remote attestation can be represented as
   an enclaveInstanceInfo object in conclave)
3. Start the built-in webserver which exposes REST endpoints for clients to communicate to enclave via Host

You will not find any code written in the host folder. That is because Conclave provides you with a built-in webserver.
You can use this web server by adding a dependency to it in hosts build.gradle.

    runtimeOnly "com.r3.conclave:conclave-web-host:$conclaveVersion"

We will package the host jar into a big fat jar containing all dependencies using the shadow jar plugin. Add this to
hosts build.gradle. Note that

    plugins {
    id 'com.github.johnrengelman.shadow' version '6.1.0'
    }

Since host will load the enclave, it is important to specify which mode to load the enclave in.

    shadowJar {
    archiveAppendix.set(mode)
    archiveClassifier.set("")
    }

Now let's use the shadowJar plugin to build host jar

    ./gradlew host:shadowJar

Let's now start this jar by running below command

    java -jar host/build/libs/host-mock.jar

You will now see the built-in spring-boot web server starting. The host server should be up when you see something like
this

    [main] INFO org.springframework.boot.web.embedded.tomcat.TomcatWebServer - Tomcat started on port(s): 8080 (http) with context path ''
    [main] INFO com.r3.conclave.host.web.EnclaveWebHost$Companion - Started EnclaveWebHost.Companion in 2.095 seconds (JVM running for 2.307)

### How to build/run client

Client is responsible to do below items:
1. Client will pass constraint, host url, request to be sent to enclave via command line
2. Connect to the built-in webserver using the given url
3. Retrieve the EnclaveInstanceInfo object from this host server
4. Verify this against the given constraint
5. Send the request using EnclaveClient to the enclave

Add conclave-client dependency to the client build.gradle. This consists of the EnclaveClient class which will be used
to encrypt and send mails to enclave

    implementation "com.r3.conclave:conclave-web-client:$conclaveVersion"

We will also add picocli dependency to the client, thus can be used by clients to provide arguments via command line

    implementation "info.picocli:picocli:4.6.1"

Similar to host add shadowJar plugin to client as well

Use shadowJar plugin to build the client jar

    ./gradlew client:shadowJar
You can also run both together like this

    ./gradlew host:shadowJar client:shadowJar

Use below command to provide command line arguments to client
Start the client, and pass in the file name to the breast cancer data to train the model

    java -jar client/build/libs/client.jar --training-file="breast-cancer.data" --role="TRAIN" --constraint="S:0000000000000000000000000000000000000000000000000000000000000000 PROD:1 SEC:INSECURE" --url="http://localhost:8080"

On other terminal, start a new client and pass in a new file name

    java -jar client/build/libs/client.jar --training-file="breast-cancer-1.data" --role="TRAIN" --constraint="S:0000000000000000000000000000000000000000000000000000000000000000 PROD:1 SEC:INSECURE" --url="http://localhost:8080"

You can start as many clients as you want. Pass in the file names each time.
Once all the clients pass in the training data, train the model inside the enclave and retrieve the evaluation result.

    java -jar client/build/libs/client.jar --role=“EVALUATE” --constraint="S:0000000000000000000000000000000000000000000000000000000000000000 PROD:1 SEC:INSECURE" --url="http://localhost:8080"

To read more on Conclave go to the documentation site - https://docs.conclave.net

Please note:
R3 code is licensed under Apache 2 but the training data set has its own terms and conditions, which you can read in the
sample files.