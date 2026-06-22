import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { encrypt, decrypt } from '../utils/crypto';

@Injectable({ providedIn: 'root' })
export class AccountSetupService {
  private base = 'http://localhost:8080/api/v1/account-setup';

  constructor(private readonly http: HttpClient) { }

  private decryptSensitiveFields(res: any): any {
    if (!res) return res;
    return {
      ...res,
      creditCardNumber: decrypt(res.creditCardNumber),
      cvv: decrypt(res.cvv),
      expiryDate: decrypt(res.expiryDate),
      pin: decrypt(res.pin)
    };
  }

  create(payload: any): Observable<any> {
    const encryptedPayload = {
      ...payload,
      creditCardNumber: encrypt(payload.creditCardNumber),
      cvv: encrypt(payload.cvv),
      expiryDate: encrypt(payload.expiryDate)
    };
    return this.http.post(this.base, encryptedPayload);
  }

  sendOtp(payload: { contact: string }): Observable<any> {
    return this.http.post(this.base + '/send-otp', payload);
  }

  verifyOtp(payload: { contact: string; otp: string }): Observable<any> {
    return this.http.post(this.base + '/verify-otp', payload);
  }

  setupUpi(id: number, upiId: string): Observable<any> {
    return this.http.post(`${this.base}/${id}/upi`, { upiId });
  }

  getAccountByUser(userName: string): Observable<any> {
    return this.http.get(`${this.base}/user/${userName}`).pipe(
      map(res => this.decryptSensitiveFields(res))
    );
  }

  updateAccount(userName: string, payload: any): Observable<any> {
    const encryptedPayload = {
      ...payload,
      creditCardNumber: encrypt(payload.creditCardNumber),
      cvv: encrypt(payload.cvv),
      expiryDate: encrypt(payload.expiryDate)
    };
    return this.http.put(`${this.base}/user/${userName}`, encryptedPayload).pipe(
      map(res => this.decryptSensitiveFields(res))
    );
  }

  getAccountByNumber(accountNumber: string): Observable<any> {
    return this.http.get(`http://localhost:8080/api/v1/accounts/number/${accountNumber}`);
  }

  setPin(userName: string, pin: string): Observable<any> {
    return this.http.post(`${this.base}/user/${userName}/pin`, { pin: encrypt(pin) }).pipe(
      map(res => this.decryptSensitiveFields(res))
    );
  }
}
