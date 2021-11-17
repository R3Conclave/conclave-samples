package com.r3.conclave.sample.client;

import com.r3.conclave.client.EnclaveClient;
import com.r3.conclave.client.web.WebEnclaveTransport;
import com.r3.conclave.common.EnclaveConstraint;
import com.r3.conclave.mail.EnclaveMail;
import com.r3.conclave.sample.common.InputData;
import com.r3.conclave.sample.common.Role;
import org.tribuo.MutableDataset;
import org.tribuo.classification.LabelFactory;
import org.tribuo.data.csv.CSVLoader;
import org.tribuo.datasource.ListDataSource;
import org.tribuo.evaluation.TrainTestSplitter;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

/**
 * The clients for this application will be different doctors/hospitals. They will upload files containing breast cancer dataset.
 * The client will serialise this dataset and send it to the enclave. The enclave will load the model, train the model with this
 * data. The client can send test data to test this trained model. If the results of this testing are as expected/correct, this
 * trained model can be used to make predictions of a given input tumor.
 *
 */
@CommandLine.Command(name = "psi-client",
        mixinStandardHelpOptions = true,
        description = "Simple client that communicates with the PSIEnclave using the web host.")
public class Client implements Callable<Void> {

    @CommandLine.Option(names = {"-u", "--url"},
            required = true,
            description = "URL of the web host running the enclave.")
    private String url;

    @CommandLine.Option(names = {"-c", "--constraint"},
            required = true,
            description = "Enclave constraint which determines the enclave's identity and whether it's acceptable to use.",
            converter = EnclaveConstraintConverter.class)
    private EnclaveConstraint constraint;

    @CommandLine.Option(names = {"-f", "--file"},
            required = true,
            description = "URL of the web host running the enclave.")
    private String fileName;

    @CommandLine.Option(names = {"-r", "--role"}
            , description = "Role Options: ${TRAIN/EVALUATE}")
    private Role role = null;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Client()).execute(args);
        System.exit(exitCode);
    }

    /**
     * Serialise the input data object containing training and testing data for sending it to enclave.
     * @param inputData inputData object containing attributes to train/test the model
     * @return serialised inputData object
     * @throws IOException
     */
    public static byte[] serialize(Object inputData) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(out);
        os.writeObject(inputData);
        os.reset();
        out.close();
        return out.toByteArray();
    }

    @Override
    public Void call() throws Exception {
        EnclaveClient enclaveClient = new EnclaveClient(constraint);

        try (WebEnclaveTransport transport = new WebEnclaveTransport(url);
             EnclaveClient client = enclaveClient) {
            /*Retrieve the enclaveInstanceInfo object, i.e. the remote attestation object and verify it against the
            constraint*/
            client.start(transport);

            //Collect merchants and service providers credit card numbers list
            InputData inputData = getInput();

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
            System.out.println("Ad Conversion Rate is : " + new String(responseMail.getBodyAsBytes()));
        }
        return null;
    }

    private InputData getInput() throws IOException {
        InputData inputData = new InputData(role);
        if (role == Role.TRAIN) {
            //Load the dataset to input type object using the csv loader
            LabelFactory labelFactory = new LabelFactory();
            CSVLoader csvLoader = new CSVLoader<>(labelFactory);

            //Input attributes/labels supplied via the input data file. Last column (in this case "class") is the classification output column name
            String[] breastCancerHeaders = new String[]{"SampleCodeNumber", "ClumpThickness", "UniformityOfCellSize", "UniformityOfCellShape", "MarginalAdhesion",
                    "SingleEpithelialCell Size", "BareNuclei", "BlandChromatin", "NormalNucleoli", "Mitoses", "Class"};

            String filePath = System.getProperty("user.dir") + "/client/data/" + fileName;
            ListDataSource irisesSource = csvLoader.loadDataSource(Paths.get(filePath), "Class", breastCancerHeaders);

            //Split the input loaded data into training and testing data
            TrainTestSplitter irisSplitter = new TrainTestSplitter<>(irisesSource, 0.7, 1L);
            MutableDataset trainingDataset = new MutableDataset<>(irisSplitter.getTrain());
            MutableDataset testingDataset = new MutableDataset<>(irisSplitter.getTest());

            inputData.setTestingDataset(testingDataset);
            inputData.setTrainingDataset(trainingDataset);
        }
        return inputData;
    }


    private static class EnclaveConstraintConverter implements CommandLine.ITypeConverter<EnclaveConstraint> {
        @Override
        public EnclaveConstraint convert(String value) {
            return EnclaveConstraint.parse(value);
        }
    }
}
