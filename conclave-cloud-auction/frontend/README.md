# Conclave Pass Frontend- Password Manager implemented using Conclave Cloud
This project implements a frontend using Angular to demonstrate how a
web-browser application can directly access services in Conclave Cloud.

The frontend provides a user interface with the following capabilities,
implemented using a Conclave Functions backend.

* Login/Logout using username and password.
* List password entries for the logged in user.
* Get the actual password for an entry.
* Remove a password entry.

## Building
The Conclave Cloud JavaScript SDK npm package is not currently hosted in a
public repository. You need to download the package containing the SDK, unzip it
then update `package.json` in this project to point to it.

```json
    "conclave-cloud-sdk": "file:../../../conclave-cloud-sdk-js",
```

Once configured, the project can be built and started using a local webserver using:

```
npm install
npm start
```

This will start a web server to host the frontend on `http://localhost:4200`.

## Implementation notes
### Conclave Cloud/Functions Implementation
All of the code that interacts with Conclave Cloud can be found in the file
`src/app/ccl.service.ts`. This module shows how to connect to the Conclave Cloud
service and to prepare and execute function invocations as well as how to parse
the response that comes back from the invoked JavaScript code.

The top of the module contains a number of strings and hashes. These need to
match the values in your environment.

Once you have created an account in Conclave Cloud, you then need to create a
project and upload the functions that are referenced in this module. See the
main demonstration README.md for more details.

Once you have done this you need to find your tenant ID, project ID and the
hashes of the four functions that are called in the module. Update the values in
the module to match these.

The remainder of the project is a basic demonstration project using Angular +
Material to provide a login page, a list of password entries for the logged in
user and the ability to add, delete and query password entries. Clicking on an
entry in the password list will call the Conclave Functions backend to retrieve
the password for the selected entry.

### Polyfills
The project includes some polyfills that are required when working with the
Conclave Cloud SDK. This may change in the future, but at present the SDK
requires that the environment support a global node-style `Buffer` and a
`process` object that provides an environment where `NODE_DEBUG=undefined`.

These requirements were met by including the following in `polyfills.ts`:

```typescript
 (window as any).global = window;
 (window as any).process = {
     env: { NODE_DEBUG: undefined },
 };
 // @ts-ignore
 (window as any).Buffer = (window as any).Buffer || require('buffer').Buffer;
 ```

### Remember this is a demonstration!!!
This is just a demonstration. As such, errors may not be handled correctly and
providing invalid parameters may render a database unusable, requiring it to be
cleared and reset. Please feel free to expand the demonstration project and
contribute back to this repository to improve the capabilities/error handling.
