## Conclave Column Profiling Sample


<p align="center">
  <img src="https://conclave.net/wp-content/uploads/2020/12/Conclave_logo_master.png" alt="Conclave" width="500">
</p>
<br>

This is a sample use case for R3's Conclave Confidential Computing Platform.
It does column profiling on the dataset and returns the freq distribution as an output.
The application takes in User fields: name, age, country and gender. It returns the number of occurrence of each distinct values in age, gender and country.


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
To run the client, you will need the Enclave constraint.
The enclave constraint can be found printed during the build process as below:

`Enclave code hash:   DB2AF8DD327D18965D50932E08BE4CB663436162CB7641269A4E611FC0956C5F`, In this
case the hash `DB2AF8DD327D18965D50932E08BE4CB663436162CB7641269A4E611FC0956C5F` is the enclave constraint.

Pass your dataset in arguments as space separated values. Here user data is being passed , which includes the following fields:
Name : String
Age : Integer
Country : String
Gender : String(m,f)

To find the freq distribution run the below command:
`./gradlew runClient --args="DB2AF8DD327D18965D50932E08BE4CB663436162CB7641269A4E611FC0956C5F John 45 USA m Sera 45 India f Jacod 20 USA m Emily 20 UK na" --info`

The enclave should reply with a response message indicating the freq distribution of age, country and gender.
The enclave should reply with a response message indicating the freq distribution of age, country and gender.
The response will look like below:
`Enclave gave us the answer 'Age Frequency Distribution: {20:2}{45:2} Country Frequency Distribution: {USA:2}{UK:1}{India:1} Gender Frequency Distribution: {na:1}{f:1}{m:1}'`

The example could further be extended to include complex data fields and to perform other data operations. 
Though this example demonstrates the use of Conclave for data analysis by a single organization, Conclave is equally well suited for multi-party computation and collaboration.