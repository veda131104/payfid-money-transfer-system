import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AnalyticsService } from './analytics.service';
import { describe, it, beforeEach, afterEach, expect } from 'vitest';

describe('AnalyticsService', () => {
    let service: AnalyticsService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.resetTestingModule();
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [AnalyticsService]
        });
        service = TestBed.inject(AnalyticsService);
        httpMock = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
        httpMock.verify();
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    it('should get transaction volume', () => {
        service.getTransactionVolume('test').subscribe();
        const req = httpMock.expectOne('http://localhost:8080/api/v1/analytics/transaction-volume?name=test');
        expect(req.request.method).toBe('GET');
        req.flush([]);
    });

    it('should get success rate', () => {
        service.getSuccessRate('test').subscribe();
        const req = httpMock.expectOne('http://localhost:8080/api/v1/analytics/success-rate?name=test');
        expect(req.request.method).toBe('GET');
        req.flush({});
    });

    it('should get account activity', () => {
        service.getAccountActivity('test').subscribe();
        const req = httpMock.expectOne('http://localhost:8080/api/v1/analytics/account-activity?name=test');
        expect(req.request.method).toBe('GET');
        req.flush([]);
    });

    it('should get peak hours', () => {
        service.getPeakHours('test').subscribe();
        const req = httpMock.expectOne('http://localhost:8080/api/v1/analytics/peak-hours?name=test');
        expect(req.request.method).toBe('GET');
        req.flush([]);
    });
});
