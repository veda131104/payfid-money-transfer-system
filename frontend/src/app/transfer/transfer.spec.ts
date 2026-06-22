import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TransferComponent } from './transfer.component';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { TransactionService } from '../services/transaction.service';
import { AccountSetupService } from '../services/account-setup.service';
import { of } from 'rxjs';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { FormsModule } from '@angular/forms';
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
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load account data on init', () => {
    spyOn(authService, 'getCurrentUser').and.returnValue({ name: 'testuser', email: 'testuser@company.com' });
    spyOn(accountSetupService, 'getAccountByUser').and.returnValue(of({ accountNumber: '123456789012' }));
    component.ngOnInit();
    expect(accountSetupService.getAccountByUser).toHaveBeenCalled();
  });

  it('should execute transfer successfully', () => {
    spyOn(transactionService, 'executeTransfer').and.returnValue(of({ status: 'SUCCESS' }));
    component.pin = '1234';
    component.userPin = '1234';
    component.myAccountNumber = '123456789012';
    component.accountNumber = '987654321098';
    component.amount = '100';

    component.onConfirmPin();

    expect(transactionService.executeTransfer).toHaveBeenCalled();
  });
});
