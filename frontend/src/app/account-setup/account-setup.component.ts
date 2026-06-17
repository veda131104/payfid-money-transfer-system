import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, FormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { AccountSetupService } from '../services/account-setup.service';
import { AuthService } from '../services/auth.service';

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
export class AccountSetupComponent {
  form!: FormGroup;
  lastSentOtp = '';
  isEmailVerified = false;
  otpSentMessage = '';
  setupComplete = false;
  showPinSetup = false;
  showEditDetails = false;
  accountData: any = null;
  pinForm!: FormGroup;
  currentBalance = 0;
  editDetailsForm!: FormGroup;

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: AccountSetupService,
    private readonly authService: AuthService,
    private readonly router: Router
  ) {
    this.form = this.fb.nonNullable.group({
      accountNumber: ['', [Validators.required]],
      bankName: ['', [Validators.required]],
      branchName: ['', [Validators.required]],
      address: ['', [Validators.required]],
      ifscCode: ['', [Validators.required]],
      email: ['', [Validators.required, Validators.email]],
      phoneNumber: ['', [Validators.required]],
      creditCardNumber: ['', [Validators.required]],
      cvv: ['', [Validators.required]],
      expiryDate: ['', [Validators.required]],
      otp: ['']
    });

    this.pinForm = this.fb.nonNullable.group({
      pin: ['', [Validators.required, Validators.minLength(4), Validators.maxLength(6)]],
      confirmPin: ['', [Validators.required, Validators.minLength(4), Validators.maxLength(6)]]
    });

    this.editDetailsForm = this.fb.nonNullable.group({
      email: ['', [Validators.required, Validators.email]],
      phoneNumber: ['', [Validators.required]],
      address: ['', [Validators.required]],
      creditCardNumber: ['', [Validators.required]],
      cvv: ['', [Validators.required]],
      expiryDate: ['', [Validators.required]]
    });

    // Reset verification status if email changes
    this.form.get('email')?.valueChanges.subscribe(() => {
      this.isEmailVerified = false;
      this.otpSentMessage = '';
    });
  }

  sendOtp(): void {
    const contact = this.form.get('email')?.value || '';
    if (!contact) {
      alert('Please enter an email address first');
      return;
    }
    this.svc.sendOtp({ contact }).subscribe({
      next: (resp) => {
        this.otpSentMessage = `OTP has been sent to ${contact}. Valid for 10 minutes.`;
      },
      error: (err) => {
        const errMsg = err?.error?.message || err?.message || 'Unknown error';
        alert('Failed to send OTP. Error: ' + errMsg);
        console.error('OTP Send Error:', err);
      }
    });
  }

  verifyOtp(): void {
    const contact = this.form.get('email')?.value || '';
    const otp = this.form.get('otp')?.value || '';
    if (!contact || !otp) {
      alert('Please enter email and OTP');
      return;
    }
    this.svc.verifyOtp({ contact, otp }).subscribe(resp => {
      if (resp.verified) {
        this.isEmailVerified = true;
        alert('Email verified successfully!');
      } else {
        this.isEmailVerified = false;
        alert('Invalid OTP. Please try again.');
      }
    });
  }

  submit(): void {
    if (this.form.invalid) {
      alert('Please fill in all mandatory fields correctly before proceeding.');
      this.form.markAllAsTouched();
      return;
    }

    const accNo = this.form.get('accountNumber')?.value || '';
    if (accNo.length !== 12) {
      alert('Validation Error: Account Number must be exactly 12 digits.');
      this.form.get('accountNumber')?.markAsTouched();
      return;
    }

    if (!this.isEmailVerified) {
      alert('Security Requirement: Please verify your email address using the OTP sent to your inbox before completing setup.');
      this.form.get('otp')?.markAsTouched();
      return;
    }

    const user = this.authService.getCurrentUser();
    const payload = {
      ...this.form.getRawValue(),
      userName: user?.name
    };

    this.svc.create(payload).subscribe({
      next: (response) => {
        this.accountData = response;
        this.setupComplete = true;
        this.loadAccountBalance();
        // Refresh form for display
        this.editDetailsForm.patchValue({
          email: payload.email,
          phoneNumber: payload.phoneNumber,
          address: payload.address,
          creditCardNumber: payload.creditCardNumber,
          cvv: payload.cvv,
          expiryDate: payload.expiryDate
        });
      },
      error: (e) => alert('Error saving: ' + e?.message)
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
      alert('Please enter a valid PIN (4-6 digits)');
      return;
    }

    const pin = this.pinForm.get('pin')?.value;
    const confirmPin = this.pinForm.get('confirmPin')?.value;

    if (pin !== confirmPin) {
      alert('PINs do not match');
      return;
    }

    const user = this.authService.getCurrentUser();
    const userName = user?.name || '';
    if (!userName) {
      alert('User not found. Please log in again.');
      return;
    }
    this.svc.setPin(userName, pin).subscribe({
      next: () => {
        alert('PIN set successfully!');
        this.showPinSetup = false;
        this.pinForm.reset();
      },
      error: (err) => alert('Error setting PIN: ' + err?.message)
    });
  }

  openEditDetails(): void {
    // Auto-fill edit form with current data
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
      alert('Please fill in all fields correctly');
      return;
    }

    const user = this.authService.getCurrentUser();
    const userName = user?.name || '';
    if (!userName) {
      alert('User not found. Please log in again.');
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
        alert('Details updated successfully!');
      },
      error: (err) => alert('Error updating details: ' + err?.message)
    });
  }

  cancelEditDetails(): void {
    this.showEditDetails = false;
  }

  completeSetup(): void {
    this.router.navigate(['/dashboard']);
  }
}
