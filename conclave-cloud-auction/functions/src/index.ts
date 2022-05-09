import {AES, enc, lib} from "crypto-js";

const serviceURL = "http://20.219.140.78:8080";

interface BidEntry {
    username: string;
    bid: string;
}

// Bids array structure which will we will encrypt before saving to the db
interface BidDatabase {
    bids: Array<BidEntry>;
}

const logs = new Array<string>()

/**
 * This class defines the methods which should be uploaded to the Conclave Cloud Platform. The user can use the
 * ccl tool to upload these functions to the cloud platform.
 */
class ConclaveAuction {

    // Add bid function will be called to enter a bid to the database
    async addBid(entry: BidEntry) {
        const pd = await this.getDatabase();
        // Add the new entry.
        pd.bids.push(entry);
        // Send it back to the data store.
        await this.putDatabase(pd);
    }

    // CalculateBidWinner will query the database, loop over all the bids and return the bid winner
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

    // This method hits the database service, decrypts the data and returns it back
    private async getDatabase(): Promise<BidDatabase> {
        const response = await fetch(`${serviceURL}/bids`);
        const encryptedDB = await response.text()
        return this.decryptDatabase(encryptedDB)
    }

    // This method will take the bid entry, encrypt this and will push it to the database
    private async putDatabase(db: BidDatabase) {

        const encryptedDB = this.encryptDatabase(db);
        try {
            const res = await fetch(`${serviceURL}/bids`, {
                method: "POST",
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ encryptedDB: encryptedDB })
            });
        } catch (e) {
            logs.push("Exception");
        }
    }

    // This method will decrypt the database bid entries using project key
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

    // This method will encrypt the database bid entries using project key
    private encryptDatabase(pd: BidDatabase): string {
        const encrypted = AES.encrypt(JSON.stringify(pd), crypto.getProjectKey());
        return encrypted.toString()
    }
}

// Exporting addBid function such that addBid function is available to be called by the Conclave Cloud Platform
export async function addBid(entry: BidEntry): Promise<string> {
    await new ConclaveAuction().addBid(entry);
    //return JSON.stringify(logs);
    return "ok"
}

// Exporting addBid function such that calculateBidWinner function is available to be called by the Conclave Cloud Platform
export async function calculateBidWinner(extra: string): Promise<BidEntry> {
    return await new ConclaveAuction().calculateBidWinner();
}
