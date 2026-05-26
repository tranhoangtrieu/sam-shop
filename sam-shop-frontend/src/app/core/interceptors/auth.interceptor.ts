import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { TokenService } from '../services/token.service';

/** Paths that gateway allows without JWT (see gateway SecurityConfig). */
function isPublicGet(path: string): boolean {
  return /^\/api\/products(\/|$)/.test(path);
}

function requestPath(url: string): string {
  try {
    return new URL(url, 'http://localhost').pathname;
  } catch {
    return url.split('?')[0];
  }
}

function isTokenExpired(token: string): boolean {
  try {
    const payload = JSON.parse(atob(token.split('.')[1]));
    const exp = payload.exp as number | undefined;
    return !exp || Date.now() >= exp * 1000;
  } catch {
    return true;
  }
}

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const tokenService = inject(TokenService);
  let token = tokenService.getToken();

  if (token && isTokenExpired(token)) {
    tokenService.clear();
    token = null;
  }

  const path = requestPath(req.url);
  const attachAuth = !!token && !(req.method === 'GET' && isPublicGet(path));

  if (attachAuth) {
    req = req.clone({
      setHeaders: { Authorization: `Bearer ${token}` }
    });
  }

  return next(req).pipe(
    catchError((err: HttpErrorResponse) => {
      if (err.status === 401 && tokenService.getToken()) {
        tokenService.clear();
      }
      return throwError(() => err);
    })
  );
};
