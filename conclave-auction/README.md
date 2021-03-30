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

### Assemble the host

`./gradlew host:assemble`

Use `-PenclaveMode` if you wish to specify an enclave mode, default is simulation mode

`./gradlew host:assemble -PenclaveMode=debug`

### Running the host

`./gradlew host:installDist`

The host is required to be run using docker for mac and windows.

`docker run -it --rm -p 5051:5051 -v $PWD:/project -w /project --user $(id -u):$(id -g) conclave-build /bin/bash`

For linux system, you could skip the above docker command.

`cd host/build/install`

`./host/bin/host`

### Running the client

Run the client to sumbit a bid using the below command:

`./gradlew runClient --args="BID <enclave_constraint>"`

The enclave constraint can be found printed during the build process as below:

`Enclave code hash:   DB2AF8DD327D18965D50932E08BE4CB663436162CB7641269A4E611FC0956C5F`, In this 
case the hash `DB2AF8DD327D18965D50932E08BE4CB663436162CB7641269A4E611FC0956C5F` is the enclave constraint.

You could run multiple clients to submit different bids.

To process the bid run the below command:

`./gradlew runClient PROCESS-BID <enclave_constraint>`

The enclave should reply with a response message indicating 
the auction winner to the admin client, and each individual bidders
should get a response from the enclave regarding their bid status.
