## Conclave Column Profiling Sample

<p align="center">
  <img src="https://conclave.net/wp-content/uploads/2020/12/Conclave_logo_master.png" alt="Conclave" width="500">
</p>
<br>

This is a sample use case for R3's Conclave Confidential Computing Platform. It does Column Profiling on the dataset and
returns the frequency distribution as an output. Column profiling is one of the methods used in data profiling.
This [link](https://www.alooma.com/blog/what-is-data-profiling) explains what data profiling and column profiling means
and explains its uses and applications. The application takes in User fields: name, age, country and gender. Name, age,
country and gender are the columns in the dataset. This could be further extended/modified as per need. The application
returns the number of occurrence of each distinct values in the columns of age, gender and country. A sample output will
look like this: Age Frequency Distribution: {20=2, 45=2} Country Frequency Distribution: {USA=2, UK=1, India=1} Gender
Frequency Distribution: {na=1, f=1, m=2}. This means the value 20 and 45 have appeared 2 times in the age column. USA,
UK , India has appeared 2,1 and 1 times respectively.

## How to run in simulation mode

Kindly refer to our official documentation for machine requirements to run the sample.
https://docs.conclave.net/tutorial.html#setting-up-your-machine

### How to run on a non-linux based system

#### Running the Host

1. Download and install docker desktop. Start the Docker Desktop application.
2. Build your project as you normally would in your desired mode, e.g.: `./gradlew build -PenclaveMode=simulation`
3. Navigate to your project and run the following command: `./gradlew enclave:setupLinuxExecEnvironment`. This will
   create a docker image called `conclave-build` that can be instantiated as a container and used to run conclave
   projects.
4. Execute the following command from the root directory of your project to instantiate a container using the image
   `docker run -it --rm -p 8080:8080 -v ${PWD}:/project -w /project conclave-build /bin/bash`. This will give you a bash
   shell in a Linux environment that you can use to run your project as if you were on a native Linux machine. Make sure
   the container has all the required dependencies to run the host. Also ensure that the conclave-sdk is present within.

Start the host inside the container, which will build the enclave and host. You will find a file named
`host-simulation-1.3.jar` inside `host/build/libs`

```
cd host/build/libs
java -jar host-simulation-1.3.jar
```

It should print out some info about the started enclave `Started EnclaveWebHost.Companion in ... seconds` This will
start the Conclave Web host with your enclave. Then you can use the client to send it strings to reverse. Run the
client, to send data to the enclave.

#### Running the client

To run the client, you will need the Enclave constraint. The enclave constraint will include acceptable enclave code
signing key hash, acceptable signers, etc. Please refer to the Class EnclaveConstraint in
the [docs](https://docs.conclave.net/api/index.html) for more details. The Enclave code signing key hash can be found
printed during the build process as below:

`Code signing key hash: 4924CA3A9C8241A3C0AA1A24A407AA86401D2B79FA9FF84932DA798A942166D4`, In this case the
hash `4924CA3A9C8241A3C0AA1A24A407AA86401D2B79FA9FF84932DA798A942166D4` is the enclave constraint.

Here we are running the sample in Simulation mode, so the "SEC" key in constraint string would be `INSECURE`. Change
this to the `STALE` or `SECURITY` as applicable when you are running in hardware mode.

Ultimately pass your enclave constraint as argument. Further, pass your dataset in arguments as space separated values.
Here user data is being passed , which includes the following fields:
Name:String Age:Integer Country:String Gender:String(m,f)

To find the frequency distribution run the below command:
`cd client/build/libs`
`java -jar client-1.3.jar John 45 USA m Sera 45 India f Jacod 20 USA m Emily 20 UK na --constraint "S:4924CA3A9C8241A3C0AA1A24A407AA86401D2B79FA9FF84932DA798A942166D4 PROD:1 SEC:INSECURE" --url "http://localhost:8080"`

The enclave should reply with a response message indicating the freq distribution of age, country and gender. The
response will look like below:
`Enclave gave us the answer 'Age Frequency Distribution: {20=2, 45=2} Country Frequency Distribution: {USA=2, UK=1, India=1} Gender Frequency Distribution: {na=1, f=1, m=2}'`

### How to run on linux based system

Start the host on a non-Linux system, which will build the enclave and host:

      ./gradlew host:assemble -PenclaveMode=simulation
      ./gradlew host:run

On your terminal, once the host starts, run your client

      ./gradlew client:run John 45 USA m Sera 45 India f Jacod 20 USA m Emily 20 UK na --constraint "S:4924CA3A9C8241A3C0AA1A24A407AA86401D2B79FA9FF84932DA798A942166D4 PROD:1 SEC:INSECURE" --url "http://localhost:8080" 

To read more on Conclave go to the documentation site - https://docs.conclave.net
The example could further be extended to include complex data fields and to perform other data operations. Though this
example demonstrates the use of Conclave for data analysis by a single organization, Conclave is equally well suited for
multi-party computation and collaboration.

The repository includes configuration and serialization files for native-image builds -
enclave/src/main/resources/META-INF/native-image. These files could be generated
using [native-image-agent](https://www.graalvm.org/reference-manual/native-image/BuildConfiguration/#assisted-configuration-of-native-image-builds)
. The agent tracks usage of dynamic features and generates configuration files when run against a regular JVM. If you
make any changes to the Enclave code, you may want to either manually append these configuration files or run the
native-agent again. For running the agent, please refer to the steps mentioned in the [docs](https://docs.conclave.net/enclave-configuration.html#assisted-configuration-of-native-image-builds).
