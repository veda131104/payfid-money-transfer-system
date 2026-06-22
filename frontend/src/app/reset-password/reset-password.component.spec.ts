import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ResetPasswordComponent } from './reset-password.component';
import { ReactiveFormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { of, throwError } from 'rxjs';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { PopupService } from '../services/popup.service';

describe('ResetPasswordComponent', () => {
  let component: ResetPasswordComponent;
  let fixture: ComponentFixture<ResetPasswordComponent>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let routerSpy: jasmine.SpyObj<Router>;
  let alertSpy: jasmine.Spy;
  let queryParamMapGetSpy: jasmine.Spy;

  beforeEach(async () => {
    authServiceSpy = jasmine.createSpyObj('AuthService', ['resetPassword']);
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    queryParamMapGetSpy = jasmine.createSpy('get').and.returnValue('valid-token');

    const activatedRouteMock = {
      snapshot: {
        queryParamMap: {
          get: queryParamMapGetSpy
        }
      }
    };

    await TestBed.configureTestingModule({
      imports: [
        ResetPasswordComponent,
        ReactiveFormsModule,
        NoopAnimationsModule
      ],
      providers: [
        { provide: AuthService, useValue: authServiceSpy },
        { provide: Router, useValue: routerSpy },
        { provide: ActivatedRoute, useValue: activatedRouteMock }
      ]
    }).compileComponents();

    const popupService = TestBed.inject(PopupService);
    const originalAlert = popupService.alert;
    alertSpy = jasmine.createSpy('alert');
    popupService.alert = (msg: string, title?: string) => {
      alertSpy(msg);
      originalAlert.call(popupService, msg, title || 'Alert');
    };
  });

  it('should create and extract token on init', () => {
    fixture = TestBed.createComponent(ResetPasswordComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component).toBeTruthy();
    expect(component.token).toBe('valid-token');
    expect(routerSpy.navigate).not.toHaveBeenCalled();
  });

  it('should alert and navigate home if token is missing on init', () => {
    queryParamMapGetSpy.and.returnValue(null);
    fixture = TestBed.createComponent(ResetPasswordComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.token).toBeNull();
    expect(alertSpy).toHaveBeenCalledWith('Invalid reset link. Token is missing.');
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/']);
  });

  it('should validate password mismatch', () => {
    fixture = TestBed.createComponent(ResetPasswordComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    component.form.patchValue({
      password: 'password123',
      confirmPassword: 'differentPassword'
    });

    component.submit();

    expect(component.form.invalid).toBeTrue();
    expect(authServiceSpy.resetPassword).not.toHaveBeenCalled();
  });

  it('should reset password successfully and navigate to dashboard', () => {
    fixture = TestBed.createComponent(ResetPasswordComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    authServiceSpy.resetPassword.and.returnValue(of({}));
    component.form.patchValue({
      password: 'password123',
      confirmPassword: 'password123'
    });

    component.submit();

    expect(authServiceSpy.resetPassword).toHaveBeenCalledWith({
      token: 'valid-token',
      newPassword: 'password123'
    });
    expect(alertSpy).toHaveBeenCalledWith('Password has been reset successfully! Redirecting to sign-in...');
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/']);
  });

  it('should handle reset password error response', () => {
    fixture = TestBed.createComponent(ResetPasswordComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    authServiceSpy.resetPassword.and.returnValue(throwError(() => new Error('Expired token')));
    component.form.patchValue({
      password: 'password123',
      confirmPassword: 'password123'
    });

    component.submit();

    expect(alertSpy).toHaveBeenCalledWith('Failed to reset password. Link may be expired.');
    expect(component.isSending).toBeFalse();
  });
});
