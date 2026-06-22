import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AuthService } from './auth.service';

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
        const mockResponse = { name: 'test', email: 'test@test.com' };
        const payload = { name: 'test', password: 'pass', email: 'test@test.com' };
        service.signup(payload).subscribe(res => {
            expect(res).toEqual(mockResponse);
        });
        const req = httpMock.expectOne('http://localhost:8080/api/v1/auth/signup');
        expect(req.request.method).toBe('POST');
        req.flush(mockResponse);
    });

    it('should login and set current user', () => {
        const mockResponse = { userId: 1, name: 'test', email: 'test@company.com', token: 'token', rememberToken: 'token' };
        const payload = { name: 'test', password: 'pass', rememberMe: true };
        service.login(payload).subscribe(res => {
            expect(service.getCurrentUser()?.name).toEqual('test');
            expect(localStorage.getItem('auth_session')).toContain('test');
        });
        const req = httpMock.expectOne('http://localhost:8080/api/v1/auth/login');
        req.flush(mockResponse);
    });

    it('should return null from getCurrentUser when no session stored', () => {
        // Ensure no session in storage
        const result = service.getCurrentUser();
        expect(result).toBeNull();
    });

    it('should return parsed user from getCurrentUser when session exists', () => {
        localStorage.setItem('auth_session', JSON.stringify({ name: 'Alice', email: 'alice@example.com' }));
        const user = service.getCurrentUser();
        expect(user?.name).toBe('Alice');
    });

    it('should return null from getCurrentUser when session JSON is invalid', () => {
        localStorage.setItem('auth_session', '{invalid json}');
        const result = service.getCurrentUser();
        expect(result).toBeNull();
    });

    it('should return token from getToken when token exists', () => {
        localStorage.setItem('auth_token', 'my-jwt-token');
        const token = service.getToken();
        expect(token).toBe('my-jwt-token');
    });

    it('should return null from getToken when no token stored', () => {
        const token = service.getToken();
        expect(token).toBeNull();
    });

    it('should return true from isLoggedIn when token and session both exist', () => {
        localStorage.setItem('auth_token', 'some-token');
        localStorage.setItem('auth_session', JSON.stringify({ name: 'Alice', email: 'alice@example.com' }));
        expect(service.isLoggedIn()).toBeTrue();
    });

    it('should return false from isLoggedIn when token is missing', () => {
        localStorage.setItem('auth_session', JSON.stringify({ name: 'Alice', email: 'alice@example.com' }));
        expect(service.isLoggedIn()).toBeFalse();
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
        const mockResponse = { userId: 1, name: 'test', email: 'test@company.com', token: 'token', tokenType: 'Bearer' };
        service.loginWithToken('token').subscribe(res => {
            expect(service.getCurrentUser()?.name).toEqual('test');
        });
        const req = httpMock.expectOne('http://localhost:8080/api/v1/auth/login-with-token');
        expect(req.request.method).toBe('POST');
        req.flush(mockResponse);
    });

    it('should get credentials by token', () => {
        service.getCredentialsByToken('mytoken').subscribe();
        const req = httpMock.expectOne('http://localhost:8080/api/v1/auth/credentials/mytoken');
        expect(req.request.method).toBe('GET');
        req.flush({ name: 'test', email: 'test@test.com', password: 'pass' });
    });
});
