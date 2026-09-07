import { inject } from '@angular/core';
import { CanActivateFn } from '@angular/router';

import { AuthService } from './auth.service';

/**
 * Keeps the board views behind a Keycloak session.
 *
 * `AuthService.init()` has already run by the time any route is activated, so
 * `authenticated()` is a plain synchronous answer. When it is false the browser
 * is sent to the Keycloak login page and told to come back to the very URL that
 * was asked for, so a deep link like `/boards/3` survives the round trip.
 */
export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  if (auth.authenticated()) {
    return true;
  }

  auth.login(window.location.origin + state.url);
  return false;
};
