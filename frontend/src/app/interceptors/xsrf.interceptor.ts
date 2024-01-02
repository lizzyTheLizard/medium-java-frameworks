import { HttpInterceptorFn, HttpRequest, HttpXsrfTokenExtractor } from '@angular/common/http';
import { inject } from '@angular/core';

export const headerName = 'X-XSRF-TOKEN';

export const xsrfInterceptor: HttpInterceptorFn = (req, next) => {
  if (!shoudIntercept(req)) {
    return next(req);
  }
  const token = inject(HttpXsrfTokenExtractor).getToken();
  // Be careful not to overwrite an existing header of the same name.
  if (token !== null && !req.headers.has(headerName)) {
    req = req.clone({ headers: req.headers.set(headerName, token) });
  }
  return next(req);
};

function shoudIntercept(req: HttpRequest<unknown>) {
  if (req.method === 'GET' || req.method === 'HEAD') {
    //Those methods never perform a change, therefore no token needed
    return false;
  }
  if (req.url.toLowerCase().startsWith('http://localhost')) {
    //This is needed for local development
    return true;
  }
  //Use token if relative and not if absolute
  return !req.url.toLowerCase().startsWith('https://' || req.url.toLowerCase().startsWith('http://'));
}
