import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { UserRole } from '../models/user.model';
import { TokenService } from '../services/token.service';

export const roleGuard = (roles: UserRole[]): CanActivateFn => () => {
  const tokenService = inject(TokenService);
  const router = inject(Router);
  if (tokenService.isLoggedIn() && tokenService.hasRole(...roles)) {
    return true;
  }
  return router.createUrlTree(['/']);
};
