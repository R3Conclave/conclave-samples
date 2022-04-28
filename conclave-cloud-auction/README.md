# Conclave Auction using Conclave Cloud

This is a sample use case for R3's Conclave Confidential Computing Platform. 
It allows bidders to confidentially submit their bids to an enclave (A protected region of memory which cannot be accessed by the OS, Kernel or BIOS). The bids are processed confidentially in the enclave and the result is returned to concerned parties.

## How is Conclave Auction architected?

Each component in this application is named corresponding to the directories in this repository that
contain the implementation for the component.

Each component is described below:

### Functions
The functions consist of a typescript component that provides methods that can
be used to add, query, get and remove entries within the user's database. This
requires access to the unencrypted database and therefore, to ensure privacy,
runtime encryption is used by hosting the functions within Conclave Cloud in the
Conclave Functions service.

Conclave Functions are stateless and do not have access to any persistent
storage. Therefore, external storage within the cloud is necessary in order to
persist the user databases. The functions component uses the JavaScript
`fetch()` built-in capability to query and update an external data store with
each user's encrypted database entries. It is vitally important to ensure that
any data exchanged via `fetch()` is encrypted as the request is made outside the
Conclave Functions Intel SGX enclave. The functions module uses another built-in
function, `crypto.getProjectKey()` to get a key that is unique to the project and
function code and uses this to encrypt/decrypt the user's database prior to
exchanging it with the external storage.

### Backend
The backend service is used to store and retrieve the encrypted database for
each user. It only ever handles data that has been encrypted using a key that
can only be accessed within the functions component, ensuring that no
unauthorised entity can gain access to the user databases.

The simple implementation provided within this project consists of a Spring
application that stores the databases in a string in memory. All entries
are lost if the service is restarted.

### Frontend
The frontend provides a web-browser application that allows login/logout and
management of user keys. This has been implemented using the Angular framework
and demonstrates how the Conclave Cloud JavaScript SDK can be used to interact
with Conclave Functions.

### Cli
The CLI is a terminal-based tool written in Kotlin that allows login/logout and
management of user keys. This demonstrates how the Conclave Cloud Kotlin/Java
SDK can be used to interact with Conclave Functions.

## How is privacy preserved?
With the ConclaveAuction solution, the only component that has access to the user
databases in an unencrypted form is the function code that runs within the
Conclave Functions service. But how do we ensure that is the case?

Conclave Cloud makes it easy to ensure that only authorised function code can
access data. It does this by running the code within an Intel SGX enclave and
providing an attestation that proves the validity of the platform as well as the
integrity and authenticity of the code running inside the enclave. The details
of this whole process are handled by the Conclave Cloud platform and the client
SDKs.

Let's take a look first at how a user sends a bid to the service. The
frontend or CLI will call the `addBid` function, passing the user's password
and details of the new password entry. This information obviously should not be
sent unencrypted. Furthermore it should be encrypted using a key that is only
accessible to the set of functions that have been approved by the user to handle
the user's database.

The Conclave Cloud client SDK obtains a public key from the platform that is
signed by a report that proves the private key can only be accessed within an
approved Conclave Functions enclave. The client SDK validates the report and
ensures the key signature does indeed match the signature in the report. Once
this key has been established we can encrypt data using this key safe in the
knowledge that only a valid Conclave Functions enclave can decrypt it.

The bid entry is encrypted using this key and the `bidEntry` function
invoked. This is then picked up inside a Conclave Functions enclave which can
decrypt the parameters using the private key that only it has access to. The
function retrieves the encrypted user database from the external service.

The bid entry is encrypted using this key and the `bidEntry` function
invoked. This is then picked up inside a Conclave Functions enclave which can
decrypt the parameters using the private key that only it has access to. The
function retrieves the encrypted user database from the external service.

The function decrypts the database, adds the new entry, re-encrypts it then
sends it back to the backend.

So, we can see that the only entity that has access to all the secrets required
to access each user's database entry is Conclave Functions, and only then when
running the exact same code that the user expects the functions to be running,
and when provided with the user's bid.

## Building and deploying the demonstration
Each component must be built and deployed or hosted in order to run the
demonstration.

### 1. Backend to allow Conclave Functions to access it for storing user databases.

It is recommended to set up a new virtual machine with your cloud service
provider to host this. The resource requirements are tiny so you can use the
smallest size of virtual machine your cloud service provider provides.

The service is built using this command:

```
./gradlew build
```

You can then deploy the `build/libs/conclaveauction-0.0.1-SNAPSHOT.jar ` file to
your virtual machine and run it with:

```
java -jar ./conclaveauction-0.0.1-SNAPSHOT.jar 
```

### 2. Functions
Note down the IP address for the backend service that you have just deployed and
update the functions module to connect to the service with that address in
`functions/src/index.ts`.

Then see the instructions in [functions/README.md](functions/README.md) for
information on how to setup a Conclave Cloud project and build and deploy the
functions ready for use.

### 3. Frontend
The Conclave Cloud JavaScript client SDK is not yet hosted in a public
repository so you will need to download and unzip it locally and update the
frontend project to use your local copy.

Instructions on how to do this and build and run the frontend can be found in
[frontend/README.md](frontend/README.md).

Once the frontend is running you can access it at http://localhost:4200.

### 4. CLI
The Conclave Cloud Java client SDK is not yet hosted in a public repository so
you will need to download and unzip it locally and update the CLI project
to use your local copy.

Instructions on how to do this and build and run the CLI can be found in
[frontend/README.md](frontend/README.md).



Steps to run Auction application

Login to CCL using sneha.damle@r3.com and invoke below functions

cli add --bid "8000"

Login to CCL using tom.ron@gmail.com and invoke below functions

cli add --bid "7000"

cli calculateBidWinner

The winner of the auction is : sneha.damle@r3.com with bid amount : 8000

