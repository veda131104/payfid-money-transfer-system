import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SignupComponent } from './signup.component';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { AccountSetupService } from '../services/account-setup.service';
import { of, throwError } from 'rxjs';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';

describe('SignupComponent', () => {
  let component: SignupComponent;
  let fixture: ComponentFixture<SignupComponent>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let accountSetupServiceSpy: jasmine.SpyObj<AccountSetupService>;
  let router: Router;
  let alertSpy: jasmine.Spy;

  beforeEach(async () => {
    authServiceSpy = jasmine.createSpyObj('AuthService', ['signup', 'login']);
    accountSetupServiceSpy = jasmine.createSpyObj('AccountSetupService', ['sendOtp']);
    accountSetupServiceSpy.sendOtp.and.returnValue(of({}));
    alertSpy = spyOn(window, 'alert');

    await TestBed.configureTestingModule({
      imports: [
        SignupComponent,
        ReactiveFormsModule,
        NoopAnimationsModule,
        RouterTestingModule
      ],
      providers: [
        { provide: AuthService, useValue: authServiceSpy },
        { provide: AccountSetupService, useValue: accountSetupServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(SignupComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    spyOn(router, 'navigate');
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should validate password mismatch and show error', () => {
    component.form.patchValue({
      username: 'validUser',
      email: 'test@example.com',
      password: 'password123',
      confirmPassword: 'differentPassword'
    });
    component.sendOtp();
    expect(component.otpError).toBe('Passwords must match before sending OTP.');
    expect(accountSetupServiceSpy.sendOtp).not.toHaveBeenCalled();
  });

  it('should validate username minlength and show error', () => {
    component.form.patchValue({
      username: 'ab',
      email: 'test@example.com',
      password: 'password123',
      confirmPassword: 'password123'
    });
    // Trigger username minlength error
    component.form.get('username')?.markAsTouched();
    component.sendOtp();
    expect(component.otpError).toBe('Please fill in all signup fields correctly before sending OTP.');
  });

  it('should validate email format and show error', () => {
    component.form.patchValue({
      username: 'validUser',
      email: 'invalid-email',
      password: 'password123',
      confirmPassword: 'password123'
    });
    component.sendOtp();
    expect(component.otpError).toBe('Please fill in all signup fields correctly before sending OTP.');
  });

  it('should validate password minlength and show error', () => {
    component.form.patchValue({
      username: 'validUser',
      email: 'test@example.com',
      password: 'pass',
      confirmPassword: 'pass'
    });
    component.sendOtp();
    expect(component.otpError).toBe('Please fill in all signup fields correctly before sending OTP.');
  });

  it('should show generic error if form is invalid for other reasons', () => {
    component.form.patchValue({
      username: '',
      email: '',
      password: '',
      confirmPassword: ''
    });
    component.sendOtp();
    expect(component.otpError).toBe('Please fill in all signup fields correctly before sending OTP.');
  });

  it('should submit successfully and navigate to home if login fails after signup', () => {
    authServiceSpy.signup.and.returnValue(of({ name: 'validUser', email: 'test@example.com' }));
    authServiceSpy.login.and.returnValue(throwError(() => new Error('Login error')));
    component.form.patchValue({
      username: 'validUser',
      email: 'test@example.com',
      password: 'password123',
      confirmPassword: 'password123'
    });
    component.step = 'VERIFIED';
    component.submit();
    expect(authServiceSpy.signup).toHaveBeenCalledWith({
      name: 'validUser',
      email: 'test@example.com',
      password: 'password123'
    });
    expect(alertSpy).toHaveBeenCalledWith('Signup succeeded but automatic login failed. Please sign in.');
    expect(router.navigate).toHaveBeenCalledWith(['/']);
  });

  it('should submit and login successfully (first login -> account setup)', () => {
    jasmine.clock().install();
    authServiceSpy.signup.and.returnValue(of({ name: 'validUser', email: 'test@example.com' }));
    authServiceSpy.login.and.returnValue(of({
      userId: 1,
      name: 'validUser',
      email: 'test@example.com',
      token: 'jwt',
      tokenType: 'Bearer',
      firstLogin: true,
      rememberToken: 'token123'
    }));
    accountSetupServiceSpy.sendOtp.and.returnValue(of({}));
    component.form.patchValue({
      username: 'validUser',
      email: 'test@example.com',
      password: 'password123',
      confirmPassword: 'password123'
    });
    component.step = 'VERIFIED';
    component.submit();
    jasmine.clock().tick(1000);
    expect(router.navigate).toHaveBeenCalledWith(['/account-setup']);
    jasmine.clock().uninstall();
  });

  it('should submit and login successfully (not first login -> dashboard)', () => {
    authServiceSpy.signup.and.returnValue(of({ name: 'validUser', email: 'test@example.com' }));
    authServiceSpy.login.and.returnValue(of({
      userId: 1,
      name: 'validUser',
      email: 'test@example.com',
      token: 'jwt',
      tokenType: 'Bearer',
      firstLogin: false
    }));
    component.form.patchValue({
      username: 'validUser',
      email: 'test@example.com',
      password: 'password123',
      confirmPassword: 'password123'
    });
    component.step = 'VERIFIED';
    component.submit();
    expect(router.navigate).toHaveBeenCalledWith(['/dashboard']);
  });

  it('should handle signup error with status 409', () => {
    const errorResponse = {
      status: 409,
      error: { message: 'Username is already taken.' }
    };
    authServiceSpy.signup.and.returnValue(throwError(() => errorResponse));

    component.form.patchValue({
      username: 'takenUser',
      email: 'test@example.com',
      password: 'password123',
      confirmPassword: 'password123'
    });
    component.step = 'VERIFIED';
    component.submit();

    expect(alertSpy).toHaveBeenCalledWith('Username is already taken.');
  });

  it('should handle signup error with status 409 with fallback message', () => {
    const errorResponse = {
      status: 409,
      error: null
    };
    authServiceSpy.signup.and.returnValue(throwError(() => errorResponse));

    component.form.patchValue({
      username: 'takenUser',
      email: 'test@example.com',
      password: 'password123',
      confirmPassword: 'password123'
    });
    component.step = 'VERIFIED';
    component.submit();

    expect(alertSpy).toHaveBeenCalledWith('Signup failed. Please try again.');
  });

  it('should handle generic signup error', () => {
    const errorResponse = {
      status: 500,
      error: { message: 'Internal server error.' }
    };
    authServiceSpy.signup.and.returnValue(throwError(() => errorResponse));

    component.form.patchValue({
      username: 'validUser',
      email: 'test@example.com',
      password: 'password123',
      confirmPassword: 'password123'
    });
    component.step = 'VERIFIED';
    component.submit();

    expect(alertSpy).toHaveBeenCalledWith('Internal server error.');
  });

  it('should handle generic signup error with fallback message', () => {
    const errorResponse = {
      status: 500,
      error: null
    };
    authServiceSpy.signup.and.returnValue(throwError(() => errorResponse));

    component.form.patchValue({
      username: 'validUser',
      email: 'test@example.com',
      password: 'password123',
      confirmPassword: 'password123'
    });
    component.step = 'VERIFIED';
    component.submit();

    expect(alertSpy).toHaveBeenCalledWith('Signup failed. Please try again.');
  });
});

