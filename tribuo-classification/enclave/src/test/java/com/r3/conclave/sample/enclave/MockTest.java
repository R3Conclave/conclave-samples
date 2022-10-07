package com.r3.conclave.sample.enclave;

import com.r3.conclave.host.EnclaveHost;
import com.r3.conclave.host.EnclaveLoadException;
import com.r3.conclave.sample.common.InputData;
import com.r3.conclave.sample.common.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tribuo.DataSource;
import org.tribuo.MutableDataset;
import org.tribuo.classification.LabelFactory;
import org.tribuo.data.csv.CSVLoader;
import org.tribuo.datasource.ListDataSource;
import org.tribuo.evaluation.TrainTestSplitter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * This test class calculates the ad conversion rate given the inputs.
 */
public class MockTest {
    private EnclaveHost enclaveHost;
    private MlEnclave mlEnclave;

    @BeforeEach
    void setUp() throws EnclaveLoadException {
        enclaveHost = EnclaveHost.load("com.r3.conclave.sample.enclave.MlEnclave");
        enclaveHost.start(null, null, null, (commands) -> {
        });
        mlEnclave = (MlEnclave) enclaveHost.getMockEnclave();
    }

    @Test
    public void collectData() throws IOException {
        collectDataClient1();
        collectDataClient2();
    }

    void collectDataClient1() throws IOException {
        InputData inputData = loadInputData();
        populateData(mlEnclave, inputData);
        assertNotNull(MlEnclave.trainingDataset);
        assertNotNull(MlEnclave.testingDataset);
        assertEquals(489, MlEnclave.trainingDataset.size());
        assertEquals(210, MlEnclave.testingDataset.size());
    }

    void collectDataClient2() throws IOException {
        InputData inputData = loadInputData();
        populateData(mlEnclave, inputData);
        assertNotNull(MlEnclave.trainingDataset);
        assertNotNull(MlEnclave.testingDataset);
        assertEquals(978, MlEnclave.trainingDataset.size());
        assertEquals(420, MlEnclave.testingDataset.size());
    }

    private void populateData(MlEnclave mlEnclave, InputData inputData) {
        mlEnclave.collectData(inputData);
        System.out.println(MlEnclave.trainingDataset);
    }

    private InputData loadInputData() throws IOException {
        LabelFactory labelFactory = new LabelFactory();
        CSVLoader csvLoader = new CSVLoader<>(labelFactory);

        //input attributes/labels supplied via the input data file. Last column (in this case "class") is the classification output column name
        String[] breastCancerHeaders = new String[]{"SampleCodeNumber", "ClumpThickness", "UniformityOfCellSize", "UniformityOfCellShape", "MarginalAdhesion",
                "SingleEpithelialCell Size", "BareNuclei", "BlandChromatin", "NormalNucleoli", "Mitoses", "Class"};

        ClassLoader classLoader = getClass().getClassLoader();

        File file = new File(classLoader.getResource("data/breast-cancer.data").getFile());
        System.out.println(file.getAbsolutePath());

        DataSource irisesSource = csvLoader.loadDataSource(Paths.get(file.toURI()), "Class",breastCancerHeaders);

        //split the input loaded data into training and testing data
        TrainTestSplitter irisSplitter = new TrainTestSplitter<>(irisesSource,0.7,1L);
        MutableDataset trainingDataset = new MutableDataset<>(irisSplitter.getTrain());
        MutableDataset testingDataset = new MutableDataset<>(irisSplitter.getTest());

        InputData inputData = new InputData(Role.TRAIN);
        inputData.setTestingDataset(testingDataset);
        inputData.setTrainingDataset(trainingDataset);
        return inputData;
    }
}