import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SignupComponent } from './signup.component';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { of, throwError } from 'rxjs';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';

describe('SignupComponent', () => {
  let component: SignupComponent;
  let fixture: ComponentFixture<SignupComponent>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let routerSpy: jasmine.SpyObj<Router>;
  let alertSpy: jasmine.Spy;

  beforeEach(async () => {
    authServiceSpy = jasmine.createSpyObj('AuthService', ['signup']);
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);
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
        { provide: Router, useValue: routerSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(SignupComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should validate password mismatch and show alert', () => {
    component.form.patchValue({
      username: 'validUser',
      email: 'test@example.com',
      password: 'password123',
      confirmPassword: 'differentPassword'
    });
    component.submit();
    expect(alertSpy).toHaveBeenCalledWith('Validation Error: Passwords do not match.');
    expect(authServiceSpy.signup).not.toHaveBeenCalled();
  });

  it('should validate username minlength and show alert', () => {
    component.form.patchValue({
      username: 'ab',
      email: 'test@example.com',
      password: 'password123',
      confirmPassword: 'password123'
    });
    // Trigger username minlength error
    component.form.get('username')?.markAsTouched();
    component.submit();
    expect(alertSpy).toHaveBeenCalledWith('Validation Error: Username must be at least 3 characters long.');
  });

  it('should validate email format and show alert', () => {
    component.form.patchValue({
      username: 'validUser',
      email: 'invalid-email',
      password: 'password123',
      confirmPassword: 'password123'
    });
    component.submit();
    expect(alertSpy).toHaveBeenCalledWith('Validation Error: Please enter a valid email address.');
  });

  it('should validate password minlength and show alert', () => {
    component.form.patchValue({
      username: 'validUser',
      email: 'test@example.com',
      password: 'pass',
      confirmPassword: 'pass'
    });
    component.submit();
    expect(alertSpy).toHaveBeenCalledWith('Validation Error: Password must be at least 8 characters long.');
  });

  it('should show generic alert if form is invalid for other reasons', () => {
    component.form.patchValue({
      username: '',
      email: '',
      password: '',
      confirmPassword: ''
    });
    component.submit();
    expect(alertSpy).toHaveBeenCalledWith('Please fill in all fields correctly.');
  });

  it('should submit successfully and navigate to home', () => {
    authServiceSpy.signup.and.returnValue(of({ name: 'validUser', email: 'test@example.com' }));
    component.form.patchValue({
      username: 'validUser',
      email: 'test@example.com',
      password: 'password123',
      confirmPassword: 'password123'
    });
    component.submit();
    expect(authServiceSpy.signup).toHaveBeenCalledWith({
      name: 'validUser',
      email: 'test@example.com',
      password: 'password123'
    });
    expect(alertSpy).toHaveBeenCalledWith('Signup successful! Please log in to complete your account setup.');
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/']);
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
    component.submit();

    expect(alertSpy).toHaveBeenCalledWith('Username is already taken.');
    expect(component.form.get('username')?.value).toBe('');
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
    component.submit();

    expect(alertSpy).toHaveBeenCalledWith('Username or email is already in use.');
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
    component.submit();

    expect(alertSpy).toHaveBeenCalledWith('Signup failed. Please try again.');
  });
});
