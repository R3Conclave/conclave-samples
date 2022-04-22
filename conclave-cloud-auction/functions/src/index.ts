import {AES, enc, lib} from "crypto-js";

const serviceURL = "http://20.219.140.78:8080";

interface BidEntry {
    username: string;
    bid: string;
}

interface BidDatabase {
    bids: Array<BidEntry>;
}

const logs = new Array<string>()


class ConclavePass {
    async addBid(entry: BidEntry) {
        const pd = await this.getDatabase();
        // Add the new entry.
        pd.bids.push(entry);
        // Send it back to the data store.
        await this.putDatabase(pd);
    }

    async calculateBidWinner(): Promise<BidEntry> {
        const pd = await this.getDatabase();

        let found = null;
        let max = 0;

        pd.bids.forEach(pe => {
            if (Number(pe.bid) > max) {
                found = pe;
                max = Number(pe.bid);
            }
        });
    return found;
    }

    private async getDatabase(): Promise<BidDatabase> {
        const response = await fetch(`${serviceURL}/passwords`);
        const encryptedDB = await response.text()
        return this.decryptDatabase(encryptedDB)
    }

    private async putDatabase(db: BidDatabase) {
        //logs.push("putDatabase()");

        const encryptedDB = this.encryptDatabase(db);
        //logs.push("encryptedDB");
        try {
            const res = await fetch(`${serviceURL}/passwords`, {
                method: "POST",
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ encryptedDB: encryptedDB })
            });
            //logs.push("Service called: " + JSON.stringify(res));
        } catch (e) {
            //logs.push("Exception");
        }
    }

    private decryptDatabase(db: string): BidDatabase {
        // If we have been given a database then decrypt it.
        if (db) {
            const decryptedDb = AES.decrypt(db, crypto.getProjectKey()).toString(enc.Utf8);
            return JSON.parse(decryptedDb);
        } else {
            // If we haven't got a password database then create a new one
            return { bids: new Array<BidEntry>() };
        }
    }

    private encryptDatabase(pd: BidDatabase): string {
        const encrypted = AES.encrypt(JSON.stringify(pd), crypto.getProjectKey());
        return encrypted.toString()
    }
}

export async function addBid(entry: BidEntry): Promise<string> {
    await new ConclavePass().addBid(entry);
    //return JSON.stringify(logs);
    return "ok"
}

export async function calculateBidWinner(extra: string): Promise<BidEntry> {
    return await new ConclavePass().calculateBidWinner();
}
