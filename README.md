<p align="center">
  <img src="https://conclave.net/wp-content/uploads/2020/12/Conclave_logo_master.png" alt="Conclave" width="500">
</p>
<br>

# Conclave Samples

This repository contains multiple Conclave sample applications which is intended to help
developers to get started with Conclave and understand different features of
the platform. To learn more about the Conclave platform please visit our official
[documentation](https://docs.conclave.net/).

## Table of Contents

1. [Samples](#Samples)
2. [Learning Resources](#Resources)
3. [Getting help](#Help)
4. [License](#License)

## Samples <a name="Samples"></a>

### [EventManager](./EventManager):
Event Manager implements the idea of an enclave that can host a collection of simultaneous 'computations'. These computations are initiated by a Conclave client, have a name and a type, have a defined set of 'participants', and a 'quorum' that must be reached before results can be obtained. Supported types are average (the average of the submissions is returned), maximum and minimum (the identity of the submitter of the highest or lowest value is returned), and 'key matcher' (described below).
The idea is that an enclave of this type could be used to host simple one-off multi-party computations (e.g., five employees wanting to know the average of their salaries, or ten firms wanting to know who had the lowest sales last month without revealing what that figure was).

### [Column Profiling](./column-profiling):
This sample does Column Profiling on the dataset and
returns the frequency distribution as an output. Column profiling is one of the methods used in data profiling.
This [article](https://www.alooma.com/blog/what-is-data-profiling) explains what data profiling and column profiling means
and explains its uses and applications.

### [Conclave Auction](./conclave-auction):
This sample allows bidders to confidentially submit their bids to an enclave (A protected region of memory which cannot be accessed by the OS, Kernel or BIOS). The bids are processed confidentially in the enclave and the result is returned to concerned parties.

### [Conclave Corda Trade](./conclave-corda-trade):
This application serves as a demo for building a confidential trading system based on Corda and Conclave. An exchange Corda node would serve as a host which runs the enclave, while broker nodes serve as clients which send encrypted orders from their end-clients which are matched in the enclave and trades generated are recorded in all relevant participants ledgers.

### [Conclave Hello World Python](./hello-world-python):
This is a simple conclave application which shows how you can reverse a string in python.

### [Cordapp](./cordapp):
This is a simple CorDapp using the Conclave API and functions as a tutorial. It is licensed under the Apache 2 license, and therefore you may copy/paste it to act as the basis of your own commercial or open source apps.

### [Psi](./psi-sample):
PSI problem refers to the problem of determining the common elements from the intersection of two sets without leaking or disclosing any additional information of the remaining elements of either sets.
Using this sample will demonstrate how Conclave (based on Intel SGX) can be a new tool to solve the private set intersection (PSI) problem.

### [Tribuo Classification](./tribuo-classification):
Using this sample will show how Conclave (based on Intel SGX) can be used in training a ML model. We will use Tribuo Java ML library to load the AI model.
For example, hospitals have patients' data which can be used to determine whether a tumour is malignant or benign.
Such data can be used to train an AI model. Once a model is trained, this model can be used to predict if a given tumor is malignant
or begnin given certain input attributes.

### [Tribuo Tutorials](./tribuo-tutorials):
Tribuo is a Java machine learning library, which makes it well suited to run with Conclave. This sample provides tools for classification, regression, clustering, model development, and more.
This sample shows you how you can use the [Tribuo Java ML](https://tribuo.org/learn/4.0/tutorials/) library to load and train models like classification models, regression models, clustering models etc.

### [Database Enclave](./psi-sample):
With the new 3rd Gen Intel Xeon Scalable Processors supporting 1 TB of enclave memory, 
setting up a database inside an enclave is very well possible. This sample shows how persistence is used to create a database inside an enclave and save data into it as well as how to create a
table, insert records into it, and select records from the table. This also shows how persisted records can be retrieved by
the enclave once the host is re-started.

## Learning Resources <a name="Resources"></a>

- [Documentation](https://docs.conclave.net)
  - We recommend reading [Concepts chapter](https://docs.conclave.net/enclaves.html) to learn more about confidential computing, enclaves, and Conclave's architecture
- [Video tutorials and bootcamps](https://www.youtube.com/channel/UCSZbii_Z5QXci6Sr-CvStuw)
  -  We recommend watching the video ["Getting started with Conclave SDK 1.3"](https://youtu.be/nwyGL5OemSU)
-  [Developer Blog](https://developer.r3.com/blog/category/conclave/)
-  Get the latest news and announcements on [Twitter](https://twitter.com/Conclavecompute),  or sign up for our monthly [newsletter](https://www.conclave.net/#newsletter)

## Getting Help <a name="Help"></a>

- Get stuck or have any questions?
  - Check our [FAQs](https://docs.conclave.net/faq.html)
  - Head over to [Discord](https://discord.gg/8RhkXc5eFp)
  - Sign up for our [mailing list](https://groups.io/g/conclave-discuss)

## License <a name="License"></a>
The source code files are available under the Apache License, Version 2.0.
The licence file can be found [here](https://github.com/R3Conclave/conclave-samples/blob/master/LICENSE).
