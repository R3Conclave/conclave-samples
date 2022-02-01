package com.r3.conclave.sample.client;

import com.r3.conclave.client.EnclaveClient;
import com.r3.conclave.client.web.WebEnclaveTransport;
import com.r3.conclave.common.EnclaveConstraint;
import com.r3.conclave.mail.EnclaveMail;
import com.r3.conclave.sample.common.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;


/**
 * Clients can send two types of commands to the enclave. Clients can insert new records in the database enclave
 * by passing the ADD command. Clients can select the inserted user data by passing in the VERIFY command.
 * We will restart the enclave and hit the VERIFY command again to make sure that the database is persisted
 * by conclave.
 */
@CommandLine.Command(name = "database-client",
        mixinStandardHelpOptions = true,
        description = "Simple client that communicates with the DatabaseEnclave using the web host.")
public class Client implements Callable<Void> {

    private static final Logger logger = LoggerFactory.getLogger(Client.class);

    @CommandLine.Option(names = {"-o", "--command"}
            , description = "Command Options: ${CREATE/ADD/VERIFY}")
    private CommandType commandType = null;

    //Use picocli to provide command line parameters
    @CommandLine.Parameters(description = "username password")
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
            logger.info("Enclave Reply is : " + new String(responseMail.getBodyAsBytes()));
        }
        return null;
    }

    private InputData getInputData() {
        InputData inputData = new InputData();
        inputData.setCommandType(commandType);
        if(!userDetails.isEmpty()) {
            if(commandType == CommandType.ADD) {
                inputData.setUser(new User(userDetails.get(0), userDetails.get(1)));
            } else if(commandType == CommandType.VERIFY) {
                inputData.setUser(new User(userDetails.get(0)));
            }
        }
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
