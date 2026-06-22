import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { TransferComponent } from './transfer.component';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { TransactionService } from '../services/transaction.service';
import { AccountSetupService } from '../services/account-setup.service';
import { of, throwError } from 'rxjs';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../services/auth.service';
import { Router } from '@angular/router';

describe('TransferComponent', () => {
  let component: TransferComponent;
  let fixture: ComponentFixture<TransferComponent>;
  let transactionService: TransactionService;
  let accountSetupService: AccountSetupService;
  let authService: AuthService;
  let router: Router;
  let alertSpy: jasmine.Spy;

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
    alertSpy = spyOn(window, 'alert');

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
      arc: jasmine.createSpy('arc'),
      rect: jasmine.createSpy('rect'),
      clip: jasmine.createSpy('clip'),
      translate: jasmine.createSpy('translate'),
      scale: jasmine.createSpy('scale'),
      rotate: jasmine.createSpy('rotate'),
    };
    Object.defineProperty(HTMLCanvasElement.prototype, 'getContext', {
      value: jasmine.createSpy('getContext').and.returnValue(dummyCtx)
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
    router = TestBed.inject(Router);
    spyOn(router, 'navigate');
  });

  it('should create', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('should load account data on init', () => {
    spyOn(authService, 'getCurrentUser').and.returnValue({ name: 'testuser', email: 'testuser@company.com' });
    spyOn(accountSetupService, 'getAccountByUser').and.returnValue(of({ accountNumber: '123456789012', pin: '4321' }));
    component.ngOnInit();
    expect(accountSetupService.getAccountByUser).toHaveBeenCalledWith('testuser');
    expect(component.myAccountNumber).toBe('123456789012');
    expect(component.userPin).toBe('4321');
  });

  it('should handle getAccountByUser error on init', () => {
    spyOn(authService, 'getCurrentUser').and.returnValue({ name: 'testuser', email: 'testuser@company.com' });
    spyOn(accountSetupService, 'getAccountByUser').and.returnValue(throwError(() => new Error('Bank details error')));
    spyOn(console, 'error');
    component.ngOnInit();
    expect(console.error).toHaveBeenCalled();
  });

  it('should format account number input (digits only, max 18)', () => {
    const inputEl = document.createElement('input');
    inputEl.value = '123-abc-45678901234567890';
    component.onAccountNumberInput({ target: inputEl } as any);
    expect(inputEl.value).toBe('123456789012345678');
    expect(component.accountNumber).toBe('123456789012345678');
  });

  it('should format amount input (digits and single decimal point only)', () => {
    const inputEl = document.createElement('input');
    inputEl.value = '10a.5.6';
    component.onAmountInput({ target: inputEl } as any);
    expect(inputEl.value).toBe('10.56');
    expect(component.amount).toBe('10.56');
  });

  it('should set description on input event', () => {
    const inputEl = document.createElement('input');
    inputEl.value = 'Test description';
    component.onDescriptionInput({ target: inputEl } as any);
    expect(component.description).toBe('Test description');
  });

  it('should format PIN input (digits only, max 4)', () => {
    const inputEl = document.createElement('input');
    inputEl.value = '12ab345';
    component.onPinInput({ target: inputEl } as any);
    expect(inputEl.value).toBe('1234');
    expect(component.pin).toBe('1234');
  });

  it('should toggle PIN visibility', () => {
    expect(component.pinVisible).toBeFalse();
    component.togglePinVisibility();
    expect(component.pinVisible).toBeTrue();
    component.togglePinVisibility();
    expect(component.pinVisible).toBeFalse();
  });

  it('should validate form before opening PIN modal', () => {
    fixture.detectChanges();
    component.myAccountNumber = '111122223333';

    // Empty account number
    component.accountNumber = '';
    component.onSendMoney();
    expect(alertSpy).toHaveBeenCalledWith("Please enter the recipient's account number.");

    // Length < 9
    component.accountNumber = '123';
    component.onSendMoney();
    expect(alertSpy).toHaveBeenCalledWith('Account number must be between 9 and 18 digits.');

    // Self-transfer
    component.accountNumber = '111122223333';
    component.onSendMoney();
    expect(alertSpy).toHaveBeenCalledWith('Self-transfer is not allowed. Please enter a different recipient account number.');

    // Empty amount
    component.accountNumber = '999999999999';
    component.amount = '';
    component.onSendMoney();
    expect(alertSpy).toHaveBeenCalledWith('Please enter the amount to transfer.');

    // Amount <= 0
    component.amount = '-5';
    component.onSendMoney();
    expect(alertSpy).toHaveBeenCalledWith('Amount must be greater than zero.');

    // No PIN set
    component.amount = '100';
    component.userPin = '';
    component.onSendMoney();
    expect(alertSpy).toHaveBeenCalledWith('Transaction Security: No PIN detected. Please go to your Profile and set a 4-digit PIN before making transfers.');

    // Successful validation setup
    component.userPin = '1234';
    component.onSendMoney();
    expect(component.successDetails.amount).toBe('100');
    expect(component.showPinModal).toBeTrue();
    expect(component.pin).toBe('');
    expect(component.pinMessage).toBe('');
  });

  it('should validate PIN format on confirm PIN', () => {
    component.pin = '';
    component.onConfirmPin();
    expect(component.pinMessage).toBe('Please enter a 4-digit PIN.');

    component.pin = '12';
    component.onConfirmPin();
    expect(component.pinMessage).toBe('Please enter a 4-digit PIN.');
  });

  it('should handle incorrect PIN, add failed txn to history, and auto-close modal after timeout', fakeAsync(() => {
    spyOn(transactionService, 'addTransaction');
    component.userPin = '4321';
    component.pin = '1111';
    component.accountNumber = '999999999999';
    component.amount = '50';
    component.showPinModal = true;

    component.onConfirmPin();

    expect(transactionService.addTransaction).toHaveBeenCalled();
    expect(component.pinMessage).toBe('Incorrect PIN. Transaction failed.');
    expect(component.pin).toBe('');
    expect(component.pinVisible).toBeFalse();

    tick(5000);
    expect(component.showPinModal).toBeFalse();
  }));

  it('should restrict confirm if myAccountNumber is default placeholder', () => {
    component.userPin = '1234';
    component.pin = '1234';
    component.myAccountNumber = '000000000000';
    component.onConfirmPin();
    expect(component.pinMessage).toBe('Loading your account details... Please try again in a moment.');
  });

  it('should execute transfer successfully', () => {
    const txnResponse = { transactionId: 555, status: 'SUCCESS' };
    spyOn(transactionService, 'executeTransfer').and.returnValue(of(txnResponse));
    spyOn(transactionService, 'addTransaction');

    component.pin = '1234';
    component.userPin = '1234';
    component.myAccountNumber = '123456789012';
    component.accountNumber = '987654321098';
    component.amount = '100';
    component.showPinModal = true;

    component.onConfirmPin();

    expect(transactionService.executeTransfer).toHaveBeenCalledWith({
      fromAccountNumber: '123456789012',
      toAccountNumber: '987654321098',
      amount: 100
    });
    expect(component.pinLoading).toBeFalse();
    expect(component.showPinModal).toBeFalse();
    expect(component.isSuccess).toBeTrue();
    expect(transactionService.addTransaction).toHaveBeenCalled();
  });

  it('should handle executeTransfer error path', () => {
    const errorResponse = { error: { message: 'Insufficient balance' }, status: 400 };
    spyOn(transactionService, 'executeTransfer').and.returnValue(throwError(() => errorResponse));

    component.pin = '1234';
    component.userPin = '1234';
    component.myAccountNumber = '123456789012';
    component.accountNumber = '987654321098';
    component.amount = '100';

    component.onConfirmPin();

    expect(component.pinLoading).toBeFalse();
    expect(component.pinMessage).toBe('Insufficient balance');
  });

  it('should close PIN modal and cancel timer', fakeAsync(() => {
    component.showPinModal = true;
    component.pin = '1234';
    component.pinMessage = 'Err';
    component.pinErrorTimer = setTimeout(() => {}, 1000);

    component.closePinModal();

    expect(component.showPinModal).toBeFalse();
    expect(component.pin).toBe('');
    expect(component.pinMessage).toBe('');
  }));

  it('should reset state on new transfer', () => {
    component.isSuccess = true;
    component.accountNumber = '123456789012';
    component.amount = '500';
    component.description = 'Desc';

    component.onNewTransfer();

    expect(component.isSuccess).toBeFalse();
    expect(component.accountNumber).toBe('');
    expect(component.amount).toBe('');
    expect(component.description).toBe('');
  });

  it('should navigate to history', () => {
    component.onCheckHistory();
    expect(router.navigate).toHaveBeenCalledWith(['/history']);
  });

  it('should logout successfully', () => {
    spyOn(authService, 'clearSession');
    component.onLogout();
    expect(authService.clearSession).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/']);
  });

  it('should get initials from userName', () => {
    component.userName = 'Alice Smith';
    expect(component.getInitials()).toBe('AS');

    component.userName = 'Bob';
    expect(component.getInitials()).toBe('B');

    component.userName = '';
    expect(component.getInitials()).toBe('U');
  });
});
