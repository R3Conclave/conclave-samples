## Enclave persistence

This is a sample to demonstrate the use of persistent map and persistent filesystem in your enclave. Conclave provides
two ways to persist information: the persistent map and persistent filesystem. Both these could be used to persist
information during enclave restarts. More on Persistent Map and Persistent Filesystem can be found below.

This Conclave sample demonstrates password authentication within an enclave. The enclave performs 2 major tasks:

1. Stores the password in Persistent Map.
2. Validates a given password with the stored password.

To see a sample on how to use Persistent Filesystem within your enclave, refer to `Persistent Enclave`. This sample has
been tested in Simulation mode. Please note that this sample is for demonstration purpose and has not been tested for
security.

This is a simple app using the Conclave API. It is licensed under the Apache 2 license, and therefore you may copy/paste
it to act as the basis of your own commercial or open source apps.

# How to run

## Mac OS using Docker

1. Download and install docker desktop. Start the Docker Desktop application.
2. Build your project as you normally would in your desired mode, e.g.: `./gradlew build -PenclaveMode=simulation`
3. Navigate to your project and run the following command: `./gradlew enclave:setupLinuxExecEnvironment`. This will
   create a docker image called `conclave-build` that can be instantiated as a container and used to run conclave
   projects.
4. Execute the following command from the root directory of your project to instantiate a container using the image
   `docker run -it --rm -p 8080:8080 -v ${PWD}:/project -w /project conclave-build /bin/bash`. This will give you a bash
   shell in a Linux environment that you can use to run your project as if you were on a native Linux machine. Please
   note, this command may not be suitable for _your_ specific project! Consult the instructions below for more
   information. Make sure the container has all the required dependencies to run the host. Also ensure that the
   conclave-sdk is present within.

Start the host inside the container, which will build the enclave and host. You will find a file named
`host-simulation-1.2.jar` inside `host/build/libs`

```
cd host/build/libs
java -jar host-simulation-1.2.jar --sealed.state.file="./test.disk" --filesystem.file="./conclave.disk"
```

The `--sealed.state.file` is the path at which the enclave should store the sealed state containing the persistent map
while `--filesystem.file` is the path to the encrypted filesystem.

It should print out some info about the started enclave `Started EnclaveWebHost.Companion in ... seconds` Then you can
use the client to send it strings to reverse. Run the client, to send data to the enclave.

```
cd client/build/libs
java -jar client-1.2.jar --constraint "S:4924CA3A9C8241A3C0AA1A24A407AA86401D2B79FA9FF84932DA798A942166D4 PROD:1 SEC:INSECURE" --file-state "client-state" --url "http://localhost:8080" ADD user myPassword
java -jar client-1.2.jar --constraint "S:4924CA3A9C8241A3C0AA1A24A407AA86401D2B79FA9FF84932DA798A942166D4 PROD:1 SEC:INSECURE" --file-state "client-state" --url "http://localhost:8080" VERIFY user myPassword

```

To confirm whether your information has persisted or not, please kill the host process using `Ctrl+C` and restart it
with the above command. You can check for the old content using the above commands.

**Please Note that : Only debug and simulation modes support output to the console from inside the enclave through the
use of System.out.println(). Make sure to remove the log statement from your enclave file while running in release mode.

## Persistent Map and Persistent Filesystem

The persistent filesystem is a can be used to store files from the enclave. This filesystem is encrypted by the Enclave
and is stored by the host in non-volatile storage, such as the disk of the host. On the other hand, the persistent map
is a persistent, encrypted key-value store that can be used to store information from your enclave. The enclave can use
this key-value store to persist information as required. Both persistent map and persistent filesystem come with their
pros and cons. Which one to use depends on the application logic and how much the users of the enclave trust the host.

1. Rollback attacks: The persistent filesystem is susceptible to rewind(or rollback) attacks. A malicious host can
   perform a rewind (or rollback) attack by deliberately restarting the enclave, providing an older image of the
   encrypted filesystem. On the other hand, persistent map is resistant to it.
2. Transactionality: Writes to the persistent filesystem are made immediately, i.e. they are not transactional. This
   means they are fast and are not buffered. However, it also means changes are still persisted if the enclave
   subsequently throws an exception. This has implications if the state of the enclave needs to be in sync with the
   state of its clients. If the changes that were persisted are linked to mail that was supposed to be sent to clients (
   but was not due to the exception) then the state of the clients will not be in-line. In case of persistent map
   changes to the persistent map are only committed at the end of every `receiveMail` and
   `receiveFromUntrustedHost` call where its entire contents is serialized and encrypted into a single blob. This
   enables transactionality with respect to the processing of mail.
3. Performance: One must note that persistent map does come at the cost of a performance overhead and so the map should
   be used judiciously. More details about the persistent map and persistent filesystem could be
   found [here](https://docs.conclave.net/persistence.html#conclave-filesystems)

### Note on Conclave modes

For a list of modes and their properties, see [Enclave modes](https://docs.conclave.net/tutorial.html#enclave-modes) in
the documentation of Conclave. For instructions on how to set the mode at build time,
see [Selecting your mode](https://docs.conclave.net/tutorial.html#selecting-your-mode) in the documentation
of [Conclave](https://docs.conclave.net/)
