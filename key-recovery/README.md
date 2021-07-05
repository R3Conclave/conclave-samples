# Key recovery sample

TODO

## How to run the sample

### Building Conclave sdk

This step is needed because I made changes to the Conclave SDK itself. If those changes get into the release, then this
step can be skipped.

1. You need to set up environment for Conclave development
The steps are described here: https://github.com/corda/sgxjvm#setting-up-a-development-environment

You should install and set up docker to use dev-env container: 
https://github.com/corda/sgxjvm/blob/master/internal-docs/docs/index.md#using-the-devenv-container

I skipped IntelliJ and CLion support from inside the container part, because you will run everything using command line anyway.

Checkout the branch `kstreich-demo-recovery` in the sgxjvm repo (https://github.com/corda/sgxjvm/tree/kstreich-demo-recovery) and run
https://github.com/corda/sgxjvm/blob/master/internal-docs/docs/index.md#entering-the-container

`./scripts/devenv_shell.sh` - this command enters the container.

Skip `build test`, because it takes ages.

Now, to build sdk itself run:

`./gradlew sdk -x test`

The sdk will be in `build/sdk` directory, zip file can be found in `build/distributions`.

Now, you need to put that newly build sdk in the gradle files of this sample. Go to the `gradle.properties` in this repo.
There are 2 properties defined:

`conclaveVersion=0.1-KEYDEMO` and
`conclaveRepo=<path_to_newly_built_conclave_sdk>`

The `conclaveRepo` should point to the location of the sdk repo you have just built. It should be in:
`<your_local_path_to_conclave_repo>/sgxjvm/build/sdk/repo`

Version is `0.1-KEYDEMO`

Alternatively, you can download a jar TODO WHERE and point gradle file to the repo in your filesystem.

// TODO link to documentation of sgx about repo setup
// TODO for Mac and Windows you need additional steps: https://docs.conclave.net/tutorial.html#setting-up-your-machine
### Running a sample itself

Sample consists of 2 host and enclave processes and a client. Both hosts run Spring boot servers. KDE host is available on
port 9001 and application enclave on 8080. If you would like to change ports, go to the main methods of the hosts and adjust
port accordingly (you can see in kde-host main how to do that in code).

Before starting the demo, you need to put code and signer constraints into the both application and kde enclave.
Documentation on an enclave constraints can be found here: https://docs.conclave.net/api/com/r3/conclave/client/EnclaveConstraint.html
and here: https://docs.conclave.net/writing-hello-world.html#constraints

TODO MAC - instructions - https://docs.conclave.net/container-gradle.html
TODO add steps to build it:
./gradlew kde-host:build
./gradlew app-host:build

 
To get code and signer hashes you need to build both enclaves. On enclave startup, there will be information print out looking
more or less like this:
``Remote attestation for enclave F86798C4B12BE12073B87C3F57E66BCE7A541EE3D0DDA4FE8853471139C9393F:
    - Mode: SIMULATION
    - Code signing key hash: 01280A6F7EAC8799C5CFDB1F11FF34BC9AE9A5BC7A7F7F54C77475F445897E3B
    - Public signing key: 302A300506032B65700321000568034F335BE25386FD405A5997C25F49508AA173E0B413113F9A80C9BBF542
    - Public encryption key: A0227D6D11078AAB73407D76DB9135C0D43A22BEACB0027D166937C18C5A7973
    - Product ID: 1
    - Revocation level: 0``
    
We are interested in the first line, and the line with "Code signing hash".

Every time you do any changes to an enclave, you need to update the constraints!
You should put constraints in `AppKeyRecoveryEnclave` in `keyDerivationEnclaveCodeSigningHash`
and in `KeyDistributionEnclave` in `constraintDemo` variable.

1. First run

// 9001
./gradlew kde-host:run

// 8080
./gradlew app-host:run

2. Watch what happens on app startup, it requests keys from kde, look at kde and see that it generates keys
3. Now, run client, and see that it returns the new shared key, different from the encryption key
from EnclaveInstanceInfo

./gradlew client:run --args="http://localhost:8080 get-shared-key"

TODO RETURN

4. Now, let's save some data (client and app enclave terminal)

./gradlew client:run --args="http://localhost:8080 save-data"

TODO RETURN

5. Check you can read them (client and app enclave terminal)

./gradlew client:run --args="http://localhost:8080 read-data"

TODO RETURN

6. Check that files are saved, kill app host
TODO RETURN
7. Swap the signing key and restart host TODO HOW TO DO THAT

./gradlew app-host:run

TODO RETURN logs

8. Look at the magic happening, the new enclave can't read the saved shared key, then requests that key from KDE, KDE sends it back
Now it can read the saved data

TODO RETURN logs