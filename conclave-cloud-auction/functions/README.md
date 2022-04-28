# Conclave Auction Backend Functions using Conclave Cloud
This project implements a module in typescript that is transpiled to JavaScript
and uploaded as a set of Conclave Functions serverless functions which provide
the privacy-preserving backend for the ConclaveAuction demonstration.

The project gives a demonstration of a non-trivial implementation that depends
on a number of standard packages, installed using npm.

The configuration of the project in `package.json`, `tsconfig.json` and
`webpack.config.js` along with the `test/template` directory can serve as a
template for your own projects targetting Conclave Cloud Functions.

## Layout
The project consists of a number of directories:

| Directory | Description |
| --------  | ----------- |
| build     | The directory containing the final JavaScript file that should be uploaded to Conclave Cloud. |
| node_modules | Dependent node modules. Generated during installation. |
| src       | The typescript source code for the module. |
| test      | Code to test the module. |
| test/template | Conclave Functions require some global objects and functions that are not normally provided in a CommonJS or ES6 module.This template is used to polyfill these into the test environment. |
| test_run  | Test build output folder. |


## Preparing the source code
The source code in `src/index.ts` contains the code for all of the functions
that we want to upload. This has been written to include a class encompassing
all functionality then the functions themselves provided by creating an instance
of the class end exporting functions that invoke the instance (at the bottom of
the file).

The implementation uses an untrusted backend service to store and retrieve an
encrypted database for each user. This can be provided using the demonstration
`backend` project which must be built and hosted on an IP address available on
the public Internet. Once the service is hosted, `src/index.ts` needs to be
modified so the `serviceURL` points to the hosted service:

```typescript
const serviceURL = "http://20.124.9.121:8080";
```

## Building
The package should be cloned into a new directory, then the dependencies
installed with:

```
npm install
```

The package can then be built using:

```
npm run build
```

The tests can be run with:

```
npm run test
```

## Uploading the functions
Once the package has been built it needs to be uploaded to a Conclave Cloud
project. This is achieved using the Conclave Cloud CLI.

Firstly log in to the Conclave Cloud project:

```
ccl login
```

Create a project if you do not already have one and set it as default for the CLI:

```
ccl projects create --name ConclaveAuction
ccl save --project [the ID of the project that was just created]
```

The `package.json` file includes a script that uploads all of the function code.
This can be executed with:

```
npm run upload
```

Alternatively you can upload each function individually.

```
ccl functions upload --code ./build/main.bundle.js --entry=addBid --name addBid
ccl functions upload --code ./build/main.bundle.js --entry=calculateBidWinner --name calculateBidWinner
```

## Implementation Notes
All Conclave Cloud Functions modules must export the function entry points in a
global object named `cclexports```. For example:

```javascript
cclexports = {
    myFunction: () => {
        return "Hello";
    }
}
```

This package hides that from the source code implementation by providing some
configuration in `webpack.config.js`.

```javascript
    output: {
        path: path.resolve(__dirname, 'build'),
        filename: 'main.bundle.js',
        library: 'cclexports'
    },
```

By providing the line `library: 'cclexports'`, webpack automatically populates
all exported items into the global `cclexports` object.

## Remember this is a demonstration!!!
This is just a demonstration. As such, errors may not be handled correctly and
providing invalid parameters may render a database unusable, requiring it to be
cleared and reset. Please feel free to expand the demonstration project and
contribute back to this repository to improve the capabilities/error handling.

