import { CommonModule } from '@angular/common';
import { Component, OnDestroy } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import {
  AbstractControl,
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  Validators
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { AuthService } from '../services/auth.service';
import { AccountSetupService } from '../services/account-setup.service';

@Component({
  selector: 'app-signup',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    RouterLink,
    ReactiveFormsModule
  ],
  templateUrl: './signup.component.html',
  styleUrl: './signup.component.scss'
})
export class SignupComponent implements OnDestroy {
  form!: FormGroup;
  loading = false;
  step: 'SIGNUP' | 'OTP_SENT' | 'VERIFIED' = 'SIGNUP';
  otpSending = false;
  otpToastMessage: string | null = null;
  otpError: string | null = null;
  otpAttemptError: string | null = null;
  otpExpirySeconds = 300;
  otpCountdown = '05:00';
  otpAttemptsRemaining = 3;
  readonly maxOtpAttempts = 3;
  resendCooldownSeconds = 30;
  resendSecondsRemaining = 30;
  resendDisabled = true;
  private otpTimerId: ReturnType<typeof setInterval> | null = null;
  private resendTimerId: ReturnType<typeof setTimeout> | null = null;
  private resendIntervalId: ReturnType<typeof setInterval> | null = null;

  constructor(
    private readonly fb: FormBuilder,
    private readonly authService: AuthService,
    private readonly accountSetupService: AccountSetupService,
    private readonly router: Router
  ) {
    this.form = this.fb.nonNullable.group(
      {
        username: ['', [Validators.required, Validators.minLength(3)]],
        email: ['', [Validators.required, Validators.email]],
        password: ['', [Validators.required, Validators.minLength(8)]],
        confirmPassword: ['', [Validators.required]],
        otp: ['']
      },
      { validators: [passwordsMatch] }
    );

    this.setEmailControlState();
  }

  private setEmailControlState(): void {
    const emailControl = this.form.get('email');
    if (!emailControl) {
      return;
    }

    if (this.step === 'SIGNUP') {
      emailControl.enable({ emitEvent: false });
    } else {
      emailControl.disable({ emitEvent: false });
    }
  }

  public get isSignupLocked(): boolean {
    return this.step !== 'SIGNUP';
  }

  sendOtp(): void {
    if (this.step !== 'SIGNUP') {
      return;
    }

    this.otpError = null;
    this.otpAttemptError = null;

    const password = this.form.get('password')?.value;
    const confirmPassword = this.form.get('confirmPassword')?.value;

    if (password !== confirmPassword) {
      this.otpError = 'Passwords must match before sending OTP.';
      return;
    }

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.otpError = 'Please fill in all signup fields correctly before sending OTP.';
      return;
    }

    const email = this.form.get('email')?.value;
    const username = this.form.get('username')?.value;

    if (!username || !email || !password) {
      this.otpError = 'Please complete signup details before sending OTP.';
      return;
    }

    this.otpSending = true;
    this.accountSetupService.sendOtp({ contact: email }).subscribe({
      next: () => this.onOtpSent(email),
      error: (err) => {
        this.otpSending = false;
        this.otpError = err?.error?.message || 'Failed to send OTP. Please try again.';
      }
    });
  }

  private onOtpSent(email: string): void {
    this.otpSending = false;
    this.step = 'OTP_SENT';
    this.setEmailControlState();
    this.otpToastMessage = `OTP sent to ${email}`;
    this.otpError = null;
    this.otpAttemptError = null;
    this.otpAttemptsRemaining = this.maxOtpAttempts;
    this.otpExpirySeconds = 300;
    this.resendSecondsRemaining = this.resendCooldownSeconds;
    this.updateOtpCountdown();
    this.startOtpTimer();
    this.startResendCooldown();
    setTimeout(() => (this.otpToastMessage = null), 2000);
  }

  verifyOtp(): void {
    if (this.step !== 'OTP_SENT') {
      return;
    }

    if (this.otpExpirySeconds <= 0) {
      this.otpAttemptError = 'OTP has expired. Please resend OTP.';
      return;
    }

    const { email, otp } = this.form.getRawValue();

    if (!otp || !/^[0-9]{6}$/.test(otp)) {
      this.otpAttemptError = 'Please enter the 6-digit OTP.';
      return;
    }

    this.accountSetupService.verifyOtp({ contact: email, otp }).subscribe({
      next: (resp) => {
        if (resp?.verified) {
          this.step = 'VERIFIED';
          this.otpAttemptError = null;
          this.clearOtpTimer();
          this.clearResendCooldown();
          this.otpToastMessage = 'OTP verified. Completing account creation...';
          setTimeout(() => (this.otpToastMessage = null), 2000);
          this.submit();
        } else {
          this.otpAttemptsRemaining -= 1;
          if (this.otpAttemptsRemaining <= 0) {
            this.otpAttemptError = 'Maximum OTP attempts reached. Please resend OTP.';
          } else {
            this.otpAttemptError = `Invalid OTP. ${this.otpAttemptsRemaining} attempt(s) remain.`;
          }
        }
      },
      error: (err) => {
        this.otpAttemptError = err?.error?.message || 'Error verifying OTP. Please try again.';
      }
    });
  }

  resendOtp(): void {
    if (this.step !== 'OTP_SENT' || this.resendDisabled) {
      return;
    }

    const email = this.form.getRawValue().email;
    if (!email) {
      this.otpError = 'Email is required to resend OTP.';
      return;
    }

    this.otpAttemptError = null;
    this.otpError = null;
    this.otpSending = true;
    this.form.get('otp')?.reset();

    this.accountSetupService.sendOtp({ contact: email }).subscribe({
      next: () => {
        this.onOtpSent(email);
        this.otpSending = false;
      },
      error: (err) => {
        this.otpSending = false;
        this.otpError = err?.error?.message || 'Failed to resend OTP. Please try again.';
      }
    });
  }

  submit(): void {
    if (this.step !== 'VERIFIED') {
      return;
    }

    const { username, email, password } = this.form.getRawValue();

    this.createAccount(username, email, password);
  }

  private createAccount(username: string, email: string, password: string): void {
    this.loading = true;
    this.authService.signup({ name: username, email, password }).subscribe({
      next: () => {
        this.authService.login({ name: username, password, rememberMe: false }).subscribe({
          next: (loginRes) => {
            this.loading = false;
            if (loginRes.rememberToken) {
              localStorage.setItem('remember_token', loginRes.rememberToken);
            } else {
              localStorage.removeItem('remember_token');
            }
            if (loginRes.firstLogin) {
              this.router.navigate(['/account-setup']);
            } else {
              this.router.navigate(['/dashboard']);
            }
          },
          error: () => {
            this.loading = false;
            alert('Signup succeeded but automatic login failed. Please sign in.');
            this.router.navigate(['/']);
          }
        });
      },
      error: error => {
        this.loading = false;
        alert(error.error?.message || 'Signup failed. Please try again.');
      }
    });
  }

  private startOtpTimer(): void {
    this.clearOtpTimer();
    this.otpTimerId = setInterval(() => {
      if (this.otpExpirySeconds <= 0) {
        this.clearOtpTimer();
        this.otpAttemptError = 'OTP has expired. Please resend OTP.';
        return;
      }
      this.otpExpirySeconds -= 1;
      this.updateOtpCountdown();
    }, 1000);
  }

  private clearOtpTimer(): void {
    if (this.otpTimerId) {
      clearInterval(this.otpTimerId);
      this.otpTimerId = null;
    }
  }

  private startResendCooldown(): void {
    this.resendDisabled = true;
    this.resendSecondsRemaining = this.resendCooldownSeconds;

    if (this.resendTimerId) {
      clearTimeout(this.resendTimerId);
      this.resendTimerId = null;
    }
    if (this.resendIntervalId) {
      clearInterval(this.resendIntervalId);
      this.resendIntervalId = null;
    }

    this.resendIntervalId = setInterval(() => {
      if (this.resendSecondsRemaining <= 1) {
        this.resendSecondsRemaining = 0;
        this.resendDisabled = false;
        if (this.resendIntervalId) {
          clearInterval(this.resendIntervalId);
          this.resendIntervalId = null;
        }
        return;
      }
      this.resendSecondsRemaining -= 1;
    }, 1000);

    this.resendTimerId = setTimeout(() => {
      this.resendDisabled = false;
      this.resendTimerId = null;
    }, this.resendCooldownSeconds * 1000);
  }

  private clearResendCooldown(): void {
    if (this.resendTimerId) {
      clearTimeout(this.resendTimerId);
      this.resendTimerId = null;
    }
    if (this.resendIntervalId) {
      clearInterval(this.resendIntervalId);
      this.resendIntervalId = null;
    }
    this.resendDisabled = true;
    this.resendSecondsRemaining = this.resendCooldownSeconds;
  }

  private updateOtpCountdown(): void {
    const minutes = Math.floor(this.otpExpirySeconds / 60);
    const seconds = this.otpExpirySeconds % 60;
    this.otpCountdown = `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
  }

  ngOnDestroy(): void {
    this.clearOtpTimer();
    if (this.resendTimerId) {
      clearTimeout(this.resendTimerId);
      this.resendTimerId = null;
    }
    if (this.resendIntervalId) {
      clearInterval(this.resendIntervalId);
      this.resendIntervalId = null;
    }
  }
}

function passwordsMatch(control: AbstractControl): ValidationErrors | null {
  const password = control.get('password')?.value;
  const confirmPassword = control.get('confirmPassword')?.value;
  const confirmPasswordControl = control.get('confirmPassword');
  
  if (!password || !confirmPassword) {
    return null;
  }
  
  if (password !== confirmPassword) {
    confirmPasswordControl?.setErrors({ passwordMismatch: true });
    return { passwordMismatch: true };
  } else {
    if (confirmPasswordControl?.hasError('passwordMismatch')) {
      const currentErrors = confirmPasswordControl.errors;
      if (currentErrors) {
        delete currentErrors['passwordMismatch'];
        confirmPasswordControl.setErrors(Object.keys(currentErrors).length ? currentErrors : null);
      }
    }
  }
  return null;
}
