# Key recovery sample

This sample shows how enclave can run a key recovery protocol described in:
https://r3-cev.atlassian.net/wiki/spaces/CTO/pages/2554101797/OCTO-040+Conclave+-+Enclave+Key+Recovery+when+Moving+Machines.
Sample consists of 2 enclaves, one Key Derivation Enclave and one Application Enclave. Both have respective host processes,
code for enclaves can be found in: kde-enclave and app-enclave modules, hosts are kde-host and app-host. Additionally, there
is a client for communication with both enclaves.

Both KDE and App hosts run Spring Boot server, for endpoints for the client and for communication between each other.

## How to run the sample

### Building Conclave sdk

This step is needed because I made changes to the Conclave SDK itself. If those changes get into the release, then this
step can be skipped. For description of changes see appendix at the end of this README. If you don't feel like building this
by yourself, I uploaded custom-built SDK to: https://r3share.mohso.com/navigate/folder/2edc03bc-374e-4bb9-8459-2678e9e600cb

1. You need to set up environment for Conclave development
The steps are described here: https://github.com/corda/sgxjvm#setting-up-a-development-environment

You should install and set up docker to use dev-env container: 
https://github.com/corda/sgxjvm/blob/master/internal-docs/docs/index.md#using-the-devenv-container

I skipped IntelliJ and CLion support from inside the container part, because you will run everything using command line anyway.

Checkout the branch `kstreich-demo-recovery` in the sgxjvm repo (https://github.com/corda/sgxjvm/tree/kstreich-demo-recovery) and run
https://github.com/corda/sgxjvm/blob/master/internal-docs/docs/index.md#entering-the-container

`./scripts/devenv_shell.sh` - this command enters the container. This may take a while...

Skip `build test`, because it takes ages.

Now, to build sdk itself run (again, on a first run it will take ages, I managed to cook dinner):

`./gradlew sdk -x test`

The sdk will be in `build/sdk` directory, zip file can be found in `build/distributions`.

Now, you need to put that newly build sdk in the gradle files of this sample. Go to the `gradle.properties` in this repo.
There are 2 properties defined:

`conclaveVersion=0.1-KEYDEMO` and
`conclaveRepo=<path_to_newly_built_conclave_sdk>`

The `conclaveRepo` should point to the location of the sdk repo you have just built. It should be in:
`<your_local_path_to_conclave_repo>/sgxjvm/build/sdk/repo`

Version is `0.1-KEYDEMO`

Alternatively, you can download the custom-built SDK https://r3share.mohso.com/navigate/folder/2edc03bc-374e-4bb9-8459-2678e9e600cb
and point gradle file to the repo in your filesystem.

There are some additional steps, if you are not using Linux. See Conclave documentation on how to set up your machine:
https://docs.conclave.net/tutorial.html#setting-up-your-machine and https://docs.conclave.net/container-gradle.html

### Running a sample itself

Sample consists of 2 host and enclave processes and a client. Both hosts run Spring boot servers. KDE host is available on
port 9001 and application enclave on 8080. If you would like to change ports, go to the main methods of the hosts and adjust
ports accordingly (you can see in kde-host main how to do that in the code).

Before starting the demo, you need to put code and signer constraints into the both application and kde enclave.
Documentation on an enclave constraints can be found here: https://docs.conclave.net/api/com/r3/conclave/client/EnclaveConstraint.html
and here: https://docs.conclave.net/writing-hello-world.html#constraints

Run
./gradlew kde-host:build

and
./gradlew app-host:build

You should see outputs similar to this:

Enclave code hash:   BE182C666D081B4BBE7B5E397FA192D2007C60A0534E580E683AA4C60FEE7AE2
Enclave code signer: 4924CA3A9C8241A3C0AA1A24A407AA86401D2B79FA9FF84932DA798A942166D4

Of course hashes will be different, if code changes. To read exactly about enclave code hash see:
https://docs.conclave.net/architecture.html#remote-attestation
    
Every time you do any changes to an enclave, you need to update the constraints!
You should put constraints in `AppKeyRecoveryEnclave` in `keyDerivationEnclaveCodeSigningHash` (KDE enclave code signer value)
and in `KeyDistributionEnclave` in `constraintDemo` variable (enclave code hash for app enclave).
Or vice versa, this is for demo purposes.

TODO Note: For simplicity, I put in the Application enclave a signer hash for the KDE. This was only for demo purposes, and should
be changed to code hash. Signer hash is easier, because it doesn't change that much. Additionally, the goal is that KDE
doesn't have hardcoded constraints, just gets them from the client.

1. First run

To start host and enclaves and show key recovery run:

// 9001
./gradlew kde-host:run

Wait till this is done, you should see output:
...
KDE HOST: Enclave started
[main] INFO org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor - Initializing ExecutorService 'applicationTaskExecutor'
[main] INFO org.springframework.boot.web.embedded.tomcat.TomcatWebServer - Tomcat started on port(s): 9001 (http) with context path ''
[main] INFO com.r3.conclave.sample.kdehost.Main$Companion - Started Main.Companion in 2.553 seconds (JVM running for 2.808)

After, run (application key recovery won't work without KDE running!):
// 8080
./gradlew app-host:run

2. Now, watch what happens on app startup, it requests keys from kde, look at KDE and see that it generates keys.
You should see:

HOST: READ SHARED KEY
HOST: Could not read shared key: sharedKey.dat
HOST: Key recovery started
HOST: calling enclave with KDE attestation
Enclave> ENCLAVE: received kde attestation from host
Enclave> ENCLAVE: KDE attestation Remote attestation for enclave BE182C666D081B4BBE7B5E397FA192D2007C60A0534E580E683AA4C60FEE7AE2:
Enclave>   - Mode: SIMULATION
Enclave>   - Code signing key hash: 4924CA3A9C8241A3C0AA1A24A407AA86401D2B79FA9FF84932DA798A942166D4
Enclave>   - Public signing key: 302A300506032B6570032100A4CE81C8A44AE48562905EC79A53E00DD53C3B64E2A03B28BFAB9BB21D50AA54
Enclave>   - Public encryption key: 75B294A19FE8FFA4CEC4FCB7EBEDB573A8F66B7ED4D7934DCE4F0A3A9E3A9764
Enclave>   - Product ID: 2
Enclave>   - Revocation level: 0
Enclave> 
Assessed security level at 2021-07-05T14:08:01.946Z is INSECURE
Enclave>   - Enclave is running in simulation mode.
Enclave> ENCLAVE: check attestation constraints
Enclave> ENCLAVE: requesting key from KDE
Enclave> ENCLAVE: check attestation constraints
Enclave> ENCLAVE: sending key request mail to KDE
HOST: Key request from enclave to be passed to the KDE 
HOST: Route key request
HOST: query for data
HOST: received response from KDE with routing hint: responseKey
Enclave> ENCLAVE: Received mail with response key hint
Enclave> ENCLAVE: load shared key
Enclave> ENCLAVE: check attestation constraints
    HOST: LOAD STORED DATA
HOST: Could not read persistent data: self.dat
[main] INFO org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor - Initializing ExecutorService 'applicationTaskExecutor'
[main] INFO org.springframework.boot.web.embedded.tomcat.TomcatWebServer - Tomcat started on port(s): 8080 (http) with context path ''


3. Now, run client, and see that it returns the new shared key, different from the encryption key
from EnclaveInstanceInfo

./gradlew client:run --args="http://localhost:8080 get-shared-key"

You should see something similar to:
CLIENT: Creating new identity key...
CLIENT: Executing command: get-shared-key
CLIENT: Obtained public shared key from the enclave: Curve25519PublicKey(4258AD9D2AB4CA56F5F7386A5600A5075AF3484ACE95A1A140E2CF9D7F215502)
CLIENT: Encryption key from enclave instance info: Curve25519PublicKey(9D460166A95C9355FD809B3D3863BB52816189AA7128D9F782FA413B91B54865)

4. Now, let's save some data (to see what happens look at client and app enclave terminal). Client generates random numbers,
and asks application enclave to save it.
Application enclave will save those numbers using shared key obtained from KDE

./gradlew client:run --args="http://localhost:8080 save-data"

Output should look more or less like that: 

CLIENT: Executing command: save-data
CLIENT: Generated random data to save: [93, 13, -2, 93, 20, -104, 123, -28, -5, -100, -20, 66, 13, 99, 51, 127, -37, -89, 54, 39, -42, 78, 25, 112, 27, 0, 92, 53, -14, -59, -110, 3]
CLIENT: Sending request to enclave to save data
CLIENT: Save data response: SaveDataResponse(code=OK)

In the application enclave terminal:
HOST: Got mail to deliver to the enclave with routing hint: save-data
Enclave> ENCLAVE: Handling save data from client request
Enclave> ENCLAVE: Saving data from client [93, 13, -2, 93, 20, -104, 123, -28, -5, -100, -20, 66, 13, 99, 51, 127, -37, -89, 54, 39, -42, 78, 25, 112, 27, 0, 92, 53, -14, -59, -110, 3] using shared key Curve25519PublicKey(4258AD9D2AB4CA56F5F7386A5600A5075AF3484ACE95A1A140E2CF9D7F215502)

5. Check you can read data that was just saved by running:

./gradlew client:run --args="http://localhost:8080 read-data"

6. Check that files with shared key and data from client were saved, kill application host
Look for self.dat and sharedKey.dat under `app-host` sample directory.

7. Swap the signing key and restart host. We swap the code signing key, because we are running this demo on a single machine.
To force change of encryption key used by the application enclave we need to force change in code signing key (it's related to 
MRSIGNER key policy - Conclave team should be helpful with explanation). To do this, go to app-enclave/build.gradle and 
change line 21 with 22:
simulation {
        signingType = privateKey
//        signingKey = file("../signing/sample_private_key.pem")
        signingKey = file("../signing/my_private_key.pem")
    }
in the above part change the signing key.    

Then run the application enclave again, check that encryption key is different and that `Enclave code signer` value is different as well:

./gradlew app-host:run

Now you should see that file with the shared key is loaded in the enclave, but enclave fails to decrypt it, so key recovery
process starts again, as before.

HOST: READ SHARED KEY
HOST: Delivering shared key from file to the enclave
HOST: Could not read shared key: java.io.IOException: javax.crypto.AEADBadTagException: Tag mismatch!
HOST: Key recovery started
HOST: calling enclave with KDE attestation
Enclave> ENCLAVE: received kde attestation from host
Enclave> ENCLAVE: KDE attestation Remote attestation for enclave BE182C666D081B4BBE7B5E397FA192D2007C60A0534E580E683AA4C60FEE7AE2:
Enclave>   - Mode: SIMULATION
Enclave>   - Code signing key hash: 4924CA3A9C8241A3C0AA1A24A407AA86401D2B79FA9FF84932DA798A942166D4
Enclave>   - Public signing key: 302A300506032B6570032100A4CE81C8A44AE48562905EC79A53E00DD53C3B64E2A03B28BFAB9BB21D50AA54
Enclave>   - Public encryption key: 75B294A19FE8FFA4CEC4FCB7EBEDB573A8F66B7ED4D7934DCE4F0A3A9E3A9764
Enclave>   - Product ID: 2
Enclave>   - Revocation level: 0
Enclave> 
Assessed security level at 2021-07-05T14:08:01.946Z is INSECURE
Enclave>   - Enclave is running in simulation mode.
Enclave> ENCLAVE: check attestation constraints
Enclave> ENCLAVE: requesting key from KDE
Enclave> ENCLAVE: check attestation constraints
Enclave> ENCLAVE: sending key request mail to KDE
HOST: Key request from enclave to be passed to the KDE 
HOST: Route key request
HOST: query for data
HOST: received response from KDE with routing hint: responseKey
Enclave> ENCLAVE: Received mail with response key hint
Enclave> ENCLAVE: load shared key
Enclave> ENCLAVE: check attestation constraints
    HOST: LOAD STORED DATA
Enclave> ENCLAVE: Received mail with self hint
Enclave> ENCLAVE: Handling mail to self
Enclave> ENCLAVE: Read stored data [93, 13, -2, 93, 20, -104, 123, -28, -5, -100, -20, 66, 13, 99, 51, 127, -37, -89, 54, 39, -42, 78, 25, 112, 27, 0, 92, 53, -14, -59, -110, 3]

In the above logs the application enclave can't read the saved shared key, then requests that key from KDE, KDE sends it back
Now it can read the saved data that was previously sent to it by the client.

8. Let's check if client can request the saved data from the restarted enclave:

./gradlew client:run --args="http://localhost:8080 read-data"


## Appendix: Changes to Conclave sdk

To make this demo possible, some changes to Conclave sdk had to be introduced.

1. `Enclave::setSharedKeyPair(newKeyPair: KeyPair)` was added to set new shared key pair to be used by this enclave.
2. `Enclave::sharedPostOffice(destinationPublicKey: PublicKey)` to create post office using shared key aimed at destination public key
3. `Enclave::getSharedPublicKey` to obtain shared public key information
4. From client perspective, `EnclaveInstanceInfo::createClusterPostOffice` to create post office using given cluster key for communication
with the enclave.
5. Additionally, decryption within the enclave changed, to use shared key if key derivation parameter is set to null.

