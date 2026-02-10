import { Injectable } from '@angular/core';

export interface SignupPayload {
  name: string;
  email: string;
  password: string;
}

export interface LoginPayload {
  email: string;
  password: string;
}

export interface AuthUser {
  name: string;
  email: string;
  password: string;
  createdAt: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly usersKey = 'auth_users';
  private readonly sessionKey = 'auth_session';

  signup(payload: SignupPayload): boolean {
    const users = this.loadUsers();
    const email = payload.email.trim().toLowerCase();

    const exists = users.some(user => user.email.toLowerCase() === email);
    if (exists) {
      return false;
    }

    const user: AuthUser = {
      name: payload.name.trim(),
      email: payload.email.trim(),
      password: payload.password,
      createdAt: new Date().toISOString()
    };

    users.push(user);
    this.saveUsers(users);
    return true;
  }

  login(payload: LoginPayload): boolean {
    const users = this.loadUsers();
    const email = payload.email.trim().toLowerCase();
    const user = users.find(candidate => candidate.email.toLowerCase() === email);
    if (!user) {
      return false;
    }

    if (user.password !== payload.password) {
      return false;
    }

    this.saveSession({ name: user.name, email: user.email });
    return true;
  }

  private loadUsers(): AuthUser[] {
    if (typeof localStorage === 'undefined') {
      return [];
    }

    const stored = localStorage.getItem(this.usersKey);
    if (!stored) {
      return [];
    }

    try {
      return JSON.parse(stored) as AuthUser[];
    } catch {
      return [];
    }
  }

  private saveUsers(users: AuthUser[]): void {
    if (typeof localStorage === 'undefined') {
      return;
    }

    localStorage.setItem(this.usersKey, JSON.stringify(users));
  }

  private saveSession(session: { name: string; email: string }): void {
    if (typeof localStorage === 'undefined') {
      return;
    }

    localStorage.setItem(
      this.sessionKey,
      JSON.stringify({ ...session, loggedInAt: new Date().toISOString() })
    );
  }
}
