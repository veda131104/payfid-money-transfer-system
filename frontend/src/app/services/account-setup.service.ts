import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class AccountSetupService {
  private base = 'http://localhost:8080/api/v1/account-setup';

  constructor(private readonly http: HttpClient) { }

  create(payload: any): Observable<any> {
    return this.http.post(this.base, payload);
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
    return this.http.get(`${this.base}/user/${userName}`);
  }

  updateAccount(userName: string, payload: any): Observable<any> {
    return this.http.put(`${this.base}/user/${userName}`, payload);
  }

  getAccountByNumber(accountNumber: string): Observable<any> {
    return this.http.get(`http://localhost:8080/api/v1/accounts/number/${accountNumber}`);
  }

  setPin(userName: string, pin: string): Observable<any> {
    return this.http.post(`${this.base}/user/${userName}/pin`, { pin });
  }
}
