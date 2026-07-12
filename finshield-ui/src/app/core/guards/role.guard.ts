import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { RoleName } from '../auth/auth.models';
import { AuthService } from '../auth/auth.service';

export const roleGuard: CanActivateFn = route => {
  const auth = inject(AuthService);
  const roles = (route.data['roles'] ?? []) as RoleName[];
  return roles.length === 0 || auth.hasAnyRole(roles)
    ? true
    : inject(Router).createUrlTree(['/dashboard']);
};
