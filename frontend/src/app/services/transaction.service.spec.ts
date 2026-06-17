import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TransactionService } from './transaction.service';
import { describe, it, beforeEach, afterEach, expect } from 'vitest';

describe('TransactionService', () => {
    let service: TransactionService;
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
            providers: [TransactionService]
        });
        service = TestBed.inject(TransactionService);
        httpMock = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
        httpMock.verify();
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    it('should execute transfer', () => {
        const payload = { fromAccountNumber: '123', toAccountNumber: '456', amount: 100 };
        service.executeTransfer(payload).subscribe();
        const req = httpMock.expectOne('http://localhost:8080/api/v1/transfers/by-account');
        expect(req.request.method).toBe('POST');
        req.flush({});
    });

    it('should get account history', () => {
        service.getAccountHistory(1).subscribe();
        const req = httpMock.expectOne('http://localhost:8080/api/v1/transfers/account/1');
        expect(req.request.method).toBe('GET');
        req.flush({ transactions: [] });
    });

    it('should add transaction and save to storage', () => {
        const tx: any = { id: '1', amount: '100', date: new Date(), type: 'debit', status: 'SUCCESS', referenceId: 'REF1' };
        service.addTransaction(tx);
        const history = service.getTransactions();
        expect(history).toContainEqual(tx);
    });
});
