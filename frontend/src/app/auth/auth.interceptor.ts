import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { from, switchMap } from 'rxjs';

import { environment } from '../../environments/environment';
import { AuthService } from './auth.service';

/**
 * Puts `Authorization: Bearer <access token>` on every call to the Kanban API.
 *
 * Doing it here rather than in `BoardService` means the whole service layer -
 * and any client generated from the OpenAPI document - stays unaware that the
 * API is secured at all. `AuthService.token()` also refreshes an
 * about-to-expire token, so a long session never fails with a surprise 401.
 *
 * Only `/api` calls are touched: the token is meant for the backend, and
 * leaking it to any other host would hand out the user's identity.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  if (!req.url.startsWith(environment.apiUrl)) {
    return next(req);
  }

  const auth = inject(AuthService);
  return from(auth.token()).pipe(
    switchMap(token =>
      next(token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req)
    )
  );
};
