// fetch() is not available when running tests so mock it here. This code is
// currently incomplete so the tests cannot be used for now.
const database = new Map();

async function fetch(url, options) {
    if (options && options.method === "POST") {
        database.set[url] = JSON.parse(options.body).encryptedDB
    } else {
        const db = database.get[url];
    }
    // This function needs to return a promise containing a response object.
}

async function runtests() {
    console.log("Add a password.");
    console.log(await cclexports.addEntry("mytoken", "mypassword", {
        url: "https://google.com",
        description: "Google",
        username: "roy.hopkins@r3.com",
        password: "reallystrongpassword"
    }));

    console.log("Add a second password.");
    console.log(await cclexports.addEntry("mytoken", "mypassword", {
        url: "https://google2.com",
        description: "Google2",
        username: "roy.hopkins@r3.com",
        password: "reallystrongpassword2"
    }));

    console.log("Query all.");
    console.log(await cclexports.query("mytoken", "mypassword", ""));

    console.log("Query.");
    console.log(await cclexports.query("mytoken", "mypassword", "google2"));

    console.log("Get.");
    console.log(await cclexports.get("mytoken", "mypassword", "https://google.com"));

    console.log("Remove.");
    console.log(await cclexports.remove("mytoken", "mypassword", "https://google2.com"));
    console.log(await cclexports.query("mytoken", "mypassword", null));
}

runtests().then(() => {
    console.log("Tests complete.");
});

