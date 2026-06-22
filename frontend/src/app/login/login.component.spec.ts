import { ComponentFixture, TestBed } from '@angular/core/testing';
import { LoginComponent } from './login.component';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { of, throwError } from 'rxjs';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';

describe('LoginComponent', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let router: Router;
  let alertSpy: jasmine.Spy;

  beforeEach(async () => {
    authServiceSpy = jasmine.createSpyObj('AuthService', ['login']);
    alertSpy = spyOn(window, 'alert');

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
    Object.defineProperty(window, 'localStorage', { value: localStorageMock, writable: true });
    localStorage.clear();

    await TestBed.configureTestingModule({
      imports: [
        LoginComponent,
        ReactiveFormsModule,
        NoopAnimationsModule,
        RouterTestingModule
      ],
      providers: [
        { provide: AuthService, useValue: authServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    spyOn(router, 'navigate');
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should validate form and show alert when invalid', () => {
    component.form.patchValue({ name: '', password: '' });
    component.submit();
    expect(alertSpy).toHaveBeenCalledWith('Please enter your username and password.');
    expect(authServiceSpy.login).not.toHaveBeenCalled();
  });

  it('should call authService.login and handle first login success with rememberMe', () => {
    const mockResponse = {
      userId: 1,
      name: 'validUser',
      email: 'test@example.com',
      token: 'some-token',
      tokenType: 'Bearer',
      rememberToken: 'token123',
      firstLogin: true
    };
    authServiceSpy.login.and.returnValue(of(mockResponse));

    component.form.patchValue({
      name: 'validUser',
      password: 'validPassword123',
      rememberMe: true
    });

    component.submit();

    expect(authServiceSpy.login).toHaveBeenCalledWith({
      name: 'validUser',
      password: 'validPassword123',
      rememberMe: true
    });
    expect(localStorage.getItem('remember_token')).toBe('token123');
    expect(router.navigate).toHaveBeenCalledWith(['/account-setup']);
  });

  it('should call authService.login and handle dashboard success without rememberMe', () => {
    const mockResponse = {
      userId: 1,
      name: 'validUser',
      email: 'test@example.com',
      token: 'some-token',
      tokenType: 'Bearer',
      rememberToken: 'token123',
      firstLogin: false
    };
    authServiceSpy.login.and.returnValue(of(mockResponse));

    component.form.patchValue({
      name: 'validUser',
      password: 'validPassword123',
      rememberMe: false
    });

    component.submit();

    expect(localStorage.getItem('remember_token')).toBeNull();
    expect(router.navigate).toHaveBeenCalledWith(['/dashboard']);
  });

  it('should handle login error response', () => {
    authServiceSpy.login.and.returnValue(throwError(() => new Error('Login failed')));

    component.form.patchValue({
      name: 'validUser',
      password: 'validPassword123',
      rememberMe: false
    });

    component.submit();

    expect(alertSpy).toHaveBeenCalledWith('Invalid username or password.');
    // Form should have been reset
    expect(component.form.get('name')?.value).toBe('');
  });
});
