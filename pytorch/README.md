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

To build the enclave/host:

```bash
./gradlew host:bootJar -PenclaveMode=release
```

This assumes you have an [SGX enabled machine](https://docs.conclave.net/machine-setup.html). If you do not then you 
can build in simulation mode instead by replacing `release` with `simulation`.

To run the enclave:
```bash
java -jar host/build/libs/host-release.jar
```

It may take a while for the enclave to load. Once it has you will see ouput similar to this:

```
2022-12-09 16:45:18.935  INFO 14188 --- [           main] c.r.c.host.web.EnclaveWebController      : Enclave com.r3.conclave.python.PythonEnclaveAdapter started
2022-12-09 16:45:18.944  INFO 14188 --- [           main] c.r.c.host.web.EnclaveWebController      : Remote attestation for enclave C21AD17F2C6F80D8F2A8E6F4FA2F62F93C8DFFAD89FEB657A81FF682FA120993:
  - Mode: RELEASE
  - Code signer: 12241F3E985F814961F4873BBB2497665A5E7EE17FE7A3230217B6D081BEE9E7
  - Session signing key: 302A300506032B657003210011D3BA3089CE2ACF63CBCD01881ED6B018334483492228693512DDDA1452894E
  - Session encryption key: B7956A10E03592840B5EFADEEE1ABA459A7088BC8E5D02C11797516B3501AF3A
  - Product ID: 1
  - Revocation level: 0

Assessed security level at 2022-12-05T03:02:22Z is SECURE
  - A signature of the ISV enclave QUOTE was verified correctly and the TCB level of the SGX platform is up-to-date.
2022-12-09 16:45:19.137  INFO 14188 --- [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port(s): 8080 (http) with context path ''
2022-12-09 16:45:19.144  INFO 14188 --- [           main] c.r.c.host.web.EnclaveWebHost$Companion  : Started EnclaveWebHost.Companion in 72.93 seconds (JVM running for 73.191)
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
    "C:C21AD17F2C6F80D8F2A8E6F4FA2F62F93C8DFFAD89FEB657A81FF682FA120993" \ 
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
java -jar client/build/libs/client.jar "http://localhost:8080" "C:C21AD17F2C6F80D8F2A8E6F4FA2F62F93C8DFFAD89FEB657A81FF682FA120993" classify
```

This will bring up a simple console where can specify local files or URLs of images. These will be securely sent to 
the enclave to be classified, which will respond back with the closest matched label.

```
Enter path or URL of image: https://upload.wikimedia.org/wikipedia/commons/6/68/Orange_tabby_cat_sitting_on_fallen_leaves-Hisashi-01A.jpg
tiger cat (74.9%)
Enter path or URL of image:
```

## Next steps

You can find out more information on the Python API [here](https://github.com/R3Conclave/conclave-core-sdk#python-support-work-in-progress),
if you wish to create your own Python enclave. As this is a work-in-progress feature, there are some limitations 
which are documented. Also remember the API isn't stable yet and is not ready for production.
