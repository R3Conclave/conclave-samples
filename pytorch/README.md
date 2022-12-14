# PyTorch sample

This sample shows how to write an enclave in Python, in particular to how integrate PyTorch to create enclaves which 
can do machine learning (ML). We are using [torchvision](https://pytorch.org/vision/stable/index.html) to do image 
classification using a pre-trained model.

**Note, Python support is a work in progress feature, which is only available on the latest master build of the
Conclave SDK. This is not ready for production yet!**

## Prerequisites

Several packages are required to run this sample, including Java and Python:

```bash
sudo curl -fsSLo /usr/share/keyrings/gramine-keyring.gpg https://packages.gramineproject.io/gramine-keyring.gpg
echo 'deb [arch=amd64 signed-by=/usr/share/keyrings/gramine-keyring.gpg] https://packages.gramineproject.io/ focal main' | sudo tee /etc/apt/sources.list.d/gramine.list

wget -qO - https://download.01.org/intel-sgx/sgx_repo/ubuntu/intel-sgx-deb.key | sudo apt-key add -
echo 'deb [arch=amd64] https://download.01.org/intel-sgx/sgx_repo/ubuntu focal main' | sudo tee /etc/apt/sources.list.d/intel-sgx.list

wget -qO - https://packages.microsoft.com/keys/microsoft.asc | sudo apt-key add -
echo 'deb [arch=amd64] https://packages.microsoft.com/ubuntu/20.04/prod focal main' | sudo tee /etc/apt/sources.list.d/msprod.list

sudo apt-get update
```

```bash
sudo apt-get install openjdk-17-jdk-headless python3-pip gramine libsgx-quote-ex libsgx-dcap-ql az-dcap-client
```

You will need the `JAVA_HOME` environment variable pointing to the Java installation directory. If this isn't 
already set then run:

```bash
export JAVA_HOME=$(readlink -f /usr/bin/java | sed "s:/bin/java::")
```

```bash
pip3 install jep torch torchvision torchaudio --extra-index-url https://download.pytorch.org/whl/cpu
```

## Architecture

There are three actors in this sample:

1. The enclave. This will have the image classification model provisioned into it, and then users can [securely](https://docs.conclave.net/mail.html)
   submit images to have their images classified.
2. The ML model owner. This is a client of the enclave who owns the private ML model and wishes that it only be used 
   in an enclave environment to ensure it's kept confidential, both from the operator of the enclave and the other 
   users.
3. Image submitters. This is the second category of enclave clients. They will submit their images to the ML enclave 
   for classification. Neither the enclave operator nor the ML model owner will see the images submitted for 
   classification

The enclave is represented by the [`enclave`](enclave) and [`host`](host) modules, with the former containing the 
actual [Python code](enclave/src/main/python/enclave.py), and the later a thin Sping Boot wrapper for communicating 
with the client.

The two client actors are represented by a single [`client`](client) application, which is written in Kotlin. This 
application runs in one of two modes, `provision` and `classify`, representing the ML model owner and image 
submitters respectively.

## Running the enclave

To build the enclave/host in simulation mode:

```bash
./gradlew host:bootJar -PenclaveMode=simulation
```

To run the enclave:
```bash
java -jar host/build/libs/host-simulation.jar
```

Once the enclave has loaded you will see ouput similar to this:

```
2022-12-13 11:20:44.592  INFO 11346 --- [           main] c.r.c.host.web.EnclaveWebController      : Enclave com.r3.conclave.python.PythonEnclaveAdapter started
2022-12-13 11:20:44.602  INFO 11346 --- [           main] c.r.c.host.web.EnclaveWebController      : Remote attestation for enclave FA640B535EF2158B345956976A4E55A29C88A93E7F0B5E9DD512D0457A3693BE:
  - Mode: SIMULATION
  - Code signer: A322D25EFD20BE8C9C07690AF7A2EA565CE2FA06BB7DF733E2119EE222D4B81F
  - Session signing key: 302A300506032B6570032100E7955625898FBF5EFCD9794CFA42672722F8A52195A06C65BB8AA44432DE60A7
  - Session encryption key: DDD4CA639AAD4DAD33A60187FB5152A0600A472B592844F80B3E316B2EDC3E7D
  - Product ID: 1
  - Revocation level: 0

Assessed security level at 2022-12-13T11:20:37.470920290Z is INSECURE
  - Enclave is running in simulation mode.
2022-12-13 11:20:44.790  INFO 11346 --- [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port(s): 8080 (http) with context path ''
2022-12-13 11:20:44.800  INFO 11346 --- [           main] c.r.c.host.web.EnclaveWebHost$Companion  : Started EnclaveWebHost.Companion in 9.288 seconds (JVM running for 9.555)
```

## Running the client

To build the client application:

```bash
./gradlew client:shadowJar
```

### ML model owner

To run as the ML model owner and provision the enclave with the private image classification model, run with the 
`provision` command:

```bash
java -jar client/build/libs/client.jar \
    "http://localhost:8080" \
    "C:FA640B535EF2158B345956976A4E55A29C88A93E7F0B5E9DD512D0457A3693BE SEC:INSECURE" \ 
    provision \
    ~/alexnet-pretrained.pt \
    classes.txt
```

Let's explain the arguments to this command:

1. URL to the Spring Boot webserver running the enclave. In this case we are running the client from the same 
   machine as the enclave and so use the loopback address.
2. This is the [enclave constraints](https://docs.conclave.net/constraints.html) which confirms to the client that 
   it is actually connecting to the correct image classification enclave, and not just something that's pretending to be 
   one. It also ensures the machine the enclave is running on is patched with the latest security updates. In this 
   example we're using the code hash of the enclave, which you can pick up from build output of the enclave.

   _Note, the code hash may end up being different, so you will need to use the value from your build output._
3. The `provision` command
4. Path to the pre-trained image classification model. In this example we are using the [AlexNet](https://www.wikiwand.com/en/AlexNet)
   model, which can be downloaded to your home directory with the following Python script:
   ```python
   from torchvision import models
   import torch
   from pathlib import Path
   
   alexnet = models.alexnet(pretrained=True)
   torch.save(alexnet, f"{Path.home()}/alexnet-pretrained.pt")
   ```
5. Path to the classes (or labels) for the model, which are available in this [sample](classes.txt).

Provisioning the model into the enclave may take a short while since it's around 230MB in size.

### Image submitter

Once the enclave has been provisioned, other users can now confidentially submit their images to be classified:

```bash
java -jar client/build/libs/client.jar "http://localhost:8080" "C:FA640B535EF2158B345956976A4E55A29C88A93E7F0B5E9DD512D0457A3693BE SEC:INSECURE" classify
```

This will bring up a simple console where can specify local files or URLs of images. These will be securely sent to 
the enclave to be classified, which will respond back with the closest matched label.

```
Enter path or URL of image: https://upload.wikimedia.org/wikipedia/commons/6/68/Orange_tabby_cat_sitting_on_fallen_leaves-Hisashi-01A.jpg
tiger cat (74.9%)
Enter path or URL of image:
```

### Release mode

The above example used simulation mode, which is insecure, but lets you test the enclave on a non-SGX machine. To 
use the full privacy of SGX, build and run the enclave in [release mode](https://docs.conclave.net/enclave-modes.html):

```bash
./gradlew host:bootJar -PenclaveMode=release
java -jar host/build/libs/host-release.jar
```

## Next steps

You can find out more information on the Python API [here](https://github.com/R3Conclave/conclave-core-sdk#python-support-work-in-progress),
if you wish to create your own Python enclave. As this is a work-in-progress feature, there are some limitations 
which are documented. Also remember the API isn't stable yet and is not ready for production.
