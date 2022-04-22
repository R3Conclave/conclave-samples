const crypto = {
    getProjectKey: () => {
        return "key";
    },
    getRandomValues: () => {
        return new Uint32Array(1);
    }
}

globalThis.crypto = crypto;
