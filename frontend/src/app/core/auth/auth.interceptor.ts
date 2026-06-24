import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AUTH_CONFIG, buildAuthorizationHeader } from './auth.config';

/** Attach Bearer or Basic credentials to backend API calls when auth is enabled. */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const config = inject(AUTH_CONFIG);

  if (!config.enabled || !req.url.startsWith('/api')) {
    return next(req);
  }
  if (req.headers.has('Authorization')) {
    return next(req);
  }

  const authorization = buildAuthorizationHeader(config);
  if (!authorization) {
    return next(req);
  }

  return next(req.clone({ setHeaders: { Authorization: authorization } }));
};
