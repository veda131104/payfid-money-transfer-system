import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HistoryComponent } from './history.component';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { TransactionService, Transaction } from '../services/transaction.service';
import { AuthService } from '../services/auth.service';
import { of, throwError } from 'rxjs';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { AccountSetupService } from '../services/account-setup.service';
import { Router, NavigationEnd } from '@angular/router';

describe('HistoryComponent', () => {
  let component: HistoryComponent;
  let fixture: ComponentFixture<HistoryComponent>;
  let transactionService: TransactionService;
  let authService: AuthService;
  let accountSetupService: AccountSetupService;
  let router: Router;
  let getCurrentUserSpy: jasmine.Spy;
  let getAccountByUserSpy: jasmine.Spy;
  let getAccountByNumberSpy: jasmine.Spy;
  let getAccountHistorySpy: jasmine.Spy;

  beforeEach(async () => {
    // Mock localStorage
    const localStorageMock = (() => {
      let store: any = {};
      return {
        getItem: (key: string) => store[key] || null,
        setItem: (key: string, value: string) => store[key] = value,
        removeItem: (key: string) => delete store[key],
        clear: () => store = {}
      };
    })();
    Object.defineProperty(window, 'localStorage', { value: localStorageMock });

    // Mock Canvas for charts
    const dummyCtx = {
      clearRect: jasmine.createSpy('clearRect'),
      fillRect: jasmine.createSpy('fillRect'),
      getImageData: jasmine.createSpy('getImageData'),
      putImageData: jasmine.createSpy('putImageData'),
      createImageData: jasmine.createSpy('createImageData'),
      setTransform: jasmine.createSpy('setTransform'),
      drawImage: jasmine.createSpy('drawImage'),
      save: jasmine.createSpy('save'),
      restore: jasmine.createSpy('restore'),
      beginPath: jasmine.createSpy('beginPath'),
      moveTo: jasmine.createSpy('moveTo'),
      lineTo: jasmine.createSpy('lineTo'),
      closePath: jasmine.createSpy('closePath'),
      stroke: jasmine.createSpy('stroke'),
      fill: jasmine.createSpy('fill'),
      measureText: jasmine.createSpy('measureText').and.returnValue({ width: 0 }),
      fillText: jasmine.createSpy('fillText'),
      strokeText: jasmine.createSpy('strokeText'),
      createLinearGradient: jasmine.createSpy('createLinearGradient').and.returnValue({
        addColorStop: jasmine.createSpy('addColorStop')
      }),
      createRadialGradient: jasmine.createSpy('createRadialGradient').and.returnValue({
        addColorStop: jasmine.createSpy('addColorStop')
      }),
      createPattern: jasmine.createSpy('createPattern'),
      arc: jasmine.createSpy('arc'),
      rect: jasmine.createSpy('rect'),
      clip: jasmine.createSpy('clip'),
      translate: jasmine.createSpy('translate'),
      scale: jasmine.createSpy('scale'),
      rotate: jasmine.createSpy('rotate'),
      globalAlpha: 1,
      globalCompositeOperation: 'source-over',
      strokeStyle: '#000',
      fillStyle: '#000',
      shadowOffsetX: 0,
      shadowOffsetY: 0,
      shadowBlur: 0,
      shadowColor: 'rgba(0, 0, 0, 0)',
      lineWidth: 1,
      lineCap: 'butt',
      lineJoin: 'miter',
      miterLimit: 10,
      font: '10px sans-serif',
      textAlign: 'start',
      textBaseline: 'alphabetic',
      direction: 'ltr',
      imageSmoothingEnabled: true,
      lineDashOffset: 0,
      setLineDash: jasmine.createSpy('setLineDash'),
      getLineDash: jasmine.createSpy('getLineDash').and.returnValue([]),
      isPointInPath: jasmine.createSpy('isPointInPath'),
      isPointInStroke: jasmine.createSpy('isPointInStroke'),
      resetTransform: jasmine.createSpy('resetTransform'),
      filter: 'none',
      getContextAttributes: jasmine.createSpy('getContextAttributes').and.returnValue({
        alpha: true,
        desynchronized: false,
        willReadFrequently: false
      }),
      drawFocusIfNeeded: jasmine.createSpy('drawFocusIfNeeded'),
      scrollPathIntoView: jasmine.createSpy('scrollPathIntoView'),
      roundRect: jasmine.createSpy('roundRect'),
      ellipse: jasmine.createSpy('ellipse'),
      reset: jasmine.createSpy('reset'),
      getTransform: jasmine.createSpy('getTransform').and.returnValue({
        a: 1, b: 0, c: 0, d: 1, e: 0, f: 0,
        invertSelf: jasmine.createSpy('invertSelf'),
        multiplySelf: jasmine.createSpy('multiplySelf'),
        preMultiplySelf: jasmine.createSpy('preMultiplySelf'),
        rotateSelf: jasmine.createSpy('rotateSelf'),
        scaleSelf: jasmine.createSpy('scaleSelf'),
        setTransform: jasmine.createSpy('setTransform'),
        skewXSelf: jasmine.createSpy('skewXSelf'),
        skewYSelf: jasmine.createSpy('skewYSelf'),
        translateSelf: jasmine.createSpy('translateSelf')
      })
    };
    Object.defineProperty(HTMLCanvasElement.prototype, 'getContext', {
      value: jasmine.createSpy('getContext').and.returnValue(dummyCtx)
    });

    await TestBed.configureTestingModule({
      imports: [HistoryComponent, HttpClientTestingModule, RouterTestingModule, NoopAnimationsModule],
      providers: [TransactionService, AuthService, AccountSetupService]
    })
      .compileComponents();

    fixture = TestBed.createComponent(HistoryComponent);
    component = fixture.componentInstance;
    transactionService = TestBed.inject(TransactionService);
    authService = TestBed.inject(AuthService);
    accountSetupService = TestBed.inject(AccountSetupService);
    router = TestBed.inject(Router);
    spyOn(router, 'navigate');

    getCurrentUserSpy = spyOn(authService, 'getCurrentUser').and.returnValue({ name: 'testuser', email: 'testuser@company.com' });
    getAccountByUserSpy = spyOn(accountSetupService, 'getAccountByUser').and.returnValue(of({ accountNumber: '123456789012' }));
    getAccountByNumberSpy = spyOn(accountSetupService, 'getAccountByNumber').and.returnValue(of({ balance: 1000, id: 1 }));
    getAccountHistorySpy = spyOn(transactionService, 'getAccountHistory').and.returnValue(of([]));
  });

  it('should create', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('should load history on init', () => {
    fixture.detectChanges();
    expect(transactionService.getAccountHistory).toHaveBeenCalled();
  });

  it('should handle navigation events to refresh data', () => {
    fixture.detectChanges();
    spyOn(transactionService, 'getTransactions').and.returnValue([]);
    
    // Simulate Router NavigationEnd event
    const navEnd = new NavigationEnd(1, '/history', '/history');
    (router.events as any).next(navEnd);

    expect(component.currentUserAccountNumber).toBe('123456789012');
  });

  it('should load more transactions and manage hasMoreTransactions flag', () => {
    fixture.detectChanges();
    const mockTxns: Transaction[] = Array.from({ length: 12 }, (_, i) => ({
      id: i.toString(),
      accountNumber: '999999999999',
      amount: '100',
      date: new Date(),
      type: 'debit',
      status: 'completed',
      referenceId: `REF${i}`,
      description: 'Test'
    }));
    component.rawTransactions = mockTxns;
    
    // First page
    component.currentPage = 0;
    component.displayedTransactions = [];
    component.loadMore();

    expect(component.displayedTransactions.length).toBe(5);
    expect(component.hasMoreTransactions).toBeTrue();

    // Second page
    component.loadMore();
    expect(component.displayedTransactions.length).toBe(10);
    expect(component.hasMoreTransactions).toBeTrue();

    // Third page (last 2)
    component.loadMore();
    expect(component.displayedTransactions.length).toBe(12);
    expect(component.hasMoreTransactions).toBeFalse();
  });

  it('should filter transactions by today, month, and year correctly', () => {
    fixture.detectChanges();
    const now = new Date();
    const pastMonth = new Date(now.getFullYear(), now.getMonth() - 1, 10);
    const pastYear = new Date(now.getFullYear() - 1, now.getMonth(), 10);

    component.rawTransactions = [
      { id: '1', accountNumber: '111', amount: '10', date: now, type: 'credit', status: 'completed', referenceId: '1', description: 'today' },
      { id: '2', accountNumber: '222', amount: '20', date: pastMonth, type: 'debit', status: 'completed', referenceId: '2', description: 'past month' },
      { id: '3', accountNumber: '333', amount: '30', date: pastYear, type: 'credit', status: 'completed', referenceId: '3', description: 'past year' }
    ];

    // Filter 'today'
    component.setFilter('today');
    expect(component.displayedTransactions.length).toBe(1);
    expect(component.displayedTransactions[0].description).toBe('today');

    // Filter 'month' -> since now is current month, only 'today' is in current month.
    // Wait, the pastMonth txn date has month = now.getMonth() - 1, so it won't match.
    // The pastYear txn has year = now.getFullYear() - 1, so it won't match.
    component.setFilter('month');
    expect(component.displayedTransactions.length).toBe(1);
    expect(component.displayedTransactions[0].description).toBe('today');

    // Filter 'year' -> today and pastMonth are in current year, pastYear is not.
    component.setFilter('year');
    expect(component.displayedTransactions.length).toBe(2);
  });

  it('should open and close transaction detail view', () => {
    fixture.detectChanges();
    const txn: Transaction = { id: '1', accountNumber: '111', amount: '10', date: new Date(), type: 'credit', status: 'completed', referenceId: '1', description: 'test' };
    component.openTransactionDetail(txn, 0);
    expect(component.selectedTransaction).toBe(txn);

    component.closeTransactionDetail();
    expect(component.selectedTransaction).toBeNull();
  });

  it('should add transaction and reset pagination', () => {
    fixture.detectChanges();
    const txn: Transaction = { id: '1', accountNumber: '111', amount: '10', date: new Date(), type: 'credit', status: 'completed', referenceId: '1', description: 'test' };
    component.addTransaction(txn);
    expect(component.allTransactions[0]).toBe(txn);
    expect(component.currentPage).toBe(1); // loadMore was run once
  });

  it('should format transaction sign based on sender/receiver perspective', () => {
    fixture.detectChanges();
    component.currentUserAccountNumber = '123456789012';

    // Received money (toAccountNumber = me) -> +
    const rxTxn: Transaction = {
      id: '1',
      accountNumber: '999999999999',
      fromAccountNumber: '999999999999',
      toAccountNumber: '123456789012',
      amount: '100',
      date: new Date(),
      type: 'transfer',
      status: 'completed',
      referenceId: '1',
      description: 'rx'
    };
    expect(component.getTransactionSign(rxTxn)).toBe('+');
    expect(component.getDisplayType(rxTxn)).toBe('credit');

    // Sent money (fromAccountNumber = me) -> -
    const txTxn: Transaction = {
      id: '2',
      accountNumber: '999999999999',
      fromAccountNumber: '123456789012',
      toAccountNumber: '999999999999',
      amount: '100',
      date: new Date(),
      type: 'transfer',
      status: 'completed',
      referenceId: '2',
      description: 'tx'
    };
    expect(component.getTransactionSign(txTxn)).toBe('-');
    expect(component.getDisplayType(txTxn)).toBe('debit');

    // Fallback: type 'credit' -> +
    const creditTxn: Transaction = {
      id: '3',
      accountNumber: '999999999999',
      amount: '100',
      date: new Date(),
      type: 'credit',
      status: 'completed',
      referenceId: '3',
      description: 'credit'
    };
    expect(component.getTransactionSign(creditTxn)).toBe('+');

    // Fallback: type 'debit' -> -
    const debitTxn: Transaction = {
      id: '4',
      accountNumber: '999999999999',
      amount: '100',
      date: new Date(),
      type: 'debit',
      status: 'completed',
      referenceId: '4',
      description: 'debit'
    };
    expect(component.getTransactionSign(debitTxn)).toBe('-');
  });

  it('should logout and clear session successfully', () => {
    fixture.detectChanges();
    spyOn(authService, 'clearSession');
    component.onLogout();
    expect(authService.clearSession).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/']);
  });
});
