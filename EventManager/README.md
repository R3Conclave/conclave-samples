Event Manager Conclave Demonstrator
===================================

NOTE
----

This project is a 'hobby' project by Richard G Brown created for my own self-education. It is based on the R3 'internal-training' project by Shams Asari. It is NOT intended to be an exemplar of best practice and it does not come with any support!

Introduction
------------

Event Manager implements the idea of an enclave that can simultaneously host a collection of 'Computations'. These computations are initiated by a Conclave client, have a name and a type, have a defined set of 'participants' and a 'quorum' that must be reached before a result (or results) can be obtained. Supported types are average (the average of the submissions is returned), maximum and minimum (the _identity_ of the submitter of the highest or lowest value is returned) and "key matcher" (described below)

The idea is that an enclave of this type could be used to host simple one-off multi-party computations (eg five employees wanting to know the average of their salaries, or ten firms wanting to know who had the lowest sales last month without revealing what that figure was). 

The idea is further extended by also incorporating the idea of 'key matching', whereby participants can submit (arbitrary String-valued) 'keys' and associated summary messages, and subsequently receive a stream of messages if/when sufficient _other_ participants have also signalled interest in the same key. This mode of operation is intended to support demonstrations such as one where insurers could submit the 'VINs' of cars for which they've received claims, to a computation instance of quorum 2, so that should a _second_ insurer report a claim for the same vehicle, both insurers would be informed.

The comments in me/gendal/conclave/eventmanager/enclave/EventManagerEnclave.kt explain more.

The project also includes a command-line application for interacting with the enclave via a simple Spring Boot host server. Both of these are also based on Shams's sample.  The host is mostly unchanged but the client has been upgraded in three key ways.

First, the client can be compiled to native (see below). Secondly, picocli has been integrated to make its use more intuitive. Thirdly, the identity persistence logic has been tweaked to make it possible to map between 'easy names' (eg Alice) and pubkeys. This is purely a convenience for demos.

Running
-------

The project is set up with a default of 'mock' mode so that it can be used immediately without any SGX hardware or need for a native-image build.  To run a simple example, open two terminals and change to the root directory for this project. The following instructions have been tested only on Mac, and probably only work on Mac:

1. In the first terminal, start the host: `./gradlew host:run`. The host will start listening on port 9999.
2. In the second terminal, build and package the client: `./gradlew client:shadowJar`
3. In the same terminal, change to the client subdirectory
4. Execute each of the scripts prefixed with 1 to 5 in order. 
    * The first script (`1.reset-client.sh`) will create a set of client identities ready for the following steps. This includes the creation of a set of `.conclave` files. These are read by any client upon startup in order to learn the 'easy' name of other clients running on the same machine (useful for demos)
    * `2.create-calcs.sh` will create several Computation instances on the enclave. These computations are all 'convened' by "Alice" but, as the third computation in that file shows, the creator of a computation does not necessarily need to be a participant
    * `3.make-submissions.sh` submits a series of values to the Computations, under various identities. Some of the submissions are invalid in order to demonstrate what happens in such cases.
    * `4.get-results.sh` iterates through the participants and retrieves any results they are entitled to see
    * Finally, `5.clean-up.sh` deletes the identity files
    
To run the host in any other mode, add `-PenclaveMode=[mock|simulation|debug|Release]` to the end of the gradlew command line in the usual manner.  If running on a Mac, you can use `container-gradle`, also in the usual way.  

Note that the host has been configured by default to listen on port 9999 (versus the Spring Boot default of 8080) in order to be compatible with the default in the container-gradle script.  If connecting to a non mock-mode enclave you will need to ensure the enclave constraint used by the client matches the enclave that is actually running.  See `EventManagerClient.properties` to see how the client constraint is set.

To run the client in native mode, execute `./gradlew client:nativeImage` from the project's root folder. You can edit the `event-client.sh` script in the client folder to make the new native image the default for the demo scripts.  If the native build of the client fails, see the notes in `configure-native-image-build.sh`
    
PRs, comments, issues welcome, as are emails to richard@r3.com

A note on security
------------------

This project has NOT been security reviewed. Our intent in making it available publicly is to help newcomers to the Conclave platform get an interesting sample product up and running as a precursor to exploring the platform and learning more. As a result, simplicity and didacticism are emphasised over the production-readiness of the sample. 

So, once you reach the point that your code is running, we advise you to think deeply about the various ways an adversary could break or subvert the application. Adversarial thinking is (and must be) at the heart of any enclave development project and you should start as early as possible in your journey. 

For example, this application has a 'lock' feature that is intended to ensure that all participants receive the same 'answer' for any calculation once it reaches quorum. However, think what happens if an adversarial host restarts the enclave after one party has received a result and then replays a stream of messages that omits one party's submission. Provided there were more submissions than needed to achieve quorum, the next party to request a result may get an answer that differs to the previous party's answer, subverting the 'lock' concept. Do we care about this? Perhaps it seems OK if different recipients get different results, provided the results are correct for the inputs from which they were derived. But now observe that a motivated adversarial host can do this as many times as they like. Does this attack give them the ability to deduce the contents of any given input to the 'average' calculation? It probably does. So it can be instructional to think through for yourself how you might prevent this attack.  For example, does your scenario need the concept of a session, that is invalidated upon enclave restart? 

Similarly, think about how request/response interactions have been modelled here. If an adversarial host were to replay messages, could it trick the enclave into thinking it had sent a particular response to a client when, in fact, the client had long-since received its response (from a previous execution of the enclave) and so was completely unaware that a new, and potentially different, response had now been generated?

R3 is working on an exemplar host process (and associated model for client interactions with enclaves) but you don't need to wait for that: get into the habit of thinking like the most paranoid person in the world! Imagine all the things a bad actor could do to ruin your day - or that of your customers - and you'll be well on your way to being a superstar confidential computing developer!  

See more about security here: https://docs.conclave.net/security.html
