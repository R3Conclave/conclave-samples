package com.r3.conclave.sample.client;

import com.r3.conclave.client.EnclaveClient;
import com.r3.conclave.client.web.WebEnclaveTransport;
import com.r3.conclave.common.EnclaveConstraint;
import com.r3.conclave.mail.EnclaveMail;
import com.r3.conclave.sample.common.*;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Client can be of two types: Merchant or Service Provider
 * Merchant - who supplies list of users who have made a purchase
 * Service Providers - who supplies a list of users who have clicked on the ad
 * Both the clients send the lists to enclave, which calculates the ad conversion rate and sends it back to the clients
 */
@CommandLine.Command(name = "psi-client",
        mixinStandardHelpOptions = true,
        description = "Simple client that communicates with the PSIEnclave using the web host.")
public class Client implements Callable<Void> {

    @CommandLine.Option(names = {"-r", "--role"}
            , description = "Role Options: ${MERCHANT/SERVICE_PROVIDER}")
    private CommandType commandType = null;

    //Use picocli to provide command line parameters
    @CommandLine.Parameters(description = "Users username and password")
    private final List<String> userDetails = new ArrayList<>();

    @CommandLine.Option(names = {"-u", "--url"},
            required = true,
            description = "URL of the web host running the enclave.")
    private String url;

    @CommandLine.Option(names = {"-c", "--constraint"},
            required = true,
            description = "Enclave constraint which determines the enclave's identity and whether it's acceptable to use.",
            converter = EnclaveConstraintConverter.class)
    private EnclaveConstraint constraint;

    @Override
    public Void call() throws Exception {
        /*A new private key is generated. Enclave Client is created using this private key and constraint.
        A corresponding public key will be used by the enclave to encrypt data to be sent to this client*/
        EnclaveClient enclaveClient = new EnclaveClient(constraint);

        try (WebEnclaveTransport transport = new WebEnclaveTransport(url);
             EnclaveClient client = enclaveClient) {
            /*Retrieve the enclaveInstanceInfo object, i.e. the remote attestation object and verify it against the
            constraint*/
            client.start(transport);

            //Collect merchants and service providers credit card numbers list
            InputData inputData = getInputData();

            byte[] requestMailBody = serialize(inputData);

            //Client will send its credit card list to enclave and wait for other client to send their list
            EnclaveMail responseMail = client.sendMail(requestMailBody);

            //ResponseMail is null till enclave doesn't reply back to the client
            if (responseMail == null) {
                do {
                    Thread.sleep(2000);
                    //Poll for reply to enclave
                    responseMail = enclaveClient.pollMail();
                } while (responseMail == null);
            }
            System.out.println("Enclave Reply is : " + new String(responseMail.getBodyAsBytes()));
        }
        return null;
    }

    private InputData getInputData() {
        InputData inputData = new InputData();
        inputData.setCommandType(commandType);
        System.out.println(userDetails.size() + "userDetails.size()");
        inputData.setUser(new User(userDetails.get(0), userDetails.get(1)));
        return inputData;
    }

    public static byte[] serialize(Object obj) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream os;
        try {
            os = new ObjectOutputStream(out);
            os.writeObject(obj);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return out.toByteArray();
    }

    private static class EnclaveConstraintConverter implements CommandLine.ITypeConverter<EnclaveConstraint> {
        @Override
        public EnclaveConstraint convert(String value) {
            return EnclaveConstraint.parse(value);
        }
    }

    public static void main(String... args) {
        int exitCode = new CommandLine(new Client()).execute(args);
        System.exit(exitCode);
    }
}
