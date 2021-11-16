package com.r3.conclave.sample.client;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;
import com.r3.conclave.client.EnclaveClient;
import com.r3.conclave.client.web.WebEnclaveTransport;
import com.r3.conclave.common.EnclaveConstraint;
import com.r3.conclave.mail.EnclaveMail;
import com.r3.conclave.sample.common.*;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
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
    Role role = null;

    //use picocli to provide command line parameters
    @CommandLine.Parameters( description = "List of credit card numbers to be sent to enclave")
        private List<String> creditCardNumbers = new ArrayList<String>();

    @CommandLine.Option(names = {"-u", "--url"},
                required = true,
                description = "URL of the web host running the enclave.")
        private String url;

    @CommandLine.Option(names = {"-c", "--constraint"},
                required = true,
                description = "Enclave constraint which determines the enclave's identity and whether it's acceptable to use.",
                converter = EnclaveConstraintConverter.class)
    private EnclaveConstraint constraint;

    /**
     * Use Kryo to serialize inputs from client to enclave
     */
    private Output serializeMessage(InputData listOfCreditCardNumbers){
        Kryo kryo = new Kryo();
        Output output = new Output(new ByteArrayOutputStream());
        kryo.register(InputData.class, new InputDataSerializer());
        kryo.writeObject(output, listOfCreditCardNumbers);
        output.close();
        return output;
    }

    @Override
    public Void call() throws Exception {
        //a new private key is generated. Enclave Client is created using this private key and constraint.
        //a corresponding public key will be used by the enclave to encrypt data to be sent to this client
        EnclaveClient enclaveClient = new EnclaveClient(constraint);

        try (WebEnclaveTransport transport = new WebEnclaveTransport(url);
             EnclaveClient client = enclaveClient)
        {
            //retrieve the enclaveInstanceInfo object, i.e. the remote attestation object and verify it against the
            //constraint
            client.start(transport);

            //collect merchants and service providers credit card numbers list
            InputData inputData = getInputData();

            byte[] requestMailBody = serializeMessage(inputData).getBuffer();

            //client will send its credit card list to enclave and wait for other client to send their list
            EnclaveMail responseMail = client.sendMail(requestMailBody);

            //responseMail is null till enclave doesn't reply back to the client
            if(responseMail == null) {
                do {
                    Thread.sleep(2000);
                    //poll for reply to enclave
                    responseMail = enclaveClient.pollMail();
                } while (responseMail==null);
            }
            System.out.println("Ad Conversion Rate is : " + new String(responseMail.getBodyAsBytes()));
        }
        return null;
    }

    private InputData getInputData() {
        InputData inputData = new InputData();
        List<UserDetails> userDetailsList = null;
        List<AdDetails> adDetailsList = null;

        inputData.setClientType(role.name());

        if (Role.MERCHANT.name().equals(role.name())) {
            userDetailsList = new ArrayList(creditCardNumbers.size());
            for (int i = 0; i < creditCardNumbers.size(); i++) {
                UserDetails userDetails = new UserDetails(creditCardNumbers.get(i));
                userDetailsList.add(userDetails);
            }
            inputData.setUserDetailsList(userDetailsList);

        }
        else if (Role.SERVICE_PROVIDER.name().equals(role.name())) {
            System.out.println("inside SERVICE_PROVIDER");

            adDetailsList = new ArrayList<>(creditCardNumbers.size());
            for (int i = 0; i < creditCardNumbers.size(); i++) {
                AdDetails adDetails = new AdDetails(creditCardNumbers.get(i));
                adDetailsList.add(adDetails);
            }
            inputData.setAdDetailsList(adDetailsList);
        }
        return inputData;
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
