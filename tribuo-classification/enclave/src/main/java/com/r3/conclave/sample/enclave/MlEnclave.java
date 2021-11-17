package com.r3.conclave.sample.enclave;

import com.r3.conclave.enclave.Enclave;
import com.r3.conclave.mail.EnclaveMail;
import com.r3.conclave.sample.common.InputData;
import com.r3.conclave.sample.common.Role;
import org.tribuo.Example;
import org.tribuo.Model;
import org.tribuo.MutableDataset;
import org.tribuo.Trainer;
import org.tribuo.classification.Label;
import org.tribuo.classification.evaluation.LabelEvaluation;
import org.tribuo.classification.evaluation.LabelEvaluator;
import org.tribuo.classification.sgd.linear.LogisticRegressionTrainer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.util.HashMap;

import java.util.Map;

/**
 * Enclave will load the classification model. This model will be trained using the dataset provided by multiple clients.
 * Clients can then test this model using the testing/evaluation data sent by the clients. Evaluation results will be sent
 * back to the clients.
 */
public class MlEnclave extends Enclave {

    protected static MutableDataset trainingDataset;
    protected static MutableDataset testingDataset;

    private Map<String, PublicKey> routingHintToPublicKey = new HashMap<>();

    /**
     * This method gets called when client wants to communicate to enclave, and sends a message wrapped in a mail to host.
     * Host in turn calls deliverMail method which in turn
     * calls this method. In this method, we will deserialize the mail message, perform the computation and send the
     * result back to the clients.
     */
    @Override
    protected void receiveMail(EnclaveMail mail, String routingHint) {

       InputData inputData = (InputData) deserialize(mail.getBodyAsBytes());
       if(inputData != null) {
           Role inputType = inputData.getInputType();

           routingHintToPublicKey.put(routingHint, mail.getAuthenticatedSender());

           if (inputType == Role.EVALUATE) {
               trainAndEvaluateModel();
           } else {
               collectData(inputData);
           }
       }
    }

    /**
     * Once the client sends EVALUATE via the terminal, this method trains the model using data collected by far from
     * all the clients. It also evaluates the model, and sends the results back to all the clients.
     */
    private void trainAndEvaluateModel() {
        Trainer<Label> trainer = new LogisticRegressionTrainer();
        Model<Label> irisModel = trainer.train(trainingDataset);

        LabelEvaluator evaluator = new LabelEvaluator();
        LabelEvaluation evaluation = evaluator.evaluate(irisModel, testingDataset);

        //send evaluation results back to all the clients using the routingHintToPublicKey mapping
        for (Map.Entry<String, PublicKey> entry : routingHintToPublicKey.entrySet()) {
            byte[] encryptedReply = postOffice(routingHintToPublicKey.get(entry.getKey()))
                    .encryptMail(evaluation.toString().getBytes(StandardCharsets.UTF_8));
            postMail(encryptedReply, entry.getKey());
        }
    }

    /**
     * This method collects data from all clients which later will be used to train the model.
     */
    protected void collectData(InputData inputData) {
        if (trainingDataset == null && testingDataset == null) {
            trainingDataset = inputData.getTrainingDataset();
            testingDataset = inputData.getTestingDataset();
        } else {
            inputData.getTrainingDataset().getData().stream().forEach(example -> trainingDataset.add((Example) example));
            inputData.getTestingDataset().getData().stream().forEach(example -> testingDataset.add((Example) example));
        }
        System.out.println("Training dataset size : " + trainingDataset.size());
        System.out.println("Testing  dataset size : " + testingDataset.size());
    }

    /**
     * This method deserializes input data from clients to be used by enclave.
     * @param  inputData
     * @return deserialized input data object
     */
    public static Object deserialize(byte[] inputData) {
        ByteArrayInputStream in = new ByteArrayInputStream(inputData);
        ObjectInputStream is;
        try {
            is = new ObjectInputStream(in);
            return is.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
}