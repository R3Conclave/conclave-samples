package com.r3.conclave.sample.client;

import com.r3.conclave.client.EnclaveClient;
import com.r3.conclave.client.web.WebEnclaveTransport;
import com.r3.conclave.common.EnclaveConstraint;
import com.r3.conclave.mail.EnclaveMail;
import com.r3.conclave.sample.dataanalysis.common.UserProfile;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import picocli.CommandLine.Parameters;


@CommandLine.Command(name = "column-profiling-client",
        mixinStandardHelpOptions = true,
        description = "Simple client that communicates with the DAEnclave using the web host.")
public class Client implements Callable<Void> {
    @Parameters(description = "The dataset as space separated values to send to the enclave")
    private List<String> dataset = new ArrayList<>();

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
        EnclaveClient enclaveClient;
        System.out.println(url);
        System.out.println(constraint);
        enclaveClient = new EnclaveClient(constraint);

        try (WebEnclaveTransport transport = new WebEnclaveTransport(url);
             EnclaveClient client = enclaveClient) {
            client.start(transport);
            ArrayList<UserProfile> inputData = getInputData();
            byte[] requestMailBody = serialize(inputData);
            EnclaveMail responseMail = client.sendMail(requestMailBody);
            String responseString = (responseMail != null) ? new String(responseMail.getBodyAsBytes()) : null;
            System.out.println("Enclave gave the output " + responseString + "`");
        }

        return null;
    }

    private ArrayList<UserProfile> getInputData() throws IOException, NullPointerException {
        if (dataset.size() == 0 || dataset.size() % 4 != 0) {
            System.err.println("Invalid input provided. Please pass the user dataset for processing as parameters to the command line as space separated values: Name Age Country Gender");
        }
        ArrayList<UserProfile> l = new ArrayList<UserProfile>() {
        };
        for (int i = 0; i < dataset.size(); i = i + 4) {
            UserProfile u = new UserProfile(dataset.get(i),
                    Integer.parseInt(dataset.get(i + 1)),
                    dataset.get(i + 2),
                    dataset.get(i + 3));
            l.add(u);
        }
        return l;
    }


    public static byte[] serialize(List<UserProfile> l) {
        // Serialize and send the data to enclave.
        // Reference for stream of bytes
        byte[] stream = null;
        // ObjectOutputStream is used to convert a Java object into OutputStream
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            for (int i = 0; i < l.size(); i++) {
                oos.writeObject(l.get(i));
            }
            oos.writeObject(null);
            oos.close();
            baos.close();
            stream = baos.toByteArray();


        } catch (IOException e) {
            // Error in serialization
            e.printStackTrace();
        }

        return stream;
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