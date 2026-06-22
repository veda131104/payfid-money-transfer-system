import { Component, OnInit, ChangeDetectorRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators, FormsModule, FormControl } from '@angular/forms';
import { AuthService } from '../services/auth.service';
import { AccountSetupService } from '../services/account-setup.service';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    RouterLink,
    RouterLinkActive,
    ReactiveFormsModule,
    FormsModule
  ],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.scss',
})
export class ProfileComponent implements OnInit {
  private cdr = inject(ChangeDetectorRef);
  routerLinkActiveOptions = { exact: true };
  isNewUser = true; // Initialize to true for safety
  isEditing = false;
  isPinModalOpen = false;
  pinForm: FormGroup;
  pinError = '';
  profileForm!: FormGroup;
  currentBalance = 0;

  profileData = {
    name: '',
    accountNumber: 'N/A',
    email: 'N/A',
    phoneNumber: 'N/A',
    address: 'N/A',
    bankName: 'N/A',
    branchName: 'N/A',
    ifscCode: 'N/A',
    upiId: 'N/A',
    accountType: 'Savings Account',
    accountStatus: 'Active',
    joinDate: 'N/A',
    creditCardNumber: 'N/A',
    cvv: 'N/A',
    expiryDate: 'N/A',
    hasPin: false
  };

  constructor(
    private router: Router,
    private authService: AuthService,
    private accountSetupService: AccountSetupService,
    private fb: FormBuilder
  ) {
    this.pinForm = this.fb.group({
      pin: ['', [Validators.required, Validators.pattern(/^\d{4}$/)]]
    });
  }

  ngOnInit(): void {
    this.profileForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      phoneNumber: ['', [Validators.required]],
      bankName: ['', [Validators.required]],
      branchName: ['', [Validators.required]],
      address: ['', [Validators.required]],
      ifscCode: ['', [Validators.required]],
      creditCardNumber: ['', [Validators.required]],
      cvv: ['', [Validators.required]],
      expiryDate: ['', [Validators.required]]
    });

    const user = this.authService.getCurrentUser();
    if (user) {
      this.profileData.name = user.name;
      this.checkAccountStatus(user.name);
    } else {
      this.router.navigate(['/']);
    }
  }

  checkAccountStatus(userName: string): void {
    this.accountSetupService.getAccountByUser(userName).subscribe({
      next: (data) => {
        this.isNewUser = false;
        this.profileData = {
          ...this.profileData,
          ...data,
          hasPin: !!data.pin,
          joinDate: data.createdAt ? new Date(data.createdAt).toLocaleDateString('en-US', {
            year: 'numeric',
            month: 'long',
            day: 'numeric'
          }) : 'N/A'
        };
        
        // Load balance from account
        if (data.accountNumber) {
          this.accountSetupService.getAccountByNumber(data.accountNumber).subscribe({
            next: (account) => {
              this.currentBalance = account.balance || 0;
              this.cdr.detectChanges();
            },
            error: (err) => {
              console.error('Error loading account balance:', err);
              this.currentBalance = 0;
            }
          });
        }
        
        this.profileForm.patchValue(data);
        this.cdr.detectChanges();
      },
      error: () => {
        this.isNewUser = true;
        this.profileData.accountStatus = 'Pending Setup';
      }
    });
  }

  onLogout(): void {
    this.authService.clearSession();
    this.router.navigate(['/']);
  }

  onEditProfile(): void {
    if (this.isNewUser) {
      this.router.navigate(['/account-setup']);
    } else {
      this.isEditing = true;
    }
  }

  onSave(): void {
    if (this.profileForm.invalid) return;

    const user = this.authService.getCurrentUser();
    if (!user) return;

    this.accountSetupService.updateAccount(user.name, this.profileForm.value).subscribe({
      next: (data) => {
        this.profileData = {
          ...this.profileData,
          ...data,
          hasPin: data.pin !== undefined ? !!data.pin : this.profileData.hasPin,
          joinDate: data.createdAt ? new Date(data.createdAt).toLocaleDateString('en-US', {
            year: 'numeric',
            month: 'long',
            day: 'numeric'
          }) : 'N/A'
        };
        this.isEditing = false;
        alert('Profile updated successfully!');
      },
      error: (e) => alert('Update failed: ' + e?.message)
    });
  }

  onCancel(): void {
    this.isEditing = false;
    this.profileForm.patchValue(this.profileData);
  }

  getInitials(): string {
    if (!this.profileData.name) return 'U';
    const names = this.profileData.name.split(' ');
    if (names.length >= 2) {
      return (names[0].charAt(0) + names[names.length - 1].charAt(0)).toUpperCase();
    }
    return this.profileData.name.charAt(0).toUpperCase();
  }

  onOpenPinModal(): void {
    console.log('🔓 [Profile] Opening PIN Modal');
    this.isPinModalOpen = true;
    this.pinForm.reset({ pin: '' });
    this.pinError = '';
    this.cdr.detectChanges();
  }

  onClosePinModal(): void {
    console.log('🔒 [Profile] Closing PIN Modal');
    this.isPinModalOpen = false;
    this.cdr.detectChanges();
  }

  onSavePin(): void {
    const pin = this.pinForm.get('pin')?.value;
    console.log('🔘 [Profile] Save PIN clicked. Status:', this.pinForm.status, 'Value:', pin);

    if (this.pinForm.invalid) {
      const control = this.pinForm.get('pin');
      console.warn('⚠️ [Profile] PIN Form is INVALID. Errors:', control?.errors);

      if (control?.hasError('required')) {
        this.pinError = 'PIN is required';
      } else if (control?.hasError('pattern')) {
        this.pinError = 'PIN must be exactly 4 digits';
      } else {
        this.pinError = 'Invalid PIN format';
      }
      alert('Validation Error: ' + this.pinError);
      this.cdr.detectChanges();
      return;
    }

    const user = this.authService.getCurrentUser();
    if (!user) {
      alert('User session not found. Please log in again.');
      return;
    }

    console.log('📡 [Profile] Sending PIN update for user:', user.name);
    this.accountSetupService.setPin(user.name, pin).subscribe({
      next: (resp) => {
        console.log('✅ [Profile] PIN update response:', resp);
        this.profileData.hasPin = true;
        alert('Success! Your PIN has been set.');
        this.onClosePinModal();
      },
      error: (e) => {
        console.error('❌ [Profile] Set PIN error:', e);
        const errorMessage = e?.error?.message || e?.message || 'Unknown error';
        if (errorMessage.includes('Bank details not found')) {
          this.pinError = 'Bank details not found. Please complete Account Setup first.';
          alert('Error: Bank details not found. Please complete Account Setup first.');
        } else {
          this.pinError = 'Failed to set PIN: ' + errorMessage;
          alert('Error: ' + this.pinError);
        }
        this.cdr.detectChanges();
      }
    });
  }
}
