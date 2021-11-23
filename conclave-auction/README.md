# Conclave Auction Sample

<p align="center">
  <img src="https://conclave.net/wp-content/uploads/2020/12/Conclave_logo_master.png" alt="Conclave" width="500">
</p>
<br>

This is a sample use case for R3's Conclave Confidential Computing Platform.
It allows bidders to confidentially submit their bids to an enclave (A protected 
region of memory which cannot be accessed by the OS, Kernel or BIOS). The bids are 
processed confidentially in the enclave and the result is returned to concerned parties.


## Conclave SDK Location
In order to compile this sample successfully, kindly download the conclave SDK 
and update the location in gradle.properties.

`conclaveRepo=<path_to_conclave_sdk>`


Conclave SDK can ve downloaded from here: https://conclave.net/get-conclave/

## Running the sample
Kindly refer to our official documentation for machine requirements to run the sample.
https://docs.conclave.net/tutorial.html#setting-up-your-machine

### Running the web host server


`./gradlew host:run`

Use `-PenclaveMode` if you wish to specify an enclave mode, default is simulation mode

`./gradlew host:assemble -PenclaveMode=debug`

### Running in Simulation Mode

#### Running the host

`./gradlew host:installDist`

The host is required to be run using docker for mac and windows.

`docker run -it --rm -p 5051:5051 -v $PWD:/project -w /project --user $(id -u):$(id -g) conclave-build /bin/bash`

For linux system, you could skip the above docker command.

`cd host/build/install`

`./host/bin/host`

#### Running the client

Run the client to sumbit a bid using the below command:

`./gradlew runClient --args="BID <enclave_constraint>"`

The enclave constraint used in this sample is the `code signer`,  which can be found printed during the build process as in the example below:

```
Enclave code hash:   26037FC0370589FEA489110ED8124223650F5620B0732F94CDDEDDF207F07457
Enclave code signer: 4502FEF2B5973A9DCF2F5C85358ED9F099C7738300364A7D7451371E43694A85
```

In this case the code signer `4502FEF2B5973A9DCF2F5C85358ED9F099C7738300364A7D7451371E43694A85` is the enclave constraint.

You could run multiple clients to submit different bids.

To process the bid run the below command:

`./gradlew runClient PROCESS-BID <enclave_constraint>`

The enclave should reply with a response message indicating 
the auction winner to the admin client, and each individual bidders
should get a response from the enclave regarding their bid status.


### Running in Mock Mode

#### Running the host

Use the below command to start the web host server.
`./gradlew -PenclaveMode=mock host:run`

The web host server is an out-of-the-box spring boot server which serves as a host component for 
a simple conclave application. It perform common tasks done by a host like, load enclave, relay mails 
between client and enclave, etc. which can be called via exposed APIs.

#### Running the client

Run the client to submit a bid using the below command:

`./gradlew runClient --args="BIDDER <enclave_constraint> <host-url>, <bid-amount>"`

If you are building an enclave in mock mode then the enclave reports it is using a signing key hash 
consisting of all zeros. 

`./gradlew runClient --args="'BIDDER' ’S:0000000000000000000000000000000000000000000000000000000000000000 PROD:1 SEC:INSECURE' 'http://localhost:8080' 1000"`

You could run multiple clients to submit different bids.

To process the bid run the below command:

`./gradlew runClient --args="'ADMIN' 'S:0000000000000000000000000000000000000000000000000000000000000000 PROD:1 SEC:INSECURE' 'http://localhost:8080'"`

The enclave should reply with a response message indicating 
the auction winner to the admin client, and each individual bidders
should get a response from the enclave regarding their bid status.

#### More about enclave constraint
In this sample the `code signer` is used as enclave constraint, but you can also use the `code hash`. If you want to use it, remember to change the code of the client to:

`C:"+ code hash +" SEC:INSECURE"`

Read more in the https://docs.conclave.net/enclave-configuration.html#productid. 

You can read more on the constraint here. https://docs.conclave.net/writing-hello-world.html#constraints.