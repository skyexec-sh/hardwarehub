import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from './auth.service';

let refreshInFlight = false;

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const token = auth.getAccessToken();

  const authReq =
    token && !req.url.includes('/auth/login') && !req.url.includes('/auth/refresh')
      ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
      : req;

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status !== 401 || req.url.includes('/auth/login') || req.url.includes('/auth/refresh')) {
        return throwError(() => error);
      }
      if (refreshInFlight || !auth.getRefreshToken()) {
        auth.clearSession();
        return throwError(() => error);
      }

      refreshInFlight = true;
      return auth.refresh().pipe(
        switchMap((res) => {
          refreshInFlight = false;
          const retry = req.clone({
            setHeaders: { Authorization: `Bearer ${res.accessToken}` },
          });
          return next(retry);
        }),
        catchError((refreshError) => {
          refreshInFlight = false;
          auth.clearSession();
          return throwError(() => refreshError);
        }),
      );
    }),
  );
};
