import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import { AuthResponse, AuthUser, RoleName } from './auth.models';

const TOKEN_KEY = 'finshield.access-token';
const USER_KEY = 'finshield.user';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly tokenState = signal<string | null>(sessionStorage.getItem(TOKEN_KEY));
  private readonly userState = signal<AuthUser | null>(this.restoreUser());

  readonly currentUser = this.userState.asReadonly();
  readonly authenticated = computed(() =>
    Boolean(this.userState() && this.isTokenUsable(this.tokenState())),
  );

  get accessToken(): string | null {
    const token = this.tokenState();
    return this.isTokenUsable(token) ? token : null;
  }

  login(email: string, password: string): Observable<ApiResponse<AuthResponse>> {
    return this.http
      .post<ApiResponse<AuthResponse>>(`${environment.apiBaseUrl}/auth/login`, { email, password })
      .pipe(tap(response => this.establishSession(response.data)));
  }

  hasAnyRole(roles: readonly RoleName[]): boolean {
    return this.authenticated() && (this.userState()?.roles.some(role => roles.includes(role)) ?? false);
  }

  logout(redirect = true): void {
    sessionStorage.removeItem(TOKEN_KEY);
    sessionStorage.removeItem(USER_KEY);
    this.tokenState.set(null);
    this.userState.set(null);
    if (redirect) void this.router.navigate(['/login']);
  }

  private establishSession(auth: AuthResponse): void {
    sessionStorage.setItem(TOKEN_KEY, auth.accessToken);
    sessionStorage.setItem(USER_KEY, JSON.stringify(auth.user));
    this.tokenState.set(auth.accessToken);
    this.userState.set(auth.user);
  }

  private restoreUser(): AuthUser | null {
    const value = sessionStorage.getItem(USER_KEY);
    if (!value) return null;
    try {
      return JSON.parse(value) as AuthUser;
    } catch {
      sessionStorage.removeItem(USER_KEY);
      return null;
    }
  }

  private isTokenUsable(token: string | null): token is string {
    if (!token) return false;
    try {
      const encodedPayload = token.split('.')[1];
      if (!encodedPayload) return false;
      const base64 = encodedPayload.replace(/-/g, '+').replace(/_/g, '/');
      const payload = JSON.parse(atob(base64.padEnd(Math.ceil(base64.length / 4) * 4, '='))) as { exp?: number };
      return typeof payload.exp === 'number' && payload.exp * 1000 > Date.now();
    } catch {
      return false;
    }
  }
}
