import { HttpClient, HttpHeaders } from '@angular/common/http';
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
  email: string;
}

export interface LoginResponse {
  userId: number;
  name: string;
  email: string;
  token: string;
  tokenType: string;
  rememberToken?: string;
}

export interface CredentialsResponse {
  name: string;
  email: string;
  password: string;
}

export interface ForgotPasswordRequest {
  username: string;
}

export interface ResetPasswordRequest {
  token: string;
  newPassword: string;
}

export interface ForgotPasswordResponse {
  message: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly baseUrl = 'http://localhost:8080/api/v1/auth';
  private readonly sessionKey = 'auth_session';
  private readonly tokenKey = 'auth_token';

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

  login(payload: LoginPayload): Observable<LoginResponse> {
    return this.http
      .post<LoginResponse>(`${this.baseUrl}/login`, payload)
      .pipe(tap(response => {
        this.saveToken(response.token);
        this.saveSession(response);
      }));
  }

  // Get JWT token
  getToken(): string | null {
    if (!this.isBrowser()) {
      return null;
    }
    return localStorage.getItem(this.tokenKey);
  }

  // Save JWT token
  private saveToken(token: string): void {
    if (!this.isBrowser()) {
      return;
    }
    localStorage.setItem(this.tokenKey, token);
  }

  clearSession(): void {
    if (!this.isBrowser()) {
      return;
    }

    localStorage.removeItem(this.sessionKey);
    localStorage.removeItem(this.tokenKey);
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

  isLoggedIn(): boolean {
    return this.getToken() !== null && this.getCurrentUser() !== null;
  }

  private saveSession(session: any): void {
    if (!this.isBrowser()) {
      return;
    }

    localStorage.setItem(
      this.sessionKey,
      JSON.stringify({ 
        userId: session.userId,
        name: session.name, 
        email: session.email, 
        loggedInAt: new Date().toISOString() 
      })
    );
  }

  loginWithToken(token: string): Observable<LoginResponse> {
    return this.http
      .post<LoginResponse>(`${this.baseUrl}/login-with-token`, { token })
      .pipe(tap(response => {
        this.saveToken(response.token);
        this.saveSession(response);
      }));
  }

  getCredentialsByToken(token: string): Observable<CredentialsResponse> {
    return this.http.get<CredentialsResponse>(
      `${this.baseUrl}/credentials/${token}`
    );
  }

  forgotPassword(username: string): Observable<ForgotPasswordResponse> {
    return this.http.post<ForgotPasswordResponse>(
      `${this.baseUrl}/forgot-password`,
      { username }
    );
  }

  resetPassword(request: ResetPasswordRequest): Observable<any> {
    return this.http.post<any>(
      `${this.baseUrl}/reset-password`,
      request
    );
  }
}

