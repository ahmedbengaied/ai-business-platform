import { HttpInterceptorFn } from '@angular/common/http';

const tokenKey = 'ai-business-platform-token';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const token = sessionStorage.getItem(tokenKey);
  if (!token) {
    return next(request);
  }

  return next(
    request.clone({
      setHeaders: { Authorization: `Bearer ${token}` },
    }),
  );
};
