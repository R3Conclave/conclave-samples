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

To run the client, you will need the Enclave constraint. The enclave constraint will include acceptable Enclave code
hashes, acceptable signers, etc. Please refer to the Class EnclaveConstraint in
the [docs](https://docs.conclave.net/api/index.html) for more details. The Enclave Code Hash can be found printed during
the build process as below:

`Enclave code hash:   DB2AF8DD327D18965D50932E08BE4CB663436162CB7641269A4E611FC0956C5F`, In this case the
hash `DB2AF8DD327D18965D50932E08BE4CB663436162CB7641269A4E611FC0956C5F` is the enclave constraint.

Here we are running the sample in Simulation mode, so the "SEC" key in constraint string would be `INSECURE`. Change
this to the `STALE` or `SECURITY` as applicable when you are running in hardware mode.

Ultimately pass your enclave constraint as argument. Further, pass your dataset in arguments as space separated values.
Here user data is being passed , which includes the following fields:
Name : String Age : Integer Country : String Gender : String(m,f)

To find the frequency distribution run the below command:
`./gradlew client:run --args="C:DB2AF8DD327D18965D50932E08BE4CB663436162CB7641269A4E611FC0956C5F SEC:INSECURE John 45 USA m Sera 45 India f Jacod 20 USA m Emily 20 UK na" --info`

The enclave should reply with a response message indicating the freq distribution of age, country and gender. The
response will look like below:
`Enclave gave us the answer 'Age Frequency Distribution: {20=2, 45=2} Country Frequency Distribution: {USA=2, UK=1, India=1} Gender Frequency Distribution: {na=1, f=1, m=2}'`

The example could further be extended to include complex data fields and to perform other data operations. Though this
example demonstrates the use of Conclave for data analysis by a single organization, Conclave is equally well suited for
multi-party computation and collaboration.