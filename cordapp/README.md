# CorDapp Sample

This is a simple [CorDapp](https://docs.r3.com/en/platform/corda/4.8/open-source/cordapp-overview.html) written in Java using the Conclave API. It is licensed under the Apache 2 license. So, you
can copy/paste it to act as the basis of your commercial or open source apps.

This CorDapp sample builds on the [hello-world](https://docs.conclave.net/writing-hello-world.html) sample. It reverses a string using two [nodes](https://docs.r3.com/en/platform/corda/4.8/enterprise/node/component-topology.html). One of these nodes is used to load the enclave.

This sample does not use smart contracts. It requires only flows.

The sample divides the code into several parts. You can copy/paste the packages that _don't_ have `samples` in the name.
The code expects a DCAP-capable host.

*Note:
To understand this tutorial, you should read both the [Conclave Hello World tutorial](https://docs.conclave.net/writing-hello-world.html)
and [the Corda tutorials](https://docs.corda.net/docs/corda-os/4.7/hello-world-introduction.html).*

## How to run the sample CorDapp in different environments

Unlike most CorDapps, this sample will *only run on Linux* due to the need for an enclave. However, you can use
virtualization to run it on Windows and macOS. For Windows and macOS, you need to set up a docker container.

### Linux

1. Download and unpack JDK 8 and set `JAVA_HOME` environment variable to point to it.
2. Run `./gradlew workflows:test`

### macOS

1. Download and install Docker Desktop. Ensure that it is running.
2. Download and unpack JDK 11 (LTS) and set `JAVA_HOME` environment variable to point to it. If you're already using
   versions 8 to 12, you can skip this step.
3. Create and enter the Linux execution environment:
   ```
   ./gradlew enclave:setupLinuxExecEnvironment
   docker run -it --rm -v ${HOME}:/home/${USER} -w /home/${USER} conclave-build /bin/bash
   ```
   This will mount your home directory and give you a Linux shell.
4. Run the following command to set up JDK 8 inside the Linux environment:
   ```
   apt-get -y update && apt-get install -y openjdk-8-jdk-headless && export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
   ```
5. Change directory to your project and run the project.
    ```
    cd <project-directory>
    ./gradlew workflows:test
    ```

### Windows

1. Download and install Docker Desktop. Ensure that it is running.
2. Download and unpack JDK 11 (LTS) and set `JAVA_HOME` environment variable to point to it. If you're already using
   versions 8 to 12, you can skip this step.
3. Create and enter the Linux execution environment:
    ```
    .\gradlew.bat enclave:setupLinuxExecEnvironment
    docker run -it --rm -v ${HOME}:/home/${env:UserName} -w /home/${env:UserName} conclave-build /bin/bash
    ```
    This will mount your user directory and give you a Linux shell.
4. Run the following command to set up JDK 8 inside the Linux environment:
   ```
   apt-get -y update && apt-get install -y openjdk-8-jdk-headless && export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
   ```
5. Change directory to your project and run the project.
    ```
    cd <project-directory>
    ./gradlew workflows:test
    ```

Alternatively, Ubuntu 20.04 via WSL 2 ([Windows Subsystem for Linux 2](https://docs.microsoft.com/en-us/windows/wsl/install))
may also prove to work for you, though this has not been extensively tested.

For an explanation of the Docker command used above, see
[the tutorials](https://docs.conclave.net/running-hello-world.html#appendix-summary-of-docker-command-options).

## Corda Node Identity Validation
The [certificates](certificates) folder contains the truststore.jks Java KeyStore that has the Corda Root
Certificate Authority (CA) public key. This public key can be used for development or testing purposes. The public key also creates
the certificate *trustedroot.cer*, embedded as a resource in the enclave. This certificate is used to validate a Corda node's identity
when the host relays a message to the enclave.

To learn more about root certificates in the Corda network, please refer to
[the Corda documentation](https://docs.r3.com/en/platform/corda/4.8/open-source/permissioning.html).

You can find the public Corda Network Root Certificate [here](https://trust.corda.network/).

### Usage
Use the shell script `dmp-cordarootca.sh` to dump the Root CA certificate. Then, copy and paste the
output to the cordapp/enclave/src/main/resources/trustedroot.cer.

*Note that this has been already
done for you, and is reported here only for documentation purpose*.

```shell
cordapp/certificates> ./dump-cordarootca.sh
-----BEGIN CERTIFICATE-----
MIICCTCCAbCgAwIBAgIIcFe0qctqSucwCgYIKoZIzj0EAwIwWDEbMBkGA1UEAwwS
Q29yZGEgTm9kZSBSb290IENBMQswCQYDVQQKDAJSMzEOMAwGA1UECwwFY29yZGEx
DzANBgNVBAcMBkxvbmRvbjELMAkGA1UEBhMCVUswHhcNMTcwNTIyMDAwMDAwWhcN
MjcwNTIwMDAwMDAwWjBYMRswGQYDVQQDDBJDb3JkYSBOb2RlIFJvb3QgQ0ExCzAJ
BgNVBAoMAlIzMQ4wDAYDVQQLDAVjb3JkYTEPMA0GA1UEBwwGTG9uZG9uMQswCQYD
VQQGEwJVSzBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABGlm6LFHrVkzfuUHin36
Jrm1aUMarX/NUZXw8n8gSiJmsZPlUEplJ+f/lzZMky5EZPTtCciG34pnOP0eiMd/
JTCjZDBiMB0GA1UdDgQWBBR8rqnfuUgBKxOJC5rmRYUcORcHczALBgNVHQ8EBAMC
AYYwIwYDVR0lBBwwGgYIKwYBBQUHAwEGCCsGAQUFBwMCBgRVHSUAMA8GA1UdEwEB
/wQFMAMBAf8wCgYIKoZIzj0EAwIDRwAwRAIgDaL4SguKsNeTT7SeUkFdoCBACeG8
GqO4M1KlfimphQwCICiq00hDanT5W8bTLqE7GIGuplf/O8AABlpWrUg6uiUB
-----END CERTIFICATE-----
```

### Note on Conclave modes
By default, this sample will build and run in [mock mode](https://docs.conclave.net/mockmode.html), so it won't use a
secure enclave. For a list of Conclave modes and their properties, see [here](https://docs.conclave.net/enclave-modes.html).
For instructions on how to set the mode at build time, see [here](https://docs.conclave.net/running-hello-world.html#beyond-mock-mode).

## Configure your workflow CorDapp module

Both Conclave and Corda rely on Gradle build system plugins. Follow the instructions in the
[hello world tutorial](https://docs.conclave.net/writing-hello-world.html) to configure Gradle to include an enclave mode. Add the host libraries
to your Corda workflows module:

```
dependencies {
    compile project(path: ":enclave", configuration: mode)
    compile "com.r3.conclave:conclave-host:$conclaveVersion"
    compile "com.r3.conclave:conclave-client:$conclaveVersion"
    compile "com.r3.conclave:conclave-common:$conclaveVersion"
    compile "com.r3.conclave:conclave-mail:$conclaveVersion"

    // Corda dependencies.
    cordaCompile "$corda_release_group:corda-core:$corda_release_version"
    cordaRuntime "$corda_release_group:corda:$corda_release_version"
    testCompile "$corda_release_group:corda-node-driver:$corda_release_version"
}
```

*Note:
You must use Gradle's `compile` configurations for the host dependencies. If you use `implementation`, you
will get errors when the node starts up about missing classes due to issues with fat JARing. The host `dependencies` section should look like the above snippet.*

## Write an enclave host service

Conclave loads the enclave into a service object. This singleton will be available to flows for later usage, and lets
us integrate with the node's lifecycle. The sample project contains a helper class called `EnclaveHostService` that
you can copy into your project and subclass. You can edit this helper class to make it work for your use case.

```java
@CordaService
public class ReverseEnclaveService extends EnclaveHostService {
    public ReverseEnclaveService(@NotNull AppServiceHub serviceHub) {
        super("com.r3.conclave.cordapp.sample.enclave.ReverseEnclave");
    }
}
```

This will enable SGX support on the host. Then, it loads the sample `ReverseEnclave` class. The `EnclaveHostService` class exposes methods to
send and receive mail with the enclave, and suspends flows waiting for the enclave to deliver mail.

In the next section about relaying a mail from a flow to the enclave, Conclave uses
the above class as a parameter to initiate the responder flow that ensures the host service is started.

## Relaying mail from a flow to the enclave

You can see how to [create a new subclass of enclave](https://docs.conclave.net/writing-hello-world.html#create-a-new-subclass-of-enclave) and how to
[receive and post mail in the enclave](https://docs.conclave.net/writing-hello-world.html#sending-and-receiving-mail) in the
[hello-world](https://docs.conclave.net/writing-hello-world.html#sending-and-receiving-mail) tutorial. Conclave has wrapped some of this boilerplate into an API to simplify setting up secure flows and exchanging secure messages between parties.

In this tutorial, reversing a string involves two parties: one is the initiator that sends the secret string to reverse, and
the other is the responder that reverses the string inside the enclave. To implement this, you will need an *initiator* flow used by clients and a *responder* flow used by the host node.

### The Responder Flow
The responder flow to get the enclave host service up and running is as follows:

1. Get the enclave attestation and send it to the other party for verification.
2. Get, verify, and acknowledge the other party's encrypted identity.
3. Get the other party's encrypted mail with the string to reverse.
4. Send the string and the encrypted mail to the enclave.
5. Retrieve the enclave-encrypted mail to send to the other party for decryption.

```java
// We start with a few lines of boilerplate: read the Corda tutorials to know more about it.
@InitiatedBy(ReverseFlow.class)
public class ReverseFlowResponder extends FlowLogic<Void> {

    // private variable
    private final FlowSession counterpartySession;

    // Constructor
    public ReverseFlowResponder(FlowSession counterpartySession) {
        this.counterpartySession = counterpartySession;
    }

    @Suspendable
    @Override
    public Void call() throws FlowException {

        EnclaveFlowResponder session =
                EnclaveClientHelper.initiateResponderFlow(this, counterpartySession, ReverseEnclaveService.class);

        session.relayMessageToFromEnclave();

        return null;
    }
}
```

You can initiate the responder's flow by starting a specific instance of the
`EnclaveHostService` and sending the remote attestation. All interactions with the enclave should start
this way, although the attestation may be cached.

The flow responder expects to receive an identity during the initialization. You can send an empty identity message if the party prefers to remain anonymous.

```kotlin
@Suspendable
@Throws(FlowException::class)
@JvmStatic
@JvmOverloads
fun <T : EnclaveHostService> initiateResponderFlow(flow: FlowLogic<*>,
                                                   counterPartySession: FlowSession,
                                                   serviceType: Class<T>): EnclaveFlowResponder {
    // Start an instance of the enclave hosting service
    val host = flow.serviceHub.cordaService(serviceType)
    // Send the other party the enclave identity (remote attestation) for verification.
    counterPartySession.send(host.attestationBytes)
    val instance = EnclaveFlowResponder(flow, counterPartySession, host)
    // Relay the initial identity message to the enclave and relay the response back
    instance.relayMessageToFromEnclave()
    return instance
}
```

The `EnclaveFlowResponder` implements a request/response protocol that receives an encrypted byte array
and uses the `deliverAndPickUpMail` method of the `EnclaveHostService` class. This returns an operation that can be passed
to `await`. The flow will suspend and free up its thread, potentially for an extended period. The enclave need not reply immediately.
It can return from processing the delivered mail without replying. When the enclave replies, the flow will be re-awakened, and the encrypted mail will be returned to the other side.

```kotlin
@Suspendable
@Throws(FlowException::class)
fun relayMessageToFromEnclave() {
    // Other party sends us an encrypted mail.
    val encryptedMail = session.receive(ByteArray::class.java).unwrap { it }
    // Deliver and wait for the enclave to reply. The flow will suspend until the enclave chooses to deliver a mail
    // to this flow, which might not be immediately.
    val encryptedReply: ByteArray = flow.await(host.deliverAndPickUpMail(flow, encryptedMail))
    // Send back to the other party the encrypted enclave's reply
    session.send(encryptedReply)
}
```

*Note:
This sample code does not handle node restarts, although the Corda flow framework has built-in support for it.*

### The Initiator Flow

The initiator flow that starts a session with the responder party is as follows:

1. Get the enclave attestation and validate it.
2. Build and send a verifiable identity for the enclave to validate.
3. Send the encrypted mail with the string to reverse.
4. Read and decrypt the enclave's response. Remember that a verifiable identity is only sent to the enclave if the `anonymous`
property is set to false.

```java
@InitiatingFlow
@StartableByRPC
public class ReverseFlow extends FlowLogic<String> {
    private final Party receiver;
    private final String message;
    private final String constraint;
    private final Boolean anonymous;

    public ReverseFlow(Party receiver, String message, String constraint) {
        this(receiver, message, constraint, false);
    }

    public ReverseFlow(Party receiver, String message, String constraint, Boolean anonymous) {
        this.receiver = receiver;
        this.message = message;
        this.constraint = constraint;
        this.anonymous = anonymous;
    }

    @Override
    @Suspendable
    public String call() throws FlowException {
        EnclaveFlowInitiator session = EnclaveClientHelper.initiateFlow(this, receiver, constraint, anonymous);

        byte[] response = session.sendAndReceive(message.getBytes(StandardCharsets.UTF_8));

        return new String(response);
    }
}
```

After establishing a session with the hosting node, the flow waits for an [`EnclaveInstanceInfo`](api/-conclave/com.r3.conclave.common/-enclave-instance-info/index.html) to verify it
against the constraint passed. If the enclave verifies successfully
and doesn't throw an exception, the flow sends the initiator party identity to the enclave for
[authentication](writing-cordapps.md#authenticating-senders-identity). The last step is applicable only if the party wishes to
share its identity. The flow doesn't send any identity information if the `anonymous` parameter is set to `true`.



```kotlin
@Suspendable
@Throws(FlowException::class)
@JvmStatic
@JvmOverloads
fun initiateFlow(flow: FlowLogic<*>, receiver: Party, constraint: String,
                 anonymous: Boolean = false): EnclaveFlowInitiator {
    val session = flow.initiateFlow(receiver)

    // Read the enclave attestation from the peer.
    val attestation = session.receive(ByteArray::class.java).unwrap { from: ByteArray ->
        EnclaveInstanceInfo.deserialize(from)
    }

    // The key hash below (the hex string after 'S') is the public key version of sample_private_key.pem
    // In a real app you should remove the SEC:INSECURE part, of course.
    try {
        EnclaveConstraint.parse(constraint).check(attestation)
    } catch (e: InvalidEnclaveException) {
        throw FlowException(e)
    }
    val instance = EnclaveFlowInitiator(flow, session, attestation)
    instance.sendIdentityToEnclave(anonymous)

    return instance
}
```

After verifying the enclave and authenticating the identity (if the party shares its identity),
the flow can securely exchange encrypted messages with the enclave through the `EnclaveFlowInitiator` instance.
This instance implements a send/receive API that encrypts and decrypts the outgoing and incoming data in the form of byte arrays.
The send and receive methods use the [`PostOffice`](api/-conclave/com.r3.conclave.mail/-post-office/index.html) API. The
`EnclaveFlowInitiator` class holds one `PostOffice` instance which has the topic set to the session's flow id.

```kotlin
@Suspendable
@Throws(FlowException::class)
fun sendAndReceive(messageBytes: ByteArray): ByteArray {
    sendToEnclave(messageBytes)
    return receiveFromEnclave()
}

@Suspendable
@Throws(FlowException::class)
private fun sendToEnclave(messageBytes: ByteArray) {
    val encryptedMail = postOffice.encryptMail(messageBytes)
    session.send(encryptedMail)
}

@Suspendable
@Throws(FlowException::class)
fun receiveFromEnclave(): ByteArray {
    val reply: EnclaveMail = session.receive(ByteArray::class.java).unwrap { mail: ByteArray ->
        try {
            postOffice.decryptMail(mail)
        } catch (e: IOException) {
            throw FlowException("Unable to decrypt mail from Enclave", e)
        }
    }
    return reply.bodyAsBytes
}
```

# Authenticating the sender's identity

The enclave can authenticate a sender's identity that belongs to a network where nodes are identified by a X.509
certificate, and the network is controlled by a certificate authority like in a Corda network.

The network CA root certificate's public key must be stored in the enclave's resource folder (in this example the
path is enclave/src/main/resources/) in a file named trustedroot.cer, so that the enclave can validate the sender
identity.

The identity is set up by the `EnclaveFlowInitiator` class during authentication and sent to the enclave which
verifies it. Please be aware that the first byte of the identity message indicates whether a party wants to remain
anonymous or not. If a party decides to remain anonymous, the identity message is padded with zeros to prevent
anyone in the middle from using statistical analysis to guess whether a party is anonymous or not.
The remaining bytes represent the party's identity If the party decides to authenticate
itself.

```kotlin
@Suspendable
private fun buildMailerIdentity(): SenderIdentityImpl {
    val sharedSecret = encryptionKey.publicKey.encoded
    val signerPublicKey = flow.ourIdentity.owningKey
    val signature = flow.serviceHub.keyManagementService.sign(sharedSecret, signerPublicKey).withoutKey()
    val signerCertPath = flow.ourIdentityAndCert.certPath
    return SenderIdentityImpl(signerCertPath, signature.bytes)
}

@Suspendable
@Throws(FlowException::class)
fun sendIdentityToEnclave(isAnonymous: Boolean) {

    val serializedIdentity = getSerializedIdentity(isAnonymous)
    sendToEnclave(serializedIdentity)

    val mail: EnclaveMail = session.receive(ByteArray::class.java).unwrap { mail: ByteArray ->
        try {
            postOffice.decryptMail(mail)
        } catch (e: IOException) {
            throw FlowException("Unable to decrypt mail from Enclave", e)
        }
    }

    if (!mail.topic.contentEquals("$flowTopic-ack"))
        throw FlowException("The enclave could not validate the identity sent")
}
```

When the enclave successfully validates the identity, it stores it in a key-based cache. Subsequent messages
from the same sender in the same session are paired with the cached identity which is then available from
within the `receiveMail` method through the extra parameter called `identity`. This identity can be used to uniquely identify a sender if the user is not anonymous.

```java
@Override
protected void receiveMail(long id, EnclaveMail mail, String routingHint, SenderIdentity identity) {
    String reversedString = reverse(new String(mail.getBodyAsBytes()));

    String responseString;
    if (identity == null) {
        responseString = String.format("Reversed string: %s; Sender name: <Anonymous>", reversedString);
    } else {
        responseString = String.format("Reversed string: %s; Sender name: %s", reversedString, identity.getName());
    }

    // Get the PostOffice instance for responding back to this mail. Our response will use the same topic.
    final EnclavePostOffice postOffice = postOffice(mail);
    // Create the encrypted response and send it back to the sender.
    final byte[] reply = postOffice.encryptMail(responseString.getBytes(StandardCharsets.UTF_8));
    postMail(reply, routingHint);
}
```

The method `receiveMail` contains the enclave logic. You can code your business requirements in the `receiveMail method`.
In the example above, the enclave reverses a string and returns the result back with the sender name if the party is not anonymous. The mail parameter contains some metadata, and the data which is going to be processed by the enclave.
The call [`mail.getBodyAsBytes()`](https://docs.conclave.net/api/-conclave/com.r3.conclave.mail/-enclave-mail/index.html#-2025980493%2FFunctions%2F-654294413) returns
the data to be processed by the enclave. As mentioned before, the `identity`
parameter object contains the identity of the sender if the sender is not anonymous. It is set to null if the sender is anonymous.

*Note:
The `receiveMail` method has an extra identity parameter in addition to the parameters in a regular enclave. You can check how this works in the [CordaEnclave class](https://github.com/R3Conclave/conclave-samples/blob/master/cordapp/enclave/src/main/kotlin/com/r3/conclave/cordapp/sample/enclave/CordaEnclave.kt).*
