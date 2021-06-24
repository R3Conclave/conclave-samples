# Confidential Trade with Corda and Conclave 

This application serves as a demo for building a confidential trading system based on
Corda and Conclave. It leverages the flow framework for Corda sending and receiving
confidential orders and matched trades.

An exchange Corda node would serve as a host which runs the enclave, while broker 
nodes servers as clients which send encryped order from their end-clients which 
are matched in the enclave and trades generated are recorded in all relevant 
participants ledgers.


# Usage

## Pre-Requisites
For Corda development environment setup, please refer to: [Setup Guide](https://docs.corda.net/getting-set-up.html).

## Conclave SDK Location
In order to compile this sample successfully, kindly download the conclave SDK and update the location in gradle.properties.

conclaveRepo=<path_to_conclave_sdk>

Conclave SDK can ve downloaded from here: https://conclave.net/get-conclave/

### Running the nodes:
Open a terminal and go to the project root directory and type: (to deploy the nodes using bootstrapper)
```
./gradlew prepareDockerNodes
```

Then type: (to run the nodes)
```
docker-compose -f ./build/nodes/docker-compose.yml up
```

<strong>NOTE:</strong> We will be using docker here since enclaves can be run only on linux machines. You could run
it locally if you are on linux using the below commands:
```
./gradlew clean deployNodes
```
and to run
```
./build/nodes/runnodes
```

### Interacting with the nodes
We will interact with the nodes via their specific shells. When the nodes are up and running, use the following command 
to login to the node shell.

```
# find the ssh port for PartyA using docker ps
ssh user1@0.0.0.0 -p 2222

# the password defined in the node config, default is "test"
Password: test
```

You should be able to login to the node shell now. Use the below command to send an order to the exchange.
```
start OrderFlow instrument: TCS, transactionType: BUY, quantity: 150, price: 1000, exchange: Exchange
```
You can validate the the orders have only be recorded at the Broker's end usin the
below vaultQuery command.

```
run vaultQuery contractStateType: net.corda.samples.trade.states.Order
```

Login to Broker B using the same process as for Broker A (ssh port is 2223)
and send a corresponding sell order using the below command.

```
start OrderFlow instrument: TCS, transactionType: SELL, quantity: 100, price: 1000, exchange: Exchange
```

The orders will be automatically matched in the enclave at the exchange and a matched
trade will be recorded in brokers and exchange's vault. Query the vault using the below 
command:


```
run vaultQuery contractStateType: net.corda.samples.trade.states.Trade
```

Notice that the order at Broker A is partially executed only 100 of the 150 quantity 
has been matched.

Hence the orders are confidentially send to the Exchange's hosted enclave while once an
order has been matched the trade becomes publically available.