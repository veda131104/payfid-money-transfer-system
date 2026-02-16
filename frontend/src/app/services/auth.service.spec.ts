import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AuthService } from './auth.service';
import { describe, it, beforeEach, afterEach, expect } from 'vitest';

describe('AuthService', () => {
    let service: AuthService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        const localStorageMock = (() => {
            let store: { [key: string]: string } = {};
            return {
                getItem: (key: string) => store[key] || null,
                setItem: (key: string, value: string) => { store[key] = value.toString(); },
                removeItem: (key: string) => { delete store[key]; },
                clear: () => { store = {}; },
                length: 0,
                key: (index: number) => null
            };
        })();
        Object.defineProperty(window, 'localStorage', { value: localStorageMock, writable: true });

        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [AuthService]
        });
        service = TestBed.inject(AuthService);
        httpMock = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
        httpMock.verify();
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    it('should signup', () => {
        const mockResponse = { name: 'test' };
        const payload = { name: 'test', password: 'pass', email: 'test@test.com' };
        service.signup(payload).subscribe(res => {
            expect(res).toEqual(mockResponse);
        });
        const req = httpMock.expectOne('http://localhost:8080/api/v1/auth/signup');
        expect(req.request.method).toBe('POST');
        req.flush(mockResponse);
    });

    it('should login and set current user', () => {
        const mockResponse = { name: 'test', rememberToken: 'token' };
        const payload = { name: 'test', password: 'pass', rememberMe: true };
        service.login(payload).subscribe(res => {
            expect(service.getCurrentUser()?.name).toEqual('test');
            expect(localStorage.getItem('auth_session')).toContain('test');
        });
        const req = httpMock.expectOne('http://localhost:8080/api/v1/auth/login');
        req.flush(mockResponse);
    });

    it('should clear session', () => {
        localStorage.setItem('auth_session', JSON.stringify({ name: 'test' }));
        service.clearSession();
        expect(service.getCurrentUser()).toBeNull();
        expect(localStorage.getItem('auth_session')).toBeNull();
    });

    it('should process forgot password', () => {
        service.forgotPassword('test').subscribe();
        const req = httpMock.expectOne('http://localhost:8080/api/v1/auth/forgot-password');
        expect(req.request.method).toBe('POST');
        req.flush({});
    });

    it('should reset password', () => {
        const payload = { token: 'token', newPassword: 'newpass' };
        service.resetPassword(payload).subscribe();
        const req = httpMock.expectOne('http://localhost:8080/api/v1/auth/reset-password');
        expect(req.request.method).toBe('POST');
        req.flush({});
    });

    it('should login with token', () => {
        const mockResponse = { name: 'test' };
        service.loginWithToken('token').subscribe(res => {
            expect(service.getCurrentUser()?.name).toEqual('test');
        });
        const req = httpMock.expectOne('http://localhost:8080/api/v1/auth/verify-token?token=token');
        req.flush(mockResponse);
    });
});
