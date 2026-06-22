import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HistoryComponent } from './history.component';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { TransactionService } from '../services/transaction.service';
import { AuthService } from '../services/auth.service';
import { of } from 'rxjs';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { AccountSetupService } from '../services/account-setup.service';

describe('HistoryComponent', () => {
  let component: HistoryComponent;
  let fixture: ComponentFixture<HistoryComponent>;
  let transactionService: TransactionService;
  let authService: AuthService;
  let accountSetupService: AccountSetupService;
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

    getCurrentUserSpy = spyOn(authService, 'getCurrentUser').and.returnValue({ name: 'testuser', email: 'testuser@company.com' });
    getAccountByUserSpy = spyOn(accountSetupService, 'getAccountByUser').and.returnValue(of({ accountNumber: '123456789012' }));
    getAccountByNumberSpy = spyOn(accountSetupService, 'getAccountByNumber').and.returnValue(of({ balance: 1000, id: 1 }));
    getAccountHistorySpy = spyOn(transactionService, 'getAccountHistory').and.returnValue(of([]));

    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load history on init', () => {
    expect(transactionService.getAccountHistory).toHaveBeenCalled();
  });
});
