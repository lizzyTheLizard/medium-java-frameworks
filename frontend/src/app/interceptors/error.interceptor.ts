import { HttpErrorResponse, HttpEvent, HttpInterceptorFn } from '@angular/common/http';
import { catchError, EMPTY, Observable } from 'rxjs';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  return next(req).pipe(catchError((error) => handleError(error)));
};

function handleError(error: Error): Observable<HttpEvent<unknown>> {
  if (error instanceof HttpErrorResponse) {
    switch (error.status) {
      case 0: //no connection
      case 401: //not authenticated
      case 403: //forbidden
      case 500: //server-error
        console.error('Tecnical error in server communication', error);
        return EMPTY;
      case 404: //server-error
        console.error('Resource not found', error);
        return EMPTY;
    }
  }
  //Otherwise re-throw
  throw error;
}
