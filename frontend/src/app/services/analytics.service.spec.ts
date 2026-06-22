import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AnalyticsService } from './analytics.service';

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

    it('should get transaction volume with name', () => {
        service.getTransactionVolume('test').subscribe();
        const req = httpMock.expectOne('http://localhost:8080/api/v1/analytics/transaction-volume?name=test');
        expect(req.request.method).toBe('GET');
        req.flush([]);
    });

    it('should get transaction volume without name', () => {
        service.getTransactionVolume().subscribe();
        const req = httpMock.expectOne('http://localhost:8080/api/v1/analytics/transaction-volume');
        expect(req.request.method).toBe('GET');
        req.flush([]);
    });

    it('should get success rate with name', () => {
        service.getSuccessRate('test').subscribe();
        const req = httpMock.expectOne('http://localhost:8080/api/v1/analytics/success-rate?name=test');
        expect(req.request.method).toBe('GET');
        req.flush({});
    });

    it('should get success rate without name', () => {
        service.getSuccessRate().subscribe();
        const req = httpMock.expectOne('http://localhost:8080/api/v1/analytics/success-rate');
        expect(req.request.method).toBe('GET');
        req.flush({});
    });

    it('should get account activity with name', () => {
        service.getAccountActivity('test').subscribe();
        const req = httpMock.expectOne('http://localhost:8080/api/v1/analytics/account-activity?name=test');
        expect(req.request.method).toBe('GET');
        req.flush([]);
    });

    it('should get account activity without name', () => {
        service.getAccountActivity().subscribe();
        const req = httpMock.expectOne('http://localhost:8080/api/v1/analytics/account-activity');
        expect(req.request.method).toBe('GET');
        req.flush([]);
    });

    it('should get peak hours with name', () => {
        service.getPeakHours('test').subscribe();
        const req = httpMock.expectOne('http://localhost:8080/api/v1/analytics/peak-hours?name=test');
        expect(req.request.method).toBe('GET');
        req.flush([]);
    });

    it('should get peak hours without name', () => {
        service.getPeakHours().subscribe();
        const req = httpMock.expectOne('http://localhost:8080/api/v1/analytics/peak-hours');
        expect(req.request.method).toBe('GET');
        req.flush([]);
    });
});
