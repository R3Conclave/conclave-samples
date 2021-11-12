## Conclave Sample: Private Set Intersection

<p align="center">
  <img src="./psi.png" alt="Corda" width="600">
</p>

Using this sample will see how Conclave (based on Intel SGX) can be a new tool to solve the private set intersection (PSI) problem.

This is a simple app using the Conclave API. It is licensed under the Apache 2 license, and therefore you may 
copy/paste it to act as the basis of your own commercial or open source apps.

Use below link to start download Conclave and start building Conclave apps:

https://conclave.net/get-conclave/
        
# What is Private Set Intersection (PSI) problem?

PSI problem refers to the problem of determining the common elements from the intersection of two sets without leaking or disclosing any 
additional information of the remaining elements of the either sets.

# Use case : measuring ad conversion rates

To increase sales, one of the key marketing strategies used these days by merchants, is to publish an ad on the service 
provider (Facebook, Google) platforms.
Depending on the ad conversion rate (users who purchased the items after clicking on the ad), the merchant makes payments
to the service providers.
Measuring ad conversion rates is done usually by comparing the list of people who have seen an ad with those who have 
completed a transaction(purchased a product). 
These lists are held by the advertiser (say, Google or Facebook), and by merchants, respectively. 
It is often possible to identify users on both ends, using identifiers such as credit card numbers, email addresses, etc. 
A simple solution, which ignores privacy completely, is for one side to disclose its list of customers to the other side, 
which then computes the necessary statistics. 

For this use case, the merchant wants to calculate the ad conversion rate. Merchant usually tends to share the list of 
converted user details with the Service Provider.
Service Provider has list of users who has clicked on the ad. Service Provider performs an intersection of these lists 
on its side to check which converted users had clicked the ad.

## Parties involved in the sample
Merchant, Service Provider, Host.

Merchant - a Merchant who has a list of users who have purchased their product.  

Service Provider - Service Provider like Facebook, Google who publishes ads on its website.

Host - Hosts the enclave to calculate ad conversion rate.

For simplicity, in this sample clients pass in following data to enclave:

Merchant - Merchant passes credit card numbers of all users who have made a purchase.

Service Provider - Service Provider passes credit card numbers of all users who have clicked the ad.

This sample is built against conclave release 1.2.<provide link to relase notes>

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
Start the merchant client

     java -jar client/build/libs/client.jar  --constraint "S:0000000000000000000000000000000000000000000000000000000000000000 PROD:1 SEC:INSECURE" --url "http://localhost:8080" MERCHANT 88

Start the service provider client

    java -jar client/build/libs/client.jar  --constraint "S:0000000000000000000000000000000000000000000000000000000000000000 PROD:1 SEC:INSECURE" --url "http://localhost:8080" SERVICE-PROVIDER 88

Once you run both the clients, the enclave calculates the ad conversion rate and sends it to both the clients

    Ad Conversion Rate is : 100.0


#### A note about enclave constraint
In this sample the `code signer` is used as enclave constraint, but you can also use the `code hash`. 
If you want to use it, remember to change the code of the client to:

`EnclaveConstraint.parse("C:"+ constraint +" SEC:INSECURE" ).check(attestation);`

Read more in the [documentation](https://docs.conclave.net/enclave-configuration.html#productid).

To read more on Conclave go to the documentation site - https://docs.conclave.net