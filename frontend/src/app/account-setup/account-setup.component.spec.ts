import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AccountSetupComponent } from './account-setup.component';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AccountSetupService } from '../services/account-setup.service';
import { AuthService } from '../services/auth.service';
import { of, throwError } from 'rxjs';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

describe('AccountSetupComponent', () => {
  let component: AccountSetupComponent;
  let fixture: ComponentFixture<AccountSetupComponent>;
  let svcSpy: jasmine.SpyObj<AccountSetupService>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let routerSpy: jasmine.SpyObj<Router>;
  let alertSpy: jasmine.Spy;

  beforeEach(async () => {
    svcSpy = jasmine.createSpyObj('AccountSetupService', [
      'getAccountByUser',
      'sendOtp',
      'verifyOtp',
      'create',
      'getAccountByNumber',
      'setPin',
      'updateAccount'
    ]);
    authServiceSpy = jasmine.createSpyObj('AuthService', ['getCurrentUser']);
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    alertSpy = spyOn(window, 'alert');

    // Default return value to prevent ngOnInit crashes
    authServiceSpy.getCurrentUser.and.returnValue({ name: 'Alice', email: 'alice@company.com' });
    svcSpy.getAccountByUser.and.returnValue(throwError(() => new Error('Not found')));

    await TestBed.configureTestingModule({
      imports: [
        AccountSetupComponent,
        ReactiveFormsModule,
        NoopAnimationsModule
      ],
      providers: [
        { provide: AccountSetupService, useValue: svcSpy },
        { provide: AuthService, useValue: authServiceSpy },
        { provide: Router, useValue: routerSpy }
      ]
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(AccountSetupComponent);
    component = fixture.componentInstance;
  });

  it('should create and handle OnInit when account does not exist', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
    expect(routerSpy.navigate).not.toHaveBeenCalled();
  });

  it('should redirect to dashboard on OnInit when account already exists', () => {
    svcSpy.getAccountByUser.and.returnValue(of({ accountNumber: '123456789012' }));
    fixture.detectChanges();
    expect(alertSpy).toHaveBeenCalledWith('Your account is already set up! Redirecting to dashboard.');
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/dashboard']);
  });

  it('should stay on page if user session is absent on OnInit', () => {
    authServiceSpy.getCurrentUser.and.returnValue(null);
    fixture.detectChanges();
    expect(routerSpy.navigate).not.toHaveBeenCalled();
  });


  it('should submit form and handle alerts when form is invalid', () => {
    fixture.detectChanges();
    component.form.patchValue({ accountNumber: '' });
    component.submit();
    expect(alertSpy).toHaveBeenCalledWith('Please fill in all mandatory fields correctly before proceeding.');
  });

  it('should alert if accountNumber pattern fails regex', () => {
    fixture.detectChanges();
    component.form.patchValue({
      accountNumber: '123-abc',
      bankName: 'Test Bank',
      branchName: 'Test Branch',
      address: 'Test Addr',
      ifscCode: 'IFSC123',
      email: 'test@test.com',
      phoneNumber: '12345678',
      creditCardNumber: '1111222233334444',
      cvv: '123',
      expiryDate: '12/28'
    });
    component.submit();
    expect(alertSpy).toHaveBeenCalledWith('Validation Error: Account Number must be between 9 and 18 digits (numbers only).');
  });

  it('should alert and redirect to /login if user session is missing during submit', () => {
    fixture.detectChanges();
    component.form.patchValue({
      accountNumber: '1234567890',
      bankName: 'Test Bank',
      branchName: 'Test Branch',
      address: 'Test Addr',
      ifscCode: 'IFSC123',
      email: 'test@test.com',
      phoneNumber: '1234567890',
      creditCardNumber: '1111222233334444',
      cvv: '123',
      expiryDate: '12/28'
    });
    authServiceSpy.getCurrentUser.and.returnValue(null);
    component.submit();
    expect(alertSpy).toHaveBeenCalledWith('You are not logged in. Please log in again and retry.');
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('should submit successfully and navigate to dashboard', () => {
    fixture.detectChanges();
    component.form.patchValue({
      accountNumber: '1234567890',
      bankName: 'Test Bank',
      branchName: 'Test Branch',
      address: 'Test Addr',
      ifscCode: 'IFSC123',
      email: 'test@test.com',
      phoneNumber: '1234567890',
      creditCardNumber: '1111222233334444',
      cvv: '123',
      expiryDate: '12/28'
    });
    svcSpy.create.and.returnValue(of({}));
    component.submit();
    expect(svcSpy.create).toHaveBeenCalled();
    expect(alertSpy).toHaveBeenCalledWith('Account setup completed successfully!');
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/dashboard']);
  });

  it('should alert error if submit save API fails', () => {
    fixture.detectChanges();
    component.form.patchValue({
      accountNumber: '1234567890',
      bankName: 'Test Bank',
      branchName: 'Test Branch',
      address: 'Test Addr',
      ifscCode: 'IFSC123',
      email: 'test@test.com',
      phoneNumber: '1234567890',
      creditCardNumber: '1111222233334444',
      cvv: '123',
      expiryDate: '12/28'
    });
    svcSpy.create.and.returnValue(throwError(() => ({ error: { message: 'Database error' } })));
    component.submit();
    expect(alertSpy).toHaveBeenCalledWith('Error saving: Database error');
  });

  it('should load balance if accountNumber exists', () => {
    fixture.detectChanges();
    component.accountData = { accountNumber: '1234567890' };
    svcSpy.getAccountByNumber.and.returnValue(of({ balance: 550.25 }));
    component.loadAccountBalance();
    expect(component.currentBalance).toBe(550.25);
  });

  it('should alert if pinForm is invalid', () => {
    fixture.detectChanges();
    component.pinForm.patchValue({ pin: '12' });
    component.setPin();
    expect(alertSpy).toHaveBeenCalledWith('Please enter a valid PIN (4-6 digits)');
  });

  it('should alert if pins do not match', () => {
    fixture.detectChanges();
    component.pinForm.patchValue({ pin: '1234', confirmPin: '5678' });
    component.setPin();
    expect(alertSpy).toHaveBeenCalledWith('PINs do not match');
  });

  it('should alert if setting PIN without active user session', () => {
    fixture.detectChanges();
    component.pinForm.patchValue({ pin: '1234', confirmPin: '1234' });
    authServiceSpy.getCurrentUser.and.returnValue(null);
    component.setPin();
    expect(alertSpy).toHaveBeenCalledWith('User not found. Please log in again.');
  });

  it('should set pin successfully', () => {
    fixture.detectChanges();
    component.pinForm.patchValue({ pin: '1234', confirmPin: '1234' });
    svcSpy.setPin.and.returnValue(of({}));
    component.setPin();
    expect(svcSpy.setPin).toHaveBeenCalledWith('Alice', '1234');
    expect(alertSpy).toHaveBeenCalledWith('PIN set successfully!');
    expect(component.showPinSetup).toBeFalse();
  });

  it('should alert error if set PIN fails', () => {
    fixture.detectChanges();
    component.pinForm.patchValue({ pin: '1234', confirmPin: '1234' });
    svcSpy.setPin.and.returnValue(throwError(() => new Error('Forbidden')));
    component.setPin();
    expect(alertSpy).toHaveBeenCalledWith('Error setting PIN: Forbidden');
  });

  it('should open edit details and prepopulate form', () => {
    fixture.detectChanges();
    component.accountData = {
      email: 'bob@bob.com',
      phoneNumber: '99999',
      address: 'Old St',
      creditCardNumber: '5555',
      cvv: '999',
      expiryDate: '10/30'
    };
    component.openEditDetails();
    expect(component.showEditDetails).toBeTrue();
    expect(component.editDetailsForm.get('email')?.value).toBe('bob@bob.com');
  });

  it('should alert when editDetailsForm is invalid', () => {
    fixture.detectChanges();
    component.showEditDetails = true;
    component.editDetailsForm.patchValue({ email: '' });
    component.saveEditDetails();
    expect(alertSpy).toHaveBeenCalledWith('Please fill in all fields correctly');
  });

  it('should alert when saving edit details without user session', () => {
    fixture.detectChanges();
    component.accountData = { email: 'bob@bob.com' };
    component.editDetailsForm.patchValue({
      email: 'new@bob.com',
      phoneNumber: '1234567890',
      address: 'Addr',
      creditCardNumber: '1111222233334444',
      cvv: '123',
      expiryDate: '12/28'
    });
    authServiceSpy.getCurrentUser.and.returnValue(null);
    component.saveEditDetails();
    expect(alertSpy).toHaveBeenCalledWith('User not found. Please log in again.');
  });

  it('should save edit details successfully', () => {
    fixture.detectChanges();
    component.accountData = { email: 'bob@bob.com', address: 'Old' };
    component.editDetailsForm.patchValue({
      email: 'new@bob.com',
      phoneNumber: '1234567890',
      address: 'New',
      creditCardNumber: '1111222233334444',
      cvv: '123',
      expiryDate: '12/28'
    });
    const updatedMock = { email: 'new@bob.com', address: 'New' };
    svcSpy.updateAccount.and.returnValue(of(updatedMock));
    component.saveEditDetails();

    expect(svcSpy.updateAccount).toHaveBeenCalled();
    expect(component.accountData).toEqual(updatedMock);
    expect(component.showEditDetails).toBeFalse();
    expect(alertSpy).toHaveBeenCalledWith('Details updated successfully!');
  });

  it('should alert error if saveEditDetails API fails', () => {
    fixture.detectChanges();
    component.accountData = { email: 'bob@bob.com' };
    component.editDetailsForm.patchValue({
      email: 'new@bob.com',
      phoneNumber: '1234567890',
      address: 'New',
      creditCardNumber: '1111222233334444',
      cvv: '123',
      expiryDate: '12/28'
    });
    svcSpy.updateAccount.and.returnValue(throwError(() => new Error('Server error')));
    component.saveEditDetails();
    expect(alertSpy).toHaveBeenCalledWith('Error updating details: Server error');
  });

  it('should cancel edit details and navigate to dashboard on complete setup', () => {
    fixture.detectChanges();
    component.cancelEditDetails();
    expect(component.showEditDetails).toBeFalse();

    component.completeSetup();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/dashboard']);
  });
});
