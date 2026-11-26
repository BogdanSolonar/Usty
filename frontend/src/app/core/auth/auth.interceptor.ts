import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';

// До кожного запиту на /api додає заголовок "Authorization: Bearer <токен>".
// Якщо бекенд відповів 401 (токен сплив або недійсний) — виходимо на сторінку входу.
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const token = auth.token();
  const isApi = req.url.startsWith('/api/');
  const isLogin = req.url === '/api/auth/login';

  const request = token && isApi && !isLogin ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;

  return next(request).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 && isApi && !isLogin) auth.logout();
      return throwError(() => error);
    }),
  );
};
