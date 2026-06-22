import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { PinConfirmComponent } from './pin-confirm.component';
import { Router } from '@angular/router';
import { TransactionService, Transaction } from '../services/transaction.service';
import { NgZone } from '@angular/core';
import { RouterTestingModule } from '@angular/router/testing';

describe('PinConfirmComponent', () => {
  let component: PinConfirmComponent;
  let fixture: ComponentFixture<PinConfirmComponent>;
  let routerSpy: jasmine.SpyObj<Router>;
  let transactionServiceSpy: jasmine.SpyObj<TransactionService>;
  let ngZone: NgZone;

  beforeEach(async () => {
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    transactionServiceSpy = jasmine.createSpyObj('TransactionService', ['addTransaction', 'generateReferenceId']);
    transactionServiceSpy.generateReferenceId.and.returnValue('REF123');

    // Mock history.state
    Object.defineProperty(window, 'history', {
      value: {
        state: {
          accountNumber: '987654321012',
          amount: '150.00'
        }
      },
      writable: true
    });

    await TestBed.configureTestingModule({
      imports: [PinConfirmComponent, RouterTestingModule],
      providers: [
        { provide: Router, useValue: routerSpy },
        { provide: TransactionService, useValue: transactionServiceSpy }
      ]
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(PinConfirmComponent);
    component = fixture.componentInstance;
    ngZone = TestBed.inject(NgZone);
    fixture.detectChanges();
  });

  it('should create and extract details from history state', () => {
    expect(component).toBeTruthy();
    expect(component.accountNumber).toBe('987654321012');
    expect(component.amount).toBe('150.00');
  });

  it('should filter pin input to contain numbers only and max length 4', () => {
    const mockEvent = {
      target: {
        value: '12a345'
      }
    } as any;

    component.onPinInput(mockEvent);

    expect(mockEvent.target.value).toBe('1234');
    expect(component.pin).toBe('1234');
  });

  it('should check if pin has length less than 4', () => {
    component.pin = '123';
    component.onConfirm();
    expect(component.message).toBe('Please enter a 4-digit PIN.');
  });

  it('should handle incorrect PIN and save failed transaction', () => {
    component.pin = '9999';
    component.onConfirm();

    expect(component.message).toBe('Incorrect PIN. Transaction failed.');
    expect(transactionServiceSpy.addTransaction).toHaveBeenCalledWith(jasmine.objectContaining({
      status: 'failed',
      accountNumber: '987654321012',
      amount: '150.00',
      description: 'Incorrect PIN'
    }));
  });

  it('should process correct PIN and save success transaction asynchronously', fakeAsync(() => {
    component.pin = '1234';
    component.onConfirm();

    expect(component.isLoading).toBeTrue();
    expect(component.message).toBe('');

    tick(2000);

    expect(component.isLoading).toBeFalse();
    expect(component.isSuccess).toBeTrue();
    expect(transactionServiceSpy.addTransaction).toHaveBeenCalledWith(jasmine.objectContaining({
      status: 'completed',
      accountNumber: '987654321012',
      amount: '150.00',
      description: 'Transfer completed'
    }));
  }));

  it('should navigate to transfer on back click', () => {
    component.onBack();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/transfer']);
  });

  it('should navigate to history on go history click', () => {
    component.onGoHistory();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/history']);
  });
});
