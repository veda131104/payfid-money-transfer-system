import { Component, NgZone } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { Router } from '@angular/router';
import { TransactionService, Transaction } from '../services/transaction.service';

@Component({
  selector: 'app-pin-confirm',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatCardModule],
  templateUrl: './pin-confirm.component.html',
  styleUrls: ['./pin-confirm.component.scss'],
})
export class PinConfirmComponent {
  accountNumber: string = '';
  amount: string = '';
  pin: string = '';
  message: string = '';
  isLoading: boolean = false;
  isSuccess: boolean = false;
  isPinFocused: boolean = false;
  pinVisible: boolean = false;

  private readonly CORRECT_PIN = '1234';

  constructor(private router: Router, private ngZone: NgZone, private transactionService: TransactionService) {
    // Grab navigation state (transfer details) from history.state
    const st: any = (typeof history !== 'undefined' ? history.state : null) || {};
    this.accountNumber = st.accountNumber || '';
    this.amount = st.amount || '';
  }

  onPinInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    input.value = input.value.replace(/[^0-9]/g, '').slice(0, 4);
    this.pin = input.value;
  }

  onConfirm(): void {
    if (!this.pin || this.pin.length < 4) {
      this.message = 'Please enter a 4-digit PIN.';
      return;
    }

    if (this.pin !== this.CORRECT_PIN) {
      // Failed transaction
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
      this.message = 'Incorrect PIN. Transaction failed.';
      return;
    }

    // Correct PIN: simulate processing
    this.isLoading = true;
    this.message = '';
    this.ngZone.runOutsideAngular(() => {
      setTimeout(() => {
        this.ngZone.run(() => {
          this.isLoading = false;
          this.isSuccess = true;

          const successTxn: Transaction = {
            id: Date.now().toString(),
            accountNumber: this.accountNumber,
            amount: this.amount,
            date: new Date(),
            type: 'debit',
            status: 'completed',
            referenceId: this.transactionService.generateReferenceId(),
            description: 'Transfer completed'
          };
          this.transactionService.addTransaction(successTxn);
        });
      }, 2000);
    });
  }

  onBack(): void {
    this.router.navigate(['/transfer']);
  }

  onGoHistory(): void {
    this.router.navigate(['/history']);
  }
}
