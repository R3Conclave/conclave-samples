# Conclave Auction Backend - Backend database storage for a Auction Application implemented using Conclave Cloud

This project contains a very simple Spring Boot service that has two endpoints
that allow Conclave Functions to persist auction bids databases for individual
users.

The service is only intended as a demo, thus has no authentication or error
checking and only persists the user data in memory. Restarting the service
clears the database.

The service listens on port 8080.

## How to build and run the backend service
The service can be built using:

```
./gradlew build
```

Once built the service can be run with:

```
java -jar build/libs/conclaveauction-0.0.1-SNAPSHOT.jar
```

## Endpoints
`POST /bids`

```
request body = {
    "encryptedDB": "[base64 encoded string containing encrypted database]"
}
```

This endpoint is used to set the encrypted database for all the users who have entered their bids.
This encrypted db is an Array of BidEntry class. Each row contains the logged in user's email and the bid amount entered by him.

`GET /bids`

```
response body = {
    "[base64 encoded string containing encrypted database]"
}
```

This endpoint is used to get the encrypted database.
