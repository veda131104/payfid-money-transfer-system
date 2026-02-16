import { HttpClient } from '@angular/common/http';
import { Injectable, Inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { Observable, tap } from 'rxjs';

export interface SignupPayload {
  name: string;
  email: string;
  password: string;
}

export interface LoginPayload {
  name: string;
  password: string;
  rememberMe?: boolean;
}

export interface AuthResponse {
  name: string;
  rememberToken?: string;
  firstLogin?: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly baseUrl = 'http://localhost:8080/api/v1/auth';
  private readonly sessionKey = 'auth_session';

  constructor(
    private readonly http: HttpClient,
    @Inject(PLATFORM_ID) private platformId: Object
  ) { }

  private isBrowser(): boolean {
    return isPlatformBrowser(this.platformId);
  }

  signup(payload: SignupPayload): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.baseUrl}/signup`, payload)
      .pipe(tap(response => this.saveSession(response)));
  }

  login(payload: LoginPayload): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.baseUrl}/login`, payload)
      .pipe(tap(response => this.saveSession(response)));
  }

  forgotPassword(username: string): Observable<any> {
    return this.http.post(`${this.baseUrl}/forgot-password`, { name: username });
  }

  loginWithToken(token: string): Observable<AuthResponse> {
    return this.http
      .get<AuthResponse>(`${this.baseUrl}/verify-token?token=${token}`)
      .pipe(tap(response => this.saveSession(response)));
  }

  resetPassword(payload: { token: string; newPassword: String }): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.baseUrl}/reset-password`, payload)
      .pipe(tap(response => this.saveSession(response)));
  }

  getCredentialsByToken(token: string): Observable<any> {
    return this.http.get(`${this.baseUrl}/remember-me?token=${token}`);
  }

  clearSession(): void {
    if (!this.isBrowser()) {
      return;
    }

    localStorage.removeItem(this.sessionKey);
  }

  getCurrentUser(): AuthResponse | null {
    if (!this.isBrowser()) {
      return null;
    }

    try {
      const session = localStorage.getItem(this.sessionKey);
      return session ? JSON.parse(session) : null;
    } catch (e) {
      return null;
    }
  }

  private saveSession(session: AuthResponse): void {
    if (!this.isBrowser()) {
      return;
    }

    localStorage.setItem(
      this.sessionKey,
      JSON.stringify({ ...session, loggedInAt: new Date().toISOString() })
    );
  }
}
