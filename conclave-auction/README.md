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

## Running the sample in Mock Mode
Kindly refer to our official documentation for machine requirements to run the sample.
https://docs.conclave.net/tutorial.html#setting-up-your-machine

### Running the web host server

Run the web host server using the below command.

`./gradlew host:run`

The web host server is an out-of-the-box spring boot server which serves as a host component for
a simple conclave application. It performs common tasks done by a host like, load enclave, relay mails
between client and enclave, etc. which can be called via exposed APIs.

### Running the client

Run the client to submit a bid using the below command:

`./gradlew runClient --args="<role-type> <enclave_constraint> <host-url> <bid-amount>"`

The enclave constraint used in this sample is the `code signer`,  which is printed
on the console as you run the host as shown in the example below. If you are building 
an enclave in mock mode then the enclave reports it is using a signing key hash
consisting of all zeros.

```
Remote attestation for enclave 6C5AE57C0D779D635FBF5227CE1DEC4A0736BD5F02CC8E8E6DB61F76DE56C1F0:
  - Mode: MOCK
  - Code signing key hash: 0000000000000000000000000000000000000000000000000000000000000000
  - Public signing key: 302A300506032B65700321005C7190C3011ECD16501EE1FD15167F42E112D4E074F1B6292CE2D046F4F1089F
  - Public encryption key: BBD680B8E3A49ADD3CF018714A2B0D997315195E8B842F49385331D0532B5646
  - Product ID: 1
  - Revocation level: 0

```

In this case the code signer `0000000000000000000000000000000000000000000000000000000000000000` is the enclave constraint.

You can submit bids using the below command with role type as bidder:
`./gradlew runClient --args="'BIDDER' ’S:0000000000000000000000000000000000000000000000000000000000000000 PROD:1 SEC:INSECURE' 'http://localhost:8080' 1000"`

You could run multiple clients to submit different bids.

To process the bid run the below command as admin:

`./gradlew runClient --args="'ADMIN' 'S:0000000000000000000000000000000000000000000000000000000000000000 PROD:1 SEC:INSECURE' 'http://localhost:8080'"`

The enclave should reply with a response message indicating 
the auction winner to the admin client, and each individual bidders
should get a response from the enclave regarding their bid status.

#### More about enclave constraint
In this sample the `code signer` is used as enclave constraint, but you can also use the `code hash`. If you want to use it, remember to change the code of the client to:

`C:"+ code hash +" SEC:INSECURE"`

Read more in the https://docs.conclave.net/enclave-configuration.html#productid. 

You can read more on the constraint here. https://docs.conclave.net/writing-hello-world.html#constraints.