import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AccountSetupService } from './account-setup.service';
import { vi, describe, it, beforeEach, afterEach, expect } from 'vitest';

describe('AccountSetupService', () => {
    let service: AccountSetupService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [AccountSetupService]
        });
        service = TestBed.inject(AccountSetupService);
        httpMock = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
        httpMock.verify();
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    it('should create account', () => {
        const mockResponse = { id: 1 };
        const payload = { userName: 'test' };

        service.create(payload).subscribe(response => {
            expect(response).toEqual(mockResponse);
        });

        const req = httpMock.expectOne('http://localhost:8080/api/v1/account-setup');
        expect(req.request.method).toBe('POST');
        req.flush(mockResponse);
    });

    it('should send OTP', () => {
        const payload = { contact: '123' };
        service.sendOtp(payload).subscribe();
        const req = httpMock.expectOne('http://localhost:8080/api/v1/account-setup/send-otp');
        expect(req.request.method).toBe('POST');
        req.flush({});
    });

    it('should verify OTP', () => {
        const payload = { contact: '123', otp: '456' };
        service.verifyOtp(payload).subscribe();
        const req = httpMock.expectOne('http://localhost:8080/api/v1/account-setup/verify-otp');
        expect(req.request.method).toBe('POST');
        req.flush({});
    });

    it('should setup UPI', () => {
        service.setupUpi(1, 'test@upi').subscribe();
        const req = httpMock.expectOne('http://localhost:8080/api/v1/account-setup/1/upi');
        expect(req.request.method).toBe('POST');
        req.flush({});
    });

    it('should get account by user', () => {
        service.getAccountByUser('test').subscribe();
        const req = httpMock.expectOne('http://localhost:8080/api/v1/account-setup/user/test');
        expect(req.request.method).toBe('GET');
        req.flush({});
    });

    it('should update account', () => {
        service.updateAccount('test', {}).subscribe();
        const req = httpMock.expectOne('http://localhost:8080/api/v1/account-setup/user/test');
        expect(req.request.method).toBe('PUT');
        req.flush({});
    });

    it('should get account by number', () => {
        service.getAccountByNumber('123').subscribe();
        const req = httpMock.expectOne('http://localhost:8080/api/v1/accounts/number/123');
        expect(req.request.method).toBe('GET');
        req.flush({});
    });

    it('should set pin', () => {
        service.setPin('test', '1234').subscribe();
        const req = httpMock.expectOne('http://localhost:8080/api/v1/account-setup/user/test/pin');
        expect(req.request.method).toBe('POST');
        req.flush({});
    });
});
