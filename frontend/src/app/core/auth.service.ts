import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { BehaviorSubject, tap } from 'rxjs';
import { environment } from '../../environments/environment';
import { LoginRequest, RegisterRequest, LoginResponse, OtpVerifyRequest, TwoFaSetupResponse, Disable2faRequest } from '../models/auth.models';

@Injectable()
export class AuthService {
  private readonly tokenKey = 'sa25s_access_token';
  private readonly tempKey = 'sa25s_temp_token';
  private readonly api = environment.apiUrl;
  private userTwoFactor$ = new BehaviorSubject<boolean>(false);

  constructor(private http: HttpClient, private router: Router) { }

  register(payload: RegisterRequest) {
    return this.http.post(`${this.api}/auth/register`, payload);
  }

  login(payload: LoginRequest) {
    return this.http.post<LoginResponse>(`${this.api}/auth/login`, payload).pipe(
      tap(res => {
        if (res.token) {
          this.setToken(res.token);
          this.userTwoFactor$.next(false);
        }
        if (res.temporaryToken) {
          localStorage.setItem(this.tempKey, res.temporaryToken);
          this.userTwoFactor$.next(true);
        }
      })
    );
  }

  setup2fa() {
    return this.http.post<TwoFaSetupResponse>(`${this.api}/auth/2fa/setup`, {});
  }

  verifyOtp(payload: OtpVerifyRequest) {
    return this.http.post<LoginResponse>(`${this.api}/auth/2fa/verify`, payload).pipe(
      tap(res => {
        if (res.token) {
          this.setToken(res.token);
          localStorage.removeItem(this.tempKey);
          this.userTwoFactor$.next(true);
        }
      })
    );
  }

  disable2fa(payload: Disable2faRequest) {
    return this.http.post(`${this.api}/auth/2fa/disable`, payload).pipe(
      tap(() => this.userTwoFactor$.next(false))
    );
  }

  getTempToken(): string | null {
    return localStorage.getItem(this.tempKey);
  }

  setToken(token: string) {
    localStorage.setItem(this.tokenKey, token);
  }

  getToken(): string | null {
    return localStorage.getItem(this.tokenKey);
  }

  isAuthenticated(): boolean {
    return !!this.getToken();
  }

  hasPending2fa(): boolean {
    return !!this.getTempToken() && !this.getToken();
  }

  logout() {
    localStorage.removeItem(this.tokenKey);
    localStorage.removeItem(this.tempKey);
    this.router.navigate(['/login']);
  }
}
