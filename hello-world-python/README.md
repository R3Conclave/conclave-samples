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
1. Release Mode - You need a real Intel SGX CPU. You load the enclave in release mode when you are in production.
2. Debug Mode - You need a real Intel SGX CPU. This provides you with certain capabilities whcih enables you to debug
   your enclave application.
3. Simulation Mode - This does not require you to have an Intel SGX CPU. This can be used to test your conclave 
   applications when in development mode. This mode requires a linux env. This can be run inside a Docker container 
   on Mac. Enclave code is compiled to native binary and loaded inside a separate JVM inside your host JVM.
4. Mock Mode - Enclave code can be directly run inside the host VM. This is very fast, and hence should ideally be used
   for day to day rapid development and testing.

To read more about modes go to the [docs](https://docs.conclave.net/enclave-modes.html).

# Configurations to be added to a hello world conclave sample

If you have not yet built any conclave application, take a look at this 
[page](https://docs.conclave.net/writing-hello-world.html) which talks about how to run a simple hello world
conclave application.

To build the application using conclave init tool take a look [here](https://docs.conclave.net/conclave-init.html)

Python is not enabled by default. 
In order to enable support add the following line to your enclave build.gradle.

      conclave {
      productID = 1
      ...
      supportLanguages = "python"
      }

Adding this line pulls all the required python dependencies.

Let's define our python function which we want to execute inside the enclave in the ReverseEnclave.class

      private static final String pythonCode = "def reverse(input):\n"
      + " return input[::-1]";

Let us also define the Bindings and the Context 

      private final Context context;
      private final Value bindings;

Let us now define the bindings in the constructor

      public ReverseEnclave() {
      context = Context.create("python");
      bindings = context.getBindings("python");
      context.eval("python", pythonCode);
      }

This sets up the bindings to Java which allow us to access the functions and variables that are defined within the 
Python code.

Below code snippet shows you how we can execute the function binded by use before.

      private String reverse(String input) {
        Value result = bindings.getMember("reverse").execute(input);
        return result.asString();
    }

Now we are done with adding the configurations and now let's see hwo to run this sample in mock mode.

# Steps to run the Hello World python sample in mock mode

For this sample, we will run our sample in mock mode.
There is development work going on to make it work in the simulation and other modes.
Before running this sample in mock mode make sure to perform below steps.

1. Download [graalvm-ce-java11](https://github.com/graalvm/graalvm-ce-builds/releases). 
2. Set the environment variable JAVA_HOME to point to the GraalVM that was previously downloaded. 
   For instance, export JAVA_HOME=/usr/lib/jvm/graalvm-ce-java11-21.1.0).
3. Update the environment variable PATH by running export PATH=JAVA_HOME/bin:$PATH.
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

## Next step
For more information on how to run a python code in Conclave take a look at the [docs](https://docs.conclave.net/javascript-python.html).
Take a look at 

This is a simple app using the Conclave API. It is licensed under the Apache 2 license, and therefore you may
copy/paste it to act as the basis of your own commercial or open source apps.
