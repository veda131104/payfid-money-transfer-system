import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, FormsModule, Validators, AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { AccountSetupService } from '../services/account-setup.service';
import { AuthService } from '../services/auth.service';
import { PopupService } from '../services/popup.service';

export function futureDateValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    if (!control.value) return null;
    const parts = control.value.split('/');
    if (parts.length !== 2) return null;
    const month = parseInt(parts[0], 10);
    const year = parseInt(parts[1], 10);
    if (isNaN(month) || isNaN(year)) return null;

    const now = new Date();
    const currentMonth = now.getMonth() + 1; // 1-12
    const currentYear = parseInt(now.getFullYear().toString().slice(-2), 10);

    if (year < currentYear || (year === currentYear && month < currentMonth)) {
      return { pastDate: true };
    }
    return null;
  };
}

@Component({
  selector: 'app-account-setup',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    ReactiveFormsModule,
    FormsModule
  ],
  templateUrl: './account-setup.component.html',
  styleUrls: ['./account-setup.component.scss']
})
export class AccountSetupComponent implements OnInit {
  form!: FormGroup;
  lastSentOtp = '';
  isEmailVerified = true;
  otpSentMessage = '';
  setupComplete = false;
  showPinSetup = false;
  showEditDetails = false;
  accountData: any = null;
  currentBalance: number = 0;
  pinForm!: FormGroup;
  editDetailsForm!: FormGroup;
  private readonly popupService = inject(PopupService);

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: AccountSetupService,
    private readonly authService: AuthService,
    private readonly router: Router
  ) {
    this.form = this.fb.nonNullable.group({
      accountNumber: ['', [Validators.required, Validators.minLength(9), Validators.maxLength(18), Validators.pattern(/^\d+$/)]],
      bankName: ['', [Validators.required]],
      branchName: ['', [Validators.required]],
      address: ['', [Validators.required]],
      ifscCode: ['', [Validators.required]],
      email: ['', [Validators.required, Validators.email]],
      phoneNumber: ['', [Validators.required, Validators.pattern(/^\d{10}$/)]],
      creditCardNumber: ['', [Validators.required, Validators.pattern(/^\d{16}$/)]],
      cvv: ['', [Validators.required, Validators.pattern(/^\d{3}$/)]],
      expiryDate: ['', [Validators.required, Validators.pattern(/^(0?[1-9]|1[0-2])\/\d{2}$/), futureDateValidator()]]
    });

    this.pinForm = this.fb.nonNullable.group({
      pin: ['', [Validators.required, Validators.minLength(4), Validators.maxLength(6)]],
      confirmPin: ['', [Validators.required, Validators.minLength(4), Validators.maxLength(6)]]
    });

    this.editDetailsForm = this.fb.nonNullable.group({
      email: ['', [Validators.required, Validators.email]],
      phoneNumber: ['', [Validators.required, Validators.pattern(/^\d{10}$/)]],
      address: ['', [Validators.required]],
      creditCardNumber: ['', [Validators.required, Validators.pattern(/^\d{16}$/)]],
      cvv: ['', [Validators.required, Validators.pattern(/^\d{3}$/)]],
      expiryDate: ['', [Validators.required, Validators.pattern(/^(0?[1-9]|1[0-2])\/\d{2}$/), futureDateValidator()]]
    });

  }

  ngOnInit(): void {
    const user = this.authService.getCurrentUser();
    if (user?.name) {
      if (user.email) {
        this.form.patchValue({ email: user.email });
      }
      this.svc.getAccountByUser(user.name).subscribe({
        next: (res) => {
          this.popupService.alert('Your account is already set up! Redirecting to dashboard.', 'Account Already Set Up');
          this.router.navigate(['/dashboard']);
        },
        error: (err) => {
          // Stay on page
        }
      });
    }
  }

  /** Strip non-digit characters from phone number input */
  onPhoneInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    input.value = input.value.replace(/[^0-9]/g, '').slice(0, 10);
    this.form.get('phoneNumber')?.setValue(input.value, { emitEvent: false });
    this.form.get('phoneNumber')?.markAsTouched();
  }

  /** Strip non-digit characters from phone number input in edit form */
  onEditPhoneInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    input.value = input.value.replace(/[^0-9]/g, '').slice(0, 10);
    this.editDetailsForm.get('phoneNumber')?.setValue(input.value, { emitEvent: false });
    this.editDetailsForm.get('phoneNumber')?.markAsTouched();
  }

  /** Strip non-digit characters from credit card number input */
  onCreditCardInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    input.value = input.value.replace(/[^0-9]/g, '').slice(0, 16);
    this.form.get('creditCardNumber')?.setValue(input.value, { emitEvent: false });
    this.form.get('creditCardNumber')?.markAsTouched();
  }

  /** Strip non-digit characters from credit card number in edit form */
  onEditCreditCardInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    input.value = input.value.replace(/[^0-9]/g, '').slice(0, 16);
    this.editDetailsForm.get('creditCardNumber')?.setValue(input.value, { emitEvent: false });
    this.editDetailsForm.get('creditCardNumber')?.markAsTouched();
  }

  /** Strip non-digit characters from CVV input */
  onCvvInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    input.value = input.value.replace(/[^0-9]/g, '').slice(0, 3);
    this.form.get('cvv')?.setValue(input.value, { emitEvent: false });
    this.form.get('cvv')?.markAsTouched();
  }

  /** Strip non-digit characters from CVV input in edit form */
  onEditCvvInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    input.value = input.value.replace(/[^0-9]/g, '').slice(0, 3);
    this.editDetailsForm.get('cvv')?.setValue(input.value, { emitEvent: false });
    this.editDetailsForm.get('cvv')?.markAsTouched();
  }

  /**
   * Smart expiry date formatter: auto-inserts '/', validates month 1-12.
   * Supports MM/YY format. Blocks invalid month numbers (e.g. 00, 13+).
   */
  onExpiryInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    const rawValue = input.value;
    this._formatExpiryInput(input, rawValue, 'form');
  }

  onEditExpiryInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    const rawValue = input.value;
    this._formatExpiryInput(input, rawValue, 'editDetailsForm');
  }

  private _formatExpiryInput(input: HTMLInputElement, rawValue: string, formName: 'form' | 'editDetailsForm'): void {
    // Allow only digits and slash
    let digits = rawValue.replace(/[^0-9]/g, '');

    // Validate first digit: only 0 or 1 allowed as the first digit of month
    if (digits.length >= 1) {
      const firstDigit = parseInt(digits[0], 10);
      if (firstDigit > 1) {
        // Prefix with '0' to make it a valid month (e.g. '5' → '05')
        digits = '0' + digits[0] + digits.slice(1);
      }
    }

    // Validate full month (digits[0..1]) is between 01 and 12
    if (digits.length >= 2) {
      const month = parseInt(digits.slice(0, 2), 10);
      if (month < 1 || month > 12) {
        // Truncate to first digit and let user re-enter
        digits = digits[0];
      }
    }

    // Build formatted value
    let formatted = '';
    if (digits.length <= 2) {
      formatted = digits;
    } else {
      formatted = digits.slice(0, 2) + '/' + digits.slice(2, 4);
    }

    input.value = formatted;
    const ctrl = this[formName].get('expiryDate');
    ctrl?.setValue(formatted, { emitEvent: false });
    ctrl?.markAsTouched();
  }


  submit(): void {
    if (this.form.invalid) {
      const accNoControl = this.form.get('accountNumber');
      if (accNoControl && accNoControl.invalid && (accNoControl.hasError('pattern') || accNoControl.hasError('minlength') || accNoControl.hasError('maxlength'))) {
        this.popupService.alert('Validation Error: Account Number must be between 9 and 18 digits (numbers only).', 'Validation Error');
        accNoControl.markAsTouched();
        return;
      }
      this.popupService.alert('Please fill in all mandatory fields correctly before proceeding.', 'Validation Error');
      this.form.markAllAsTouched();
      return;
    }

    const accNo = this.form.get('accountNumber')?.value || '';
    if (!/^\d{9,18}$/.test(accNo)) {
      this.popupService.alert('Validation Error: Account Number must be between 9 and 18 digits (numbers only).', 'Validation Error');
      this.form.get('accountNumber')?.markAsTouched();
      return;
    }

    const user = this.authService.getCurrentUser();
    if (!user?.name) {
      this.popupService.alert('You are not logged in. Please log in again and retry.', 'Session Expired');
      this.router.navigate(['/login']);
      return;
    }

    const payload = {
      ...this.form.getRawValue(),
      userName: user.name
    };

    this.svc.create(payload).subscribe({
      next: (response) => {
        this.popupService.alert('Account setup completed successfully!', 'Success');
        this.router.navigate(['/dashboard']);
      },
      error: (e) => {
        const msg = e?.error?.message || e?.message || 'An unexpected error occurred during account setup.';
        this.popupService.alert('Error saving: ' + msg, 'Error');
      }
    });
  }

  loadAccountBalance(): void {
    if (this.accountData?.accountNumber) {
      this.svc.getAccountByNumber(this.accountData.accountNumber).subscribe({
        next: (account) => {
          this.currentBalance = account.balance || 0;
        },
        error: (err) => console.error('Error loading balance:', err)
      });
    }
  }

  setPin(): void {
    if (this.pinForm.invalid) {
      this.popupService.alert('Please enter a valid PIN (4-6 digits)', 'Validation Error');
      return;
    }

    const pin = this.pinForm.get('pin')?.value;
    const confirmPin = this.pinForm.get('confirmPin')?.value;

    if (pin !== confirmPin) {
      this.popupService.alert('PINs do not match', 'Validation Error');
      return;
    }

    const user = this.authService.getCurrentUser();
    const userName = user?.name || '';
    if (!userName) {
      this.popupService.alert('User not found. Please log in again.', 'Error');
      return;
    }
    this.svc.setPin(userName, pin).subscribe({
      next: () => {
        this.popupService.alert('PIN set successfully!', 'Success');
        this.showPinSetup = false;
        this.pinForm.reset();
      },
      error: (err) => this.popupService.alert('Error setting PIN: ' + err?.message, 'Error')
    });
  }

  openEditDetails(): void {
    if (this.accountData) {
      this.editDetailsForm.patchValue({
        email: this.accountData.email,
        phoneNumber: this.accountData.phoneNumber,
        address: this.accountData.address,
        creditCardNumber: this.accountData.creditCardNumber,
        cvv: this.accountData.cvv,
        expiryDate: this.accountData.expiryDate
      });
      this.showEditDetails = true;
    }
  }

  saveEditDetails(): void {
    if (this.editDetailsForm.invalid) {
      this.popupService.alert('Please fill in all fields correctly', 'Validation Error');
      return;
    }

    const user = this.authService.getCurrentUser();
    const userName = user?.name || '';
    if (!userName) {
      this.popupService.alert('User not found. Please log in again.', 'Error');
      return;
    }
    const updatePayload = {
      ...this.accountData,
      ...this.editDetailsForm.getRawValue()
    };

    this.svc.updateAccount(userName, updatePayload).subscribe({
      next: (updated) => {
        this.accountData = updated;
        this.showEditDetails = false;
        this.popupService.alert('Details updated successfully!', 'Success');
      },
      error: (err) => this.popupService.alert('Error updating details: ' + err?.message, 'Error')
    });
  }

  cancelEditDetails(): void {
    this.showEditDetails = false;
  }

  completeSetup(): void {
    this.router.navigate(['/dashboard']);
  }
}
