package com.r3.conclave.sample.client;

import com.r3.conclave.client.EnclaveConstraint;
import com.r3.conclave.client.InvalidEnclaveException;
import com.r3.conclave.common.EnclaveInstanceInfo;
import com.r3.conclave.mail.Curve25519PrivateKey;
import com.r3.conclave.mail.EnclaveMail;
import com.r3.conclave.mail.PostOffice;
import com.r3.conclave.sample.common.InputData;
import org.tribuo.MutableDataset;
import org.tribuo.classification.LabelFactory;
import org.tribuo.data.csv.CSVLoader;
import org.tribuo.datasource.ListDataSource;
import org.tribuo.evaluation.TrainTestSplitter;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.util.UUID;

/**
 * The clients for this application will be different doctors/hospitals. They will upload files containing breast cancer dataset.
 * The client will serialise this dataset and send it to the enclave. The enclave will load the model, train the model with this
 * data. The client can send test data to test this trained model. If the results of this testing are as expected/correct, this
 * trained model can be used to make predictions of a given input tumor.
 *
 */
public class Client {
    private static final String TRAIN = "TRAIN";

    public static void main(String[] args) throws InterruptedException, IOException, InvalidEnclaveException {

        if (args.length == 0) {
            System.err.println("Please pass <TRAIN> <CONSTRAINT> <FILENAME> to train the model using the data from filename " +
                    "or Please pass <EVALUATE> <CONSTRAINT> to evaluate the model. The constraint can be found" +
                    " printed in console during the build process.");

            return;
        }
        String inputType = args[0];
        String constraint = args[1];

        //connect to host server
        DataInputStream fromHost;
        DataOutputStream toHost;
        while (true) {
            try {
                System.out.println("Attempting to connect to localhost:9999");
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress(InetAddress.getLoopbackAddress(), 9999), 5000);
                fromHost = new DataInputStream(socket.getInputStream());
                toHost = new DataOutputStream(socket.getOutputStream());
                break;
            } catch (Exception e) {
                System.err.println("Retrying: " + e.getMessage());
                Thread.sleep(2000);
            }
        }

        //take inputs from user, populate the input data object with training, testing data and input type - train or evaluate
        InputData inputData = new InputData(inputType);

        if(TRAIN.equals(inputType)) {
            //load the dataset to input type object using the csv loader
            LabelFactory labelFactory = new LabelFactory();
            CSVLoader csvLoader = new CSVLoader<>(labelFactory);

            //input attributes/labels supplied via the input data file. Last column (in this case "class") is the classification output column name
            String[] breastCancerHeaders = new String[]{"SampleCodeNumber", "ClumpThickness", "UniformityOfCellSize", "UniformityOfCellShape", "MarginalAdhesion",
                    "SingleEpithelialCell Size", "BareNuclei", "BlandChromatin", "NormalNucleoli", "Mitoses", "Class"};

            String fileName = System.getProperty("user.dir")+"/data/"+args[2];
            ListDataSource irisesSource = csvLoader.loadDataSource(Paths.get(fileName),"Class",breastCancerHeaders);

            //split the input loaded data into training and testing data
            TrainTestSplitter irisSplitter = new TrainTestSplitter<>(irisesSource,0.7,1L);
            MutableDataset trainingDataset = new MutableDataset<>(irisSplitter.getTrain());
            MutableDataset testingDataset = new MutableDataset<>(irisSplitter.getTest());

            inputData.setTestingDataset(testingDataset);
            inputData.setTrainingDataset(trainingDataset);
        }

        //retrieve the attestation object from host immediately after connecting to host
        byte[] attestationBytes = new byte[fromHost.readInt()];
        fromHost.readFully(attestationBytes);

        //convert byte[] received from host to EnclaveInstanceInfo object
        EnclaveInstanceInfo instanceInfo = EnclaveInstanceInfo.deserialize(attestationBytes);

        //verify attestation received by enclave against the enclave code hash which we have
        String enclaveMode = instanceInfo.getEnclaveInfo().getEnclaveMode().toString();
        if("MOCK".equals(enclaveMode)) {
            EnclaveConstraint.parse("S:0000000000000000000000000000000000000000000000000000000000000000"+" PROD:1 SEC:INSECURE" ).check(instanceInfo);
        } else {
            EnclaveConstraint.parse("S:"+constraint+" PROD:1 SEC:INSECURE" ).check(instanceInfo);
        }

        //create a dummy key pair for sending via mail to enclave
        PrivateKey key = Curve25519PrivateKey.random();

        //create PostOffice specifying - clients public key, topic name , enclaves public key
        PostOffice postOffice = instanceInfo.createPostOffice(key, UUID.randomUUID().toString());

        //encrypt the message using enclave's public key
        byte[] encryptedRequest = postOffice.encryptMail(serialize(inputData));

        //send the encrypted mail to host to relay it to enclave
        toHost.writeInt(encryptedRequest.length);
        toHost.write(encryptedRequest);

        //get the reply back from host via the socket
        byte[] encryptedReply = new byte[fromHost.readInt()];
        fromHost.readFully(encryptedReply);

        //use Post Office to decrypt back the mail sent by the enclave
        EnclaveMail mail = postOffice.decryptMail(encryptedReply);
        System.out.println(new String(mail.getBodyAsBytes()));

        toHost.close();
        fromHost.close();
    }

    /**
     * Serialise the input data object containing training and testing data for sending it to enclave.
     * @param inputData inputData object containing attributes to train/test the model
     * @return          serialised inputData object
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
}
