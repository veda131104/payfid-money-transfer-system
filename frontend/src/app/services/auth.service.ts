import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable, Inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { Observable, tap, BehaviorSubject } from 'rxjs';

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
  firstLogin?: boolean;
  refreshToken?: string;
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
  private readonly refreshTokenKey = 'auth_refresh_token';
  private readonly publicKeyKey = 'auth_public_key';

  private tokenSubject = new BehaviorSubject<string | null>(null);
  token$ = this.tokenSubject.asObservable();

  constructor(
    private readonly http: HttpClient,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {
    const token = this.getToken();
    if (token) {
      this.tokenSubject.next(token);
    }
  }

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
        if (response.refreshToken) {
          this.saveRefreshToken(response.refreshToken);
        }
        this.saveSession(response);
      }));
  }

  getToken(): string | null {
    if (!this.isBrowser()) return null;
    return localStorage.getItem(this.tokenKey);
  }

  getRefreshToken(): string | null {
    if (!this.isBrowser()) return null;
    return localStorage.getItem(this.refreshTokenKey);
  }

  private saveToken(token: string): void {
    if (!this.isBrowser()) return;
    localStorage.setItem(this.tokenKey, token);
    this.tokenSubject.next(token);
  }

  private saveRefreshToken(token: string): void {
    if (!this.isBrowser()) return;
    localStorage.setItem(this.refreshTokenKey, token);
  }

  clearSession(): void {
    if (!this.isBrowser()) return;
    localStorage.removeItem(this.sessionKey);
    localStorage.removeItem(this.tokenKey);
    localStorage.removeItem(this.refreshTokenKey);
    localStorage.removeItem(this.publicKeyKey);
    this.tokenSubject.next(null);
  }

  getCurrentUser(): AuthResponse | null {
    if (!this.isBrowser()) return null;
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
    if (!this.isBrowser()) return;
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
        if (response.refreshToken) {
          this.saveRefreshToken(response.refreshToken);
        }
        this.saveSession(response);
      }));
  }

  refreshToken(): Observable<any> {
    const refreshToken = this.getRefreshToken();
    return this.http.post<any>(`${this.baseUrl}/refresh`, { refreshToken }).pipe(
      tap(response => {
        this.saveToken(response.token);
        if (response.refreshToken) {
          this.saveRefreshToken(response.refreshToken);
        }
      })
    );
  }

  getPublicKey(): Observable<{ publicKey: string }> {
    return this.http.get<{ publicKey: string }>(`${this.baseUrl}/public-key`);
  }

  getCredentialsByToken(token: string): Observable<CredentialsResponse> {
    return this.http.get<CredentialsResponse>(`${this.baseUrl}/credentials/${token}`);
  }

  forgotPassword(username: string): Observable<ForgotPasswordResponse> {
    return this.http.post<ForgotPasswordResponse>(
      `${this.baseUrl}/forgot-password`,
      { name: username }
    );
  }

  resetPassword(request: ResetPasswordRequest): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/reset-password`, request);
  }
}
