## Conclave Sample: Breast Cancer Analysis using Tribuo Java ML Library

Using this sample will see how Conclave (based on Intel SGX) can be used in training a ML model. 
Most of the hospitals/doctors have lot of their patients data, which can be used to find if a tumor is malignant or begnin.
Such data can be used to train an AI model. Once a model is trained, this model can be used to predict if a given tumor is malignant 
or begnin given certain input attributes.

This is a simple app using the Conclave API. It is licensed under the Apache 2 license, and therefore you may 
copy/paste it to act as the basis of your own commercial or open source apps.

Use below link to start download Conclave and start building Conclave apps:

https://conclave.net/get-conclave/

##  Dataset sample used for this application.
The dataset used for this sample has been taken from the below url. 
https://archive.ics.uci.edu/ml/datasets/breast+cancer+wisconsin+(original)
There are 11 columns in the data set. The first 10 columns reflect different attributes which help in detecting 
if the breast cancer is malignant or benign. The last column is the class 2 for benign, 4 for malignant.

##  Attribute                     Domain
   -- -----------------------------------------
1. Sample code number            id number
2. Clump Thickness               1 - 10
3. Uniformity of Cell Size       1 - 10
4. Uniformity of Cell Shape      1 - 10
5. Marginal Adhesion             1 - 10
6. Single Epithelial Cell Size   1 - 10
7. Bare Nuclei                   1 - 10
8. Bland Chromatin               1 - 10
9. Normal Nucleoli               1 - 10
10. Mitoses                       1 - 10
11. Class:                        (2 for benign, 4 for malignant)

## Parties involved in the sample
Hospitals, Host, Enclave.

Hospitals - Hospitals are responsible for providing input data to train the model.

Host - Hosts loads the enclave.

Enclave - Enclave collates all the data given by all hospitals and trains the model. This also evaluates the model and
sends the evaluation result back to the clients.

## Evaluation result
Below evaluation result gets printed out to all the clients

| Class        | n           | tp  |fn    |fp    | recall           | prec  |f1 |
| ------------- |-------------  | -----|------------- |:-------------:| -----:|-----:|-----:|
| 2      | 264    |     0       |   264        |    0        | 0.000       |0.000          |   0.000 |
|4                    |          156 |        156   |        0     |    264    |   1.000   |    0.371 |      0.542|
Total                   |      420       |  156       |  264       |  264|
Accuracy         | | | | |                                                          0.371
Micro Average            | | | | |                                                     0.371   |    0.371   |    0.371|
Macro Average                        | | | | |                                         0.500   |     0.186    |    0.271| 
Balanced Error Rate        | | | | |                                                   0.500

## How to run on a non-linux based system

Start the host on a non-Linux system, which will build the enclave and host by default in simulation mode.

    ./container-gradle host:run

To run on a linux system, use gradlew instead of container-gradle. This will also start the host in simulation mode.

    ./gradlew -PenclaveMode=mock host:run

To run the host in a mock mode, use the below command.

    ./gradlew -PenclaveMode=mock host:run

On your terminal, once the host starts, start the client, and pass in the file name to the breast cancer data to train the model

    ./gradlew client:run --args="TRAIN breast-cancer.data"

On other terminal, start a new client and pass in a new file name

    ./gradlew client:run --args="TRAIN breast-cancer-1.data"

You can start as many clients as you want. Pass in the file names each time. Once all the clients pass in the training data,
train the model inside the enclave and retrieve the evaluation result. 

    ./gradlew client:run --args="EXECUTE"

To read more on Conclave go to the documentation site - https://docs.conclave.net

Please note the citation request made by the owners of the input data set used in this sample for testing.
Citation Request:
This breast cancer databases was obtained from the University of Wisconsin
Hospitals, Madison from Dr. William H. Wolberg.  If you publish results
when using this database, then please include this information in your
acknowledgements.  Also, please cite one or more of:
1. O. L. Mangasarian and W. H. Wolberg: "Cancer diagnosis via linear
   programming", SIAM News, Volume 23, Number 5, September 1990, pp 1 & 18.
2. William H. Wolberg and O.L. Mangasarian: "Multisurface method of
   pattern separation for medical diagnosis applied to breast cytology",
   Proceedings of the National Academy of Sciences, U.S.A., Volume 87,
   December 1990, pp 9193-9196.
3. O. L. Mangasarian, R. Setiono, and W.H. Wolberg: "Pattern recognition
   via linear programming: Theory and application to medical diagnosis",
   in: "Large-scale numerical optimization", Thomas F. Coleman and Yuying
   Li, editors, SIAM Publications, Philadelphia 1990, pp 22-30.
4. K. P. Bennett & O. L. Mangasarian: "Robust linear programming
   discrimination of two linearly inseparable sets", Optimization Methods
   and Software 1, 1992, 23-34 (Gordon & Breach Science Publishers).
