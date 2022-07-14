# Conclave Hello World Python Sample

This sample shows how to execute a python code inside a conclave enclave.
Please note this sample uses the hello world sample which is available as part of the conclave sdk.
Below ``Configurations to be added to a hello world conclave sample`` section talks about the configurations to be added 
to this sample to enable it to execute python code.
Conclave support for Python is provided by the polyglot capabilities in GraalVM. 
Refer to the GraalVM [documentation](https://www.graalvm.org/reference-manual/embed-languages/) on embedding languages 
for detailed instructions on how to use this capability.

When you use the graalvm-native-image runtime you have the option to enable Python language support. 
When enabled you can create a context in which you can load and execute a Python source module.

# What are the different modes in which you can run a typical Conclave application

There are four modes in which an enclave can be loaded
1. Release Mode
2. Debug Mode
3. Simulation Mode
4. Mock Mode

For this sample we will run our sample in mock mode.
You can read more about modes [here](https://docs.conclave.net/enclave-modes.html).

# Configurations to be added to a hello world conclave sample

If you have not yet built any conclave application, take a look at this 
[page](https://docs.conclave.net/writing-hello-world.html) which talks about how to run a simple hello world
conclave application.

By default python is not enabled in Conclave.
To enable python support take a look at this docs [page](https://docs.conclave.net/javascript-python.html#enable-javascriptpython-in-the-conclave-configuration).
which talks about the configurations which needs to be added to 
enable python in Conclave.

# Steps to run the Hello World Python sample in mock mode

For this sample, we will run our sample in mock mode.
Before running this sample in mock mode make sure to perform below steps.

1. Download [graalvm-ce-java11](https://github.com/graalvm/graalvm-ce-builds/releases). 
2. Set the environment variable JAVA_HOME to point to the GraalVM that was previously downloaded. 
   For instance, export JAVA_HOME=/usr/lib/jvm/graalvm-ce-java11-21.1.0).
3. Update the environment variable PATH by running export PATH=$JAVA_HOME/bin:$PATH.
4. For Python only - Install the Python component by running the command gu install python.

Once you have set GraalVM as your JAVA_HOME, we will build and start the host and the client

Start the host by executing below command, which will build the enclave and host:

```bash
./gradlew host:bootJar
java -jar host/build/libs/host-mock.jar
```

It should print out some info about the started enclave. Then you can use the client to send it strings to reverse:

```bash
./gradlew client:shadowJar
java -jar client/build/libs/client.jar "S:0000000000000000000000000000000000000000000000000000000000000000 PROD:1 SEC:INSECURE" "reverse me"
```