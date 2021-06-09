## PSI - Private Set Intersection Sample

This is a simple app using the Conclave API. It is licensed under the Apache 2 license, and therefore you may 
copy/paste it to act as the basis of your own commercial or open source apps.

Using this sample will see how Conclave (based on Intel SGX) can be a new tool to solve the private set intersection problem (PSI)

# What is Private Set Intersection Problem (PSI) ?

PSI refers to the problem of determining the common elements from the intersection of two sets without leaking or disclosing any 
additional information of the remaining elements of the either sets.

# Use Case : Measuring ad conversion rates

Measuring ad conversion rates is done by comparing the list of people who have seen an ad with those who have completed a transaction. 
These lists are held by the advertiser (say, Google or Facebook), and by merchants, respectively. 
It is often possible to identify users on both ends, using identifiers such as credit card numbers, email addresses, etc. 
A simple solution, which ignores privacy, is for one side to disclose its list of customers to the other side, which then computes the necessary statistics. 

# Parties involved in the sample
Client1, Client2, Host.

Client1 - a Merchant who has a list of users who have purchased their product.  
Client2 - Service Provider like Facebook, Google who publishes ads on its website.

Client1 wants to calculate the ad conversion rate. Client1 usually tends to share the list of converted user details to Client2.
Client2 has list of users who has clicked on the ad. Client2 performs an intersection of these lists on its side to check which 
converted users had clicked the ad.

For simplicity, in this sample clients pass in following data to enclave - 
Client1 - Merchant passes credit card numbers of all users who have made a purchase.
Client2 - Service Provider passes credit card numbers of all users who have clicked the ad.

# How to run

Start the host on a non-Linux system, which will build the enclave and host:

```
./container-gradle host:run
```

Once the host starts, pass in the credit card numbers via Client1

    ./container-gradle client:run --args="MERCHANT 1123 4456 777998 88988898"


Start Client2 and pass in the credit card numbers of users who have clicked the ad

    ./container-gradle client:run --args="SERVICE-PROVIDER 88988898 77879 00989"

Once both the clients pass in the credit card numbers, the host calculates the ad conversion rate within the enclave and sends it to both the clients.

For more information on PSI, kindly take a look at the below blog

https://medium.com/p/abbd3413567d/edit //TODO change the link once blog is published