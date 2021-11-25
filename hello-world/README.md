## Conclave Sample

This is a sample to demonstrate the use of Persistent Filesystem and persistent map in your enclave. Please refer to the ReverseEnclave.java for further details.
The Persistent Map can be used in all modes, while the Persistent Filesystem will work in all the other modes except Mock. This sample has been tested in Simulation mode.

This is a simple app using the Conclave API. It is licensed under the Apache 2 license, and therefore you may 
copy/paste it to act as the basis of your own commercial or open source apps.

# How to run

## Mac OS using Docker
1. Download and install docker desktop. 
2. Build your project as you normally would in your desired mode, e.g.: `./gradlew build -PenclaveMode=simulation`
3. Navigate to your project and run the following command: `./gradlew enclave:setupLinuxExecEnvironment`. This
   will create a docker image called `conclave-build` that can be instantiated as a container and used to run
   conclave projects.
4. Execute the following command from the root directory of your project to instantiate a container using the image
   `docker run -it --rm -p 8080:8080 -v ${PWD}:/project -w /project conclave-build /bin/bash`. This will give you a
   bash shell in a Linux environment that you can use to run your project as if you were on a native Linux machine.
   Please note, this command may not be suitable for _your_ specific project! Consult the instructions below for
   more information. Make sure the container has all the required dependencies to run the host. Also ensure that the conclave-sdk is present within.

Start the host inside the container, which will build the enclave and host. You will find a file names host-simulation-1.2-RC1.jar inside host/build/libs
```
cd hello-world/host/build/libs
java -jar host-simulation-1.2-RC3-SNAPSHOT.jar --sealed.state.file="./test.disk" --filesystem.file="./conclave.disk"
```

It should print out some info about the started enclave. Then you can use the client to send it strings to reverse.
Run the client, to send data to the enclave.
```
java -jar client-1.2-RC3-SNAPSHOT.jar --constraint "S:4924CA3A9C8241A3C0AA1A24A407AA86401D2B79FA9FF84932DA798A942166D4 PROD:1 SEC:INSECURE" --file-state "client-state" --url "http://localhost:8080" reverse-me
```
To confirm whether your information has persisted or not - Kill the host process and restart it with the above command. You can see the old content in the logs.
```
ps aux
kill -9 PID
```

**Please Note that : Only debug and simulation modes support output to the console from inside the enclave through the use of System.out.println(). Make sure to remove the log statement from your enclave file 
while running in release mode.

### Note on conclave modes
For a list of modes and their properties, see [here](https://docs.conclave.net/tutorial.html#enclave-modes).
For instructions on how to set the mode at build time, see [here](https://docs.conclave.net/tutorial.html#selecting-your-mode).
