import { Component, ChangeDetectorRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { TransactionService, Transaction } from '../services/transaction.service';
import { AuthService } from '../services/auth.service';
import { AccountSetupService } from '../services/account-setup.service';
import { PopupService } from '../services/popup.service';

@Component({
  selector: 'app-transfer',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatCardModule, RouterLink, RouterLinkActive],
  templateUrl: './transfer.component.html',
  styleUrl: './transfer.component.scss',
})
export class TransferComponent {
  private cdr = inject(ChangeDetectorRef);
  private popupService = inject(PopupService);
  accountNumber: string = '';
  amount: string = '';
  description: string = '';
  isLoading: boolean = false;
  isSuccess: boolean = false;
  successDetails: { accountNumber: string; amount: string } = {
    accountNumber: '',
    amount: '',
  };
  // PIN modal state
  showPinModal: boolean = false;
  pin: string = '';
  pinMessage: string = '';
  pinLoading: boolean = false;
  pinVisible: boolean = false;
  pinErrorTimer: any = null;
  userPin: string = '';
  routerLinkActiveOptions = { exact: true };

  // Personalized data
  userName: string = 'User';
  myAccountNumber: string = '000000000000';
  myUpiId: string = '';

  constructor(
    private router: Router,
    private transactionService: TransactionService,
    private authService: AuthService,
    private accountSetupService: AccountSetupService
  ) { }

  ngOnInit(): void {
    const user = this.authService.getCurrentUser();
    if (user) {
      this.userName = user.name;
      this.accountSetupService.getAccountByUser(user.name).subscribe({
        next: (details: any) => {
          if (details) {
            this.myAccountNumber = details.accountNumber;
            this.myUpiId = details.upiId;
            this.userPin = details.pin || ''; // Remove fallback
            this.cdr.detectChanges();
          }
        },
        error: (err: any) => console.error('Error fetching bank details:', err)
      });
    }
  }

  getInitials(): string {
    if (!this.userName) return 'U';
    const names = this.userName.split(' ');
    if (names.length >= 2) {
      return (names[0].charAt(0) + names[names.length - 1].charAt(0)).toUpperCase();
    }
    return this.userName.charAt(0).toUpperCase();
  }

  onAccountNumberInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    const value = input.value.replace(/[^0-9a-zA-Z@.]/g, '').slice(0, 50);
    input.value = value;
    this.accountNumber = value;
  }

  onAmountInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    const value = input.value.replace(/[^0-9.]/g, '');

    // Ensure only one decimal point
    const parts = value.split('.');
    if (parts.length > 2) {
      input.value = parts[0] + '.' + parts.slice(1).join('');
    } else {
      input.value = value;
    }
    this.amount = input.value;
  }

  onDescriptionInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.description = input.value;
  }

  onSendMoney(): void {
    // Validation
    if (!this.accountNumber.trim()) {
      this.popupService.alert('Please enter the recipient\'s account number or UPI ID.', 'Validation Error');
      return;
    }

    if (!this.accountNumber.includes('@')) {
      if (!/^\d+$/.test(this.accountNumber)) {
        this.popupService.alert('Account number must contain only numbers.', 'Validation Error');
        return;
      }
      if (this.accountNumber.length < 9 || this.accountNumber.length > 18) {
        this.popupService.alert('Account number must be between 9 and 18 digits.', 'Validation Error');
        return;
      }
    }

    if (this.accountNumber === this.myAccountNumber || (this.myUpiId && this.accountNumber === this.myUpiId)) {
      this.popupService.alert('self transfer is not allowed', 'Validation Error');
      return;
    }

    if (!this.amount.trim()) {
      this.popupService.alert('Please enter the amount to transfer.', 'Validation Error');
      return;
    }

    const amountValue = parseFloat(this.amount);
    if (amountValue <= 0) {
      this.popupService.alert('Amount must be greater than zero.', 'Validation Error');
      return;
    }

    if (!this.userPin) {
      this.popupService.alert('Transaction Security: No PIN detected. Please go to your Profile and set a 4-digit PIN before making transfers.', 'Security Warning');
      return;
    }

    // Open PIN modal (keep transfer details ready)
    this.successDetails = {
      accountNumber: this.accountNumber,
      amount: this.amount,
    };
    this.pin = '';
    this.pinMessage = '';
    this.showPinModal = true;
  }

  onPinInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    input.value = input.value.replace(/[^0-9]/g, '').slice(0, 4);
    this.pin = input.value;
  }

  togglePinVisibility(): void {
    this.pinVisible = !this.pinVisible;
  }

  onConfirmPin(): void {
    if (!this.pin || this.pin.length < 4) {
      this.pinMessage = 'Please enter a 4-digit PIN.';
      return;
    }

    const CORRECT_PIN = this.userPin;
    if (this.pin !== CORRECT_PIN) {
      // Add failed transaction to history
      const failedTxn: Transaction = {
        id: Date.now().toString(),
        accountNumber: this.accountNumber,
        amount: this.amount,
        date: new Date(),
        type: 'debit',
        status: 'failed',
        referenceId: this.transactionService.generateReferenceId(),
        description: 'Incorrect PIN'
      };
      this.transactionService.addTransaction(failedTxn);
      this.pinMessage = 'Incorrect PIN. Transaction failed.';
      this.pin = '';
      this.pinVisible = false;
      // Auto-close modal after 5 seconds
      if (this.pinErrorTimer) clearTimeout(this.pinErrorTimer);
      this.pinErrorTimer = setTimeout(() => {
        this.closePinModal();
      }, 5000);
      return;
    }

    if (this.myAccountNumber === '000000000000') {
      this.pinMessage = 'Loading your account details... Please try again in a moment.';
      return;
    }

    // Correct PIN: call backend
    this.pinLoading = true;
    this.pinMessage = '';

    const payload = {
      fromAccountNumber: this.myAccountNumber,
      toAccountNumber: this.accountNumber,
      amount: parseFloat(this.amount)
    };

    this.transactionService.executeTransfer(payload).subscribe({
      next: (response: any) => {
        this.pinLoading = false;
        this.showPinModal = false;
        this.isLoading = false;
        this.isSuccess = true;

        const newTransaction: Transaction = {
          id: response.transactionId?.toString() || Date.now().toString(),
          accountNumber: this.accountNumber,
          fromAccountNumber: this.myAccountNumber,  // Sender's account
          toAccountNumber: this.accountNumber,       // Receiver's account
          amount: this.amount,
          date: new Date(),
          type: 'transfer',
          status: response.status || 'SUCCESS',
          referenceId: this.transactionService.generateReferenceId(),
          description: this.description || 'Transfer completed'
        };
        this.transactionService.addTransaction(newTransaction);
        this.cdr.detectChanges();
      },
      error: (err: any) => {
        this.pinLoading = false;
        console.error('Full Transfer Error Objekt:', err);
        const backendMessage = err.error?.message;
        const statusText = err.statusText;
        const status = err.status;

        this.pinMessage = backendMessage || (statusText ? `${statusText} (${status})` : 'Transaction failed. Please try again.');
        this.cdr.detectChanges();
      }
    });
  }

  closePinModal(): void {
    if (this.pinErrorTimer) clearTimeout(this.pinErrorTimer);
    this.showPinModal = false;
    this.pin = '';
    this.pinMessage = '';
  }

  onNewTransfer(): void {
    // Reset states and form
    this.isSuccess = false;
    this.isLoading = false;
    this.accountNumber = '';
    this.amount = '';
    this.description = '';
    const inputs = document.querySelectorAll('.transfer input');
    inputs.forEach(input => (input as HTMLInputElement).value = '');
  }

  onCheckHistory(): void {
    // Navigate to history page
    this.router.navigate(['/history']);
  }

  onLogout(): void {
    this.authService.clearSession();
    this.router.navigate(['/']);
  }
}
