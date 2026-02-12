import { Injectable, Inject, PLATFORM_ID } from '@angular/core';
import {
  HttpRequest,
  HttpHandler,
  HttpEvent,
  HttpInterceptor,
  HttpErrorResponse
} from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { isPlatformBrowser } from '@angular/common';
import { AuthService } from './auth.service';
import { Router } from '@angular/router';

@Injectable()
export class JwtInterceptor implements HttpInterceptor {
  constructor(
    private authService: AuthService,
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    // Skip auth endpoints
    if (request.url.includes('/auth/')) {
      return next.handle(request);
    }

    if (isPlatformBrowser(this.platformId)) {
      const token = this.authService.getToken();
      
      if (token) {
        // Clone the request and add the authorization header
        request = request.clone({
          setHeaders: {
            Authorization: `Bearer ${token}`
          }
        });
      }
    }

    return next.handle(request).pipe(
      catchError((error: HttpErrorResponse) => {
        // If 401 Unauthorized, clear session and redirect to login
        if (error.status === 401) {
          this.authService.clearSession();
          this.router.navigate(['/login']);
        }
        return throwError(() => error);
      })
    );
  }
}
