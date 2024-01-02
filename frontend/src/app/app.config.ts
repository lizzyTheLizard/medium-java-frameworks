import { ApplicationConfig, importProvidersFrom } from '@angular/core';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { ApiModule, Configuration } from '../generated';
import { HttpClientModule, provideHttpClient, withInterceptors } from '@angular/common/http';
import { xsrfInterceptor } from './interceptors/xsrf.interceptor';
import { errorInterceptor } from './interceptors/error.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideHttpClient(withInterceptors([xsrfInterceptor, errorInterceptor])),
    importProvidersFrom([
      BrowserAnimationsModule,
      HttpClientModule,
      ApiModule.forRoot(() => new Configuration({ withCredentials: true })),
    ]),
  ],
};
