import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TransactionService } from './transaction.service';
import { PLATFORM_ID } from '@angular/core';

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
            providers: [TransactionService, { provide: PLATFORM_ID, useValue: 'browser' }]
        });
        service = TestBed.inject(TransactionService);
        httpMock = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
        httpMock.verify();
        localStorage.clear();
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

    it('should get account history with empty transactions', () => {
        service.getAccountHistory(1).subscribe();
        const req = httpMock.expectOne('http://localhost:8080/api/v1/transfers/account/1');
        expect(req.request.method).toBe('GET');
        req.flush({ transactions: [] });
    });

    it('should get account history with real transaction data (mapDtoToTransaction with idempotencyKey)', () => {
        service.getAccountHistory(5).subscribe(txns => {
            expect(txns.length).toBe(1);
            expect(txns[0].id).toBe('42');
            expect(txns[0].referenceId).toBe('KEY123');
            expect(txns[0].accountNumber).toBe('ACC001');
        });
        const req = httpMock.expectOne('http://localhost:8080/api/v1/transfers/account/5');
        req.flush({
            transactions: [{
                id: 42,
                fromAccountNumber: 'ACC001',
                toAccountNumber: 'ACC002',
                amount: 500,
                transactionDate: new Date().toISOString(),
                type: 'DEBIT',
                status: 'SUCCESS',
                idempotencyKey: 'KEY123',
                description: 'Test transfer',
                fromAccountHolderName: 'Alice',
                toAccountHolderName: 'Bob'
            }]
        });
    });

    it('should mapDtoToTransaction without idempotencyKey (fallback referenceId)', () => {
        service.getAccountHistory(6).subscribe(txns => {
            expect(txns[0].referenceId).toBe('TXN99');
            expect(txns[0].accountNumber).toBe('');
        });
        const req = httpMock.expectOne('http://localhost:8080/api/v1/transfers/account/6');
        req.flush({
            transactions: [{
                id: 99,
                fromAccountNumber: null,
                toAccountNumber: null,
                amount: 200,
                transactionDate: new Date().toISOString(),
                type: 'CREDIT',
                status: 'FAILED',
                idempotencyKey: null,
                description: 'fallback test'
            }]
        });
    });

    it('should handle getAccountHistory error', () => {
        let errorThrown = false;
        service.getAccountHistory(1).subscribe({
            next: () => {},
            error: () => { errorThrown = true; }
        });
        const req = httpMock.expectOne('http://localhost:8080/api/v1/transfers/account/1');
        req.flush('Error', { status: 500, statusText: 'Internal Server Error' });
        expect(errorThrown).toBeTrue();
    });

    it('should add transaction and save to storage', () => {
        const tx: any = { id: '1', amount: '100', date: new Date(), type: 'debit', status: 'SUCCESS', referenceId: 'REF1' };
        service.addTransaction(tx);
        const history = service.getTransactions();
        expect(history[0]).toEqual(tx);
    });

    it('should load transactions from localStorage on construction', () => {
        const stored = [
            { id: '1', accountNumber: '111', amount: '50', date: new Date().toISOString(), type: 'debit', status: 'SUCCESS', referenceId: 'REF2' }
        ];
        localStorage.setItem('transactions', JSON.stringify(stored));

        TestBed.resetTestingModule();
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [TransactionService]
        });
        const svc2 = TestBed.inject(TransactionService);
        const txns = svc2.getTransactions();
        expect(txns.length).toBe(1);
        expect(txns[0].id).toBe('1');

        const hm2 = TestBed.inject(HttpTestingController);
        hm2.verify();
    });

    it('should handle invalid JSON in localStorage gracefully', () => {
        localStorage.setItem('transactions', 'not-valid-json');

        TestBed.resetTestingModule();
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [TransactionService]
        });
        const svc3 = TestBed.inject(TransactionService);
        const txns = svc3.getTransactions();
        expect(txns.length).toBe(0);

        const hm3 = TestBed.inject(HttpTestingController);
        hm3.verify();
    });

    it('should generate a reference ID', () => {
        const refId = service.generateReferenceId();
        expect(refId).toMatch(/^TXN\d+$/);
    });

    it('should skip storage operations when not in browser (server platform)', () => {
        TestBed.resetTestingModule();
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [
                TransactionService,
                { provide: PLATFORM_ID, useValue: 'server' }
            ]
        });
        const serverService = TestBed.inject(TransactionService);
        expect(serverService).toBeTruthy();
        const tx: any = { id: '2', amount: '50', date: new Date(), type: 'credit', status: 'completed', referenceId: 'REF9' };
        serverService.addTransaction(tx);
        expect(serverService.getTransactions().length).toBe(1);

        const hm4 = TestBed.inject(HttpTestingController);
        hm4.verify();
    });
});
