import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, map, Observable, of, switchMap } from 'rxjs';
import { ApiResponse, RegisterRequest } from '../models/user.model';
import { environment } from '../../../environments/environment';
import { LoginRequest, User, UserRole } from '../models/user.model';
import { parseJwtPayload, resolveRole } from '../utils/jwt.util';
import { TokenService } from './token.service';

interface KeycloakTokenResponse {
  access_token: string;
  refresh_token?: string;
  token_type: string;
  expires_in: number;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  readonly currentUser = signal<ReturnType<TokenService['getUser']>>(null);

  private readonly tokenUrl = `${environment.keycloak.url}/realms/${environment.keycloak.realm}/protocol/openid-connect/token`;

  constructor(
    private http: HttpClient,
    private tokenService: TokenService,
    private router: Router
  ) {
    this.currentUser.set(this.tokenService.getUser());
  }

  login(request: LoginRequest): Observable<{ success: boolean; message: string; data?: unknown }> {
    const body = new URLSearchParams({
      grant_type: 'password',
      client_id: environment.keycloak.clientId,
      username: request.username.trim(),
      password: request.password
    });

    return this.http.post<KeycloakTokenResponse>(this.tokenUrl, body.toString(), {
      headers: new HttpHeaders({ 'Content-Type': 'application/x-www-form-urlencoded' })
    }).pipe(
      map(token => {
        this.storeToken(token.access_token);
        return { success: true, message: 'Success', data: this.tokenService.getUser() };
      })
    );
  }

  register(request: RegisterRequest): Observable<{ success: boolean; message: string }> {
    return this.http
      .post<ApiResponse<unknown>>(`${environment.apiUrl}/api/auth/register`, request)
      .pipe(
        switchMap(res => {
          if (!res.success) {
            return of({ success: false, message: res.message || 'Đăng ký thất bại' });
          }
          return this.login({
            username: request.username,
            password: request.password
          }).pipe(
            map(() => ({ success: true, message: 'Đăng ký thành công' })),
            catchError(() =>
              of({ success: true, message: 'Đăng ký thành công — vui lòng đăng nhập' })
            )
          );
        }),
        catchError(err =>
          of({
            success: false,
            message: err.error?.message || 'Đăng ký thất bại'
          })
        )
      );
  }

  profile(): Observable<{ success: boolean; data: User | null }> {
    const user = this.tokenService.getUser();
    if (!user) {
      return new Observable(observer => {
        observer.next({ success: false, data: null });
        observer.complete();
      });
    }
    const profileUser: User = {
      id: user.userId,
      username: user.username,
      email: `${user.username}@samshop.local`,
      role: user.role
    };
    return new Observable(observer => {
      observer.next({ success: true, data: profileUser });
      observer.complete();
    });
  }

  logout(): void {
    this.tokenService.clear();
    this.currentUser.set(null);
    this.router.navigate(['/login']);
  }

  private storeToken(accessToken: string): void {
    const payload = parseJwtPayload(accessToken);
    const username = payload?.preferred_username ?? payload?.sub ?? 'user';
    const userId = typeof payload?.userId === 'number' ? payload.userId : Number(payload?.userId ?? 0);
    const role = resolveRole(payload) as UserRole;

    this.tokenService.setToken(accessToken);
    this.tokenService.setUser({ userId, username, role });
    this.currentUser.set(this.tokenService.getUser());
  }
}
