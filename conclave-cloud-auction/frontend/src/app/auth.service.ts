import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private _email: string = "a_user@conclave.cloud"
  private _password: string = "12345678"

  constructor() { }

  public login(email: string, password: string) {
    this._email = email;
    this._password = password;
  }

  public logout() {
    this._email = "";
    this._password = "";
  }

  public isLoggedIn(): boolean {
    return (this._email.length > 0) && (this._password.length > 0)
  }

  public async userToken(): Promise<string> {
    if (!this.isLoggedIn()) {
      return "";
    }
    const encoder = new TextEncoder();
    const data = encoder.encode(this._email);
    const hash = await crypto.subtle.digest("SHA-256", data);
    const hashArray = Array.from(new Uint8Array(hash));
    const hashHex = hashArray.map(b => b.toString(16).padStart(2, '0')).join('');
    return hashHex;  
  }

  public get email() {
    return this._email
  }

  public get password() {
    return this._password
  }

}
