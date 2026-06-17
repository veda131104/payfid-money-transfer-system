import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HistoryComponent } from './history.component';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { TransactionService } from '../services/transaction.service';
import { AuthService } from '../services/auth.service';
import { of } from 'rxjs';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { AccountSetupService } from '../services/account-setup.service';

describe('HistoryComponent', () => {
  let component: HistoryComponent;
  let fixture: ComponentFixture<HistoryComponent>;
  let transactionService: TransactionService;
  let authService: AuthService;
  let accountSetupService: AccountSetupService;

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
    Object.defineProperty(HTMLCanvasElement.prototype, 'getContext', {
      value: vi.fn(() => ({
        clearRect: vi.fn(),
        fillRect: vi.fn(),
        getImageData: vi.fn(),
        putImageData: vi.fn(),
        createImageData: vi.fn(),
        setTransform: vi.fn(),
        drawImage: vi.fn(),
        save: vi.fn(),
        restore: vi.fn(),
        beginPath: vi.fn(),
        moveTo: vi.fn(),
        lineTo: vi.fn(),
        closePath: vi.fn(),
        stroke: vi.fn(),
        fill: vi.fn(),
        measureText: vi.fn(() => ({ width: 0 })),
        fillText: vi.fn(),
        strokeText: vi.fn(),
        createLinearGradient: vi.fn(() => ({
          addColorStop: vi.fn()
        })),
        createRadialGradient: vi.fn(() => ({
          addColorStop: vi.fn()
        })),
        createPattern: vi.fn(),
        arc: vi.fn(),
        rect: vi.fn(),
        clip: vi.fn(),
        translate: vi.fn(),
        scale: vi.fn(),
        rotate: vi.fn(),
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
        setLineDash: vi.fn(),
        getLineDash: vi.fn(() => []),
        isPointInPath: vi.fn(),
        isPointInStroke: vi.fn(),
        resetTransform: vi.fn(),
        filter: 'none',
        getContextAttributes: vi.fn(() => ({
          alpha: true,
          desynchronized: false,
          willReadFrequently: false
        })),
        drawFocusIfNeeded: vi.fn(),
        scrollPathIntoView: vi.fn(),
        roundRect: vi.fn(),
        ellipse: vi.fn(),
        reset: vi.fn(),
        getTransform: vi.fn(() => ({
          a: 1, b: 0, c: 0, d: 1, e: 0, f: 0,
          invertSelf: vi.fn(),
          multiplySelf: vi.fn(),
          preMultiplySelf: vi.fn(),
          rotateSelf: vi.fn(),
          scaleSelf: vi.fn(),
          setTransform: vi.fn(),
          skewXSelf: vi.fn(),
          skewYSelf: vi.fn(),
          translateSelf: vi.fn()
        }))
      }))
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

    vi.spyOn(authService, 'getCurrentUser').mockReturnValue({ name: 'testuser' });
    vi.spyOn(accountSetupService, 'getAccountByUser').mockReturnValue(of({ accountNumber: '123456789012' }));
    vi.spyOn(accountSetupService, 'getAccountByNumber').mockReturnValue(of({ balance: 1000, id: 1 }));
    vi.spyOn(transactionService, 'getAccountHistory').mockReturnValue(of([]));

    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load history on init', () => {
    expect(transactionService.getAccountHistory).toHaveBeenCalled();
  });
});
