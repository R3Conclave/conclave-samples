package com.r3.conclave.sample.client;

import com.r3.conclave.client.EnclaveClient;
import com.r3.conclave.client.web.WebEnclaveTransport;
import com.r3.conclave.common.EnclaveConstraint;
import com.r3.conclave.common.InvalidEnclaveException;
import com.r3.conclave.mail.EnclaveMail;
import com.r3.conclave.sample.common.CommandType;
import com.r3.conclave.sample.common.InputData;
import com.r3.conclave.sample.common.UserData;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.ITypeConverter;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;


@Command(name = "persistent-client",
        mixinStandardHelpOptions = true,
        description = "Simple client that communicates with the PersistentEnclave using the web host.")
public class PersistentClient implements Callable<Void> {
    @Parameters(index = "0", description = "Command specifying the type of operation: ${ADD/VERIFY}")
    private String command;

    @Parameters(index = "1", description = "username")
    private String username;

    @Parameters(index = "2", description = "password")
    private String password;

    @Option(names = {"-u", "--url"},
            required = true,
            description = "URL of the web host running the enclave.")
    private String url;

    @Option(names = {"-c", "--constraint"},
            required = true,
            description = "Enclave constraint which determines the enclave's identity and whether it's acceptable to use.",
            converter = EnclaveConstraintConverter.class)
    private EnclaveConstraint constraint;

    @Option(names = {"-f", "--file-state"},
            required = true,
            description = "File to store the state of the client. If the file doesn't exist a new one will be created.")
    private Path file;

    public static void main(String... args) {
        int exitCode = new CommandLine(new PersistentClient()).execute(args);
        System.exit(exitCode);
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

    @Override
    public Void call() throws IOException, InvalidEnclaveException {
        EnclaveClient enclaveClient;
        if (Files.exists(file)) {
            enclaveClient = new EnclaveClient(Files.readAllBytes(file));
            System.out.println("Loaded previous client state and thus using existing private key.");
        } else {
            System.out.println("No previous client state. Generating new state with new private key.");
            enclaveClient = new EnclaveClient(constraint);
        }

        try (WebEnclaveTransport transport = new WebEnclaveTransport(url);
             EnclaveClient client = enclaveClient) {
            client.start(transport);

            InputData inputData = getInputData();
            if (inputData != null) {
                byte[] requestMailBody = serialize(inputData);
                EnclaveMail responseMail = client.sendMail(requestMailBody);
                String responseString = (responseMail != null) ? new String(responseMail.getBodyAsBytes()) : null;
                System.out.println("Enclave " + "gave response:" + responseString);
                Files.write(file, client.save());
            } else {
                System.out.println("Invalid input");
            }

        }

        return null;
    }

    private InputData getInputData() {
        if (CommandType.valueOf(command) == CommandType.ADD) {
            InputData in = new InputData(CommandType.ADD, new UserData(username, password));
            return in;
        } else if (CommandType.valueOf(command) == CommandType.VERIFY) {
            InputData in = new InputData(CommandType.VERIFY, new UserData(username, password));
            return in;
        } else {
            return null;
        }
    }

    private static class EnclaveConstraintConverter implements ITypeConverter<EnclaveConstraint> {
        @Override
        public EnclaveConstraint convert(String value) {
            return EnclaveConstraint.parse(value);
        }
    }


}
