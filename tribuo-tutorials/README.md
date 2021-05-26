# Tribuo tutorials

[Tribuo](https://tribuo.org) is a Java machine learning library, which makes it well
suited to run with Conclave.
It provides tools for classification, regression, clustering, model development, and more.

If you are new to Conclave, start by reading our [documentation](https://docs.conclave.net) and
[hello world tutorial](https://docs.conclave.net/writing-hello-world.html).

If you are new to Tribuo, follow the [Classification Tutorial](https://tribuo.org/learn/4.0/tutorials/irises-tribuo-v4.html).
It introduces the ins and outs of training and testing a model as well as loading and saving the model for future use.

Take your time to familiarize yourself with the following tutorials as it will make
it easier to understand this project.
* [Classification](https://tribuo.org/learn/4.0/tutorials/irises-tribuo-v4.html)
* [Clustering](https://tribuo.org/learn/4.0/tutorials/clustering-tribuo-v4.html)
* [Regression](https://tribuo.org/learn/4.0/tutorials/regression-tribuo-v4.html)
* [Anomaly Detection](https://tribuo.org/learn/4.0/tutorials/anomaly-tribuo-v4.html)
* [Configuration](https://tribuo.org/learn/4.0/tutorials/configuration-tribuo-v4.html)

The scenarios that these tutorials explore is one where you want to train or utilise a model on a computer operated by
a third party and where it is imperative that the third party cannot access the training data or the resulting models
and that they cannot observe or influence execution. The tutorials demonstrate how to use Conclave to create
secure enclaves that can operate in this manner, and how to access them remotely in a way that can verify that
the models are indeed being executed in a mode that is protected from the owner of the computer on which they run.

To keep the communication between the client and enclave secure, [Mail](https://docs.conclave.net/mail.html)
is being used. Each mail contains a serialized request for the enclave to execute some action, i.e.,
sending configuration parameters for training models and obtaining evaluation results.
[Kotlin's serialization](https://github.com/Kotlin/kotlinx.serialization/tree/v1.0.1/docs) is used to serialize
requests and responses.

## Running
This project is compatible with Conclave v1.1. To run it, execute the following instructions:

* Set the `conclaveRepo` property in `gradle.properties` or override it on the command line,
so it points to your Conclave distribution.
* Run `./gradlew host:run`, which will launch the host and wait for the client to connect.
* Run `./gradlew client:run --args "<productID> <codeSigner> <securityInfoSummary>"`,
which will launch the client, connect to the host and execute all tutorials.

When running on a non-secure mode, `--args` can be set to
`"1 4924CA3A9C8241A3C0AA1A24A407AA86401D2B79FA9FF84932DA798A942166D4 INSECURE"`, if
using the provided `sample_private_key.pem`. For `mock` mode, only the `<productID>`
is required, while `<codeSigner>` and `securityInfoSummary` are ignored and set to
`0000000000000000000000000000000000000000000000000000000000000000` and `INSECURE`
respectively in the code.

## Acknowledgments
The tutorials use the [Iris](https://archive.ics.uci.edu/ml/datasets/Iris) and
[Wine](https://archive.ics.uci.edu/ml/datasets/Wine) datasets, from
[UCI Machine Learning Repository](https://archive.ics.uci.edu/ml/index.php) and also the
[The MNIST database of handwritten digits](http://yann.lecun.com/exdb/mnist/).

The data files have been obtained from:
* [bezdekIris.data](https://archive.ics.uci.edu/ml/machine-learning-databases/iris/bezdekIris.data)
* [winequality-red.csv](https://archive.ics.uci.edu/ml/machine-learning-databases/wine-quality/winequality-red.csv)
* [train-images-idx3-ubyte.gz](http://yann.lecun.com/exdb/mnist/train-images-idx3-ubyte.gz)
* [train-labels-idx1-ubyte.gz](http://yann.lecun.com/exdb/mnist/train-labels-idx1-ubyte.gz)
* [t10k-images-idx3-ubyte.gz](http://yann.lecun.com/exdb/mnist/t10k-images-idx3-ubyte.gz)
* [t10k-labels-idx1-ubyte.gz](http://yann.lecun.com/exdb/mnist/t10k-labels-idx1-ubyte.gz)

> Please refrain from accessing the [The MNIST database of handwritten digits](http://yann.lecun.com/exdb/mnist/) files
from automated scripts with high frequency. Make copies!
