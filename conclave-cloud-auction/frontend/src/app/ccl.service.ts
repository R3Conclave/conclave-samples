import { Injectable } from '@angular/core';
import { Conclave } from 'conclave-cloud-sdk';
import { AuthService } from './auth.service';

const tenantID = "T5A4E8A64A4ED22176918FA66FE88746332F904408A246019CB4903F7905A0AF";
const projectID = "302360f0f9fcee46441beea2e1037d18d03e2a03dea107d46a11b40946f03fa1";
const queryHash = "AD7635832EDC36AF1ED9C38E0FD57F5E056AD0215F86A7E5BACCB330A32D8E40";
const addEntryHash = "DD442ADA05F2469E3AA6F10A33A3914FF13C7318AFC9713D5BDCA8C3AD8C26DF";
const getHash = "3CFC30B748EF865D518C4C03109375AB91EC355F53ED5166C09197BF7540D9BD";
const removeHash = "CB5A3C5EC607A4FD980014D10E08015E8664ECB70607DA01A92B9C9AD4EDF681";
const calculateBidWinnerHash = "2BFBB0233F243D606796965B735940E721CBD0D68E6EE880E5F17E0725D727FA";
const addBidHash = "6ADEA2D29766AA3447C45FDE3E15BDCFF7B5587C0196DF1DE745785CCAD93DAA";

export interface PasswordEntry {
  url: string;
  description: string;
  username: string;
  password: string;
}

export interface BidEntry {
  email: string;
  bid: string;
}

@Injectable({
  providedIn: 'root'
})
export class CclService {

  private conclave: Conclave

  constructor(
    private auth: AuthService
  ) {
    this.conclave = Conclave.create({
      tenantID: tenantID,
      projectID: projectID
    });

  }

  public async getPasswords(): Promise<PasswordEntry[]> {
    const userToken = await this.auth.userToken();
    console.log("Querying passwords from CCL with user token: " + userToken);
    const result = await this.conclave.functions.call('query', queryHash, [userToken, this.auth.password, '']);
    return result.return;
  }

  public async getPassword(url: string): Promise<PasswordEntry> {
    const userToken = await this.auth.userToken();
    console.log(`Querying password for ${url} from CCL with user token: ` + userToken);
    const result = await this.conclave.functions.call('get', getHash, [userToken, this.auth.password, url]);
    return result.return;
  }

  public async addPassword(url: string, description: string, username: string, password: string) {
    const userToken = await this.auth.userToken();
    console.log("Adding password with user token: " + userToken);
    const entry: PasswordEntry = {
      url, description, username, password
    }
    const result = await this.conclave.functions.call('addEntry', addEntryHash, [userToken, this.auth.password, entry]);
    console.log(result);
  }

  public async deletePassword(url: string) {
    const userToken = await this.auth.userToken();
    console.log(`Querying password for ${url} from CCL with user token: ` + userToken);
    const result = await this.conclave.functions.call('remove', removeHash, [userToken, this.auth.password, url]);
    return result.return;
  }

  public async addBid(email: string, bid: string) {
      const userToken = await this.auth.userToken();
      console.log("Adding bid with user token: " + userToken);
      const entry: BidEntry = {
        email, bid
      }
      console.log("entry : " + entry.email)
      const result = await this.conclave.functions.call('addBid', addBidHash, [entry]);
      console.log(result);
    }

  public async calculateBidWinner() : Promise<BidEntry> {
        const result = await this.conclave.functions.call('calculateBidWinner', calculateBidWinnerHash, [""]);
        console.log("calculateBidWinner result is - " + result.return);
        return result.return;
      }

}
