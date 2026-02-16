import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TransferComponent } from './transfer.component';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { TransactionService } from '../services/transaction.service';
import { AccountSetupService } from '../services/account-setup.service';
import { of } from 'rxjs';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { FormsModule } from '@angular/forms';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { AuthService } from '../services/auth.service';

describe('TransferComponent', () => {
  let component: TransferComponent;
  let fixture: ComponentFixture<TransferComponent>;
  let transactionService: TransactionService;
  let accountSetupService: AccountSetupService;
  let authService: AuthService;

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
        arc: vi.fn(),
        rect: vi.fn(),
        clip: vi.fn(),
        translate: vi.fn(),
        scale: vi.fn(),
        rotate: vi.fn(),
      }))
    });

    await TestBed.configureTestingModule({
      imports: [
        TransferComponent,
        HttpClientTestingModule,
        RouterTestingModule,
        NoopAnimationsModule,
        FormsModule
      ],
      providers: [TransactionService, AccountSetupService]
    }).compileComponents();

    fixture = TestBed.createComponent(TransferComponent);
    component = fixture.componentInstance;
    transactionService = TestBed.inject(TransactionService);
    accountSetupService = TestBed.inject(AccountSetupService);
    authService = TestBed.inject(AuthService);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load account data on init', () => {
    vi.spyOn(authService, 'getCurrentUser').mockReturnValue({ name: 'testuser' });
    vi.spyOn(accountSetupService, 'getAccountByUser').mockReturnValue(of({ accountNumber: '123456789012' }));
    component.ngOnInit();
    expect(accountSetupService.getAccountByUser).toHaveBeenCalled();
  });

  it('should execute transfer successfully', () => {
    vi.spyOn(transactionService, 'executeTransfer').mockReturnValue(of({ status: 'SUCCESS' }));
    component.pin = '1234';
    component.userPin = '1234';
    component.myAccountNumber = '123456789012';
    component.accountNumber = '987654321098';
    component.amount = '100';

    component.onConfirmPin();

    expect(transactionService.executeTransfer).toHaveBeenCalled();
  });
});
