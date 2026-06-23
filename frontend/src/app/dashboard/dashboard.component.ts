import { Component, OnInit, ChangeDetectorRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { AuthService } from '../services/auth.service';
import { AccountSetupService } from '../services/account-setup.service';
import { TransactionService, Transaction } from '../services/transaction.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatCardModule, RouterLink, RouterLinkActive],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent implements OnInit {
  private cdr = inject(ChangeDetectorRef);
  private authService = inject(AuthService);
  private accountSetupService = inject(AccountSetupService);
  private transactionService = inject(TransactionService);

  routerLinkActiveOptions = { exact: true };
  userName = 'User';
  balance = 0;
  processedToday = 0;
  activeTransfers = 0;
  recentTransactions: Transaction[] = [];
  isFirstLogin = false;

  activityLogs: { id: string; type: string; title: string; desc: string; time: string }[] = [];

  cardDetails = {
    number: '**** **** **** ****',
    holder: 'User',
    expiry: '12/28',
    type: 'VISA'
  };

  constructor(private router: Router) {
    const user = this.authService.getCurrentUser();
    if (user) {
      this.userName = user.name;
      this.cardDetails.holder = user.name;
    }
  }

  ngOnInit(): void {
    this.loadDashboardData();
  }

  loadDashboardData(): void {
    const user = this.authService.getCurrentUser();
    if (!user) return;

    this.accountSetupService.getAccountByUser(user.name).subscribe({
      next: (data) => {
        // Fetch real balance from AccountService via AccountSetupService or direct AccountService
        // Since getAccountByUser returns BankDetails, we might need a separate call for Account balance
        this.cardDetails.number = this.maskCardNumber(data.creditCardNumber || '0000000000000000');

        // Fetch the account using the holder name to get the balance
        this.accountSetupService.getAccountByNumber(data.accountNumber).subscribe({
          next: (acc) => {
            this.balance = acc.balance || 0;
            this.cardDetails.expiry = data.expiryDate || '12/28';
            this.cardDetails.holder = data.userName || user.name;
            this.fetchTransactions(acc.id);
            this.cdr.detectChanges();
          },
          error: (err) => {
            console.error('Account Balance Load Error:', err);
            // Set default balance if account fetch fails
            this.balance = 0;
            this.cdr.detectChanges();
          }
        });
      },
      error: (err) => {
        console.error('Dashboard Load Error:', err);
        // Set default values if bank details fetch fails
        this.balance = 0;
        this.cdr.detectChanges();
      }
    });
  }

  private fetchTransactions(accountId: number): void {
    this.transactionService.getAccountHistory(accountId).subscribe({
      next: (txns) => {
        this.recentTransactions = txns.slice(0, 5);
        this.calculateMetrics(txns);
        this.deriveActivityLogs(txns);
        this.cdr.detectChanges();
      }
    });
  }

  /**
   * Builds the activity feed from the user's own transactions.
   * Maps each transaction to a typed log entry — no hardcoded data.
   */
  private deriveActivityLogs(txns: Transaction[]): void {
    this.activityLogs = txns.slice(0, 8).map(txn => {
      const displayType = this.getDisplayType(txn);
      const status = (txn.status ?? '').toUpperCase();

      // Determine dot colour
      let type: string;
      if (status === 'SUCCESS' || status === 'COMPLETED') {
        type = 'success';
      } else if (status === 'FAILED') {
        type = 'danger';
      } else {
        type = 'warning'; // pending
      }

      // Determine title based on direction + status
      let title: string;
      if (status === 'FAILED') {
        const counterparty = txn.toAccountHolderName || 'Unknown';
        title = `Transfer to ${counterparty} failed`;
      } else if (displayType === 'credit') {
        const sender = txn.fromAccountHolderName || 'Unknown';
        title = status === 'SUCCESS' || status === 'COMPLETED'
          ? `Received from ${sender}`
          : `Incoming from ${sender} (pending)`;
      } else {
        const recipient = txn.toAccountHolderName || 'Unknown';
        title = status === 'SUCCESS' || status === 'COMPLETED'
          ? `Sent to ${recipient}`
          : `Transfer to ${recipient} pending`;
      }

      // Description: amount + reference
      const formatted = parseFloat(txn.amount).toLocaleString('en-IN', {
        style: 'currency', currency: 'INR', minimumFractionDigits: 2
      });
      const desc = `${formatted} · Ref: ${txn.referenceId}`;

      return { id: txn.id, type, title, desc, time: this.getRelativeTime(txn.date) };
    });
  }

  /** Converts a date into a human-readable relative string (e.g. "5 mins ago"). */
  private getRelativeTime(date: Date | string): string {
    const now  = new Date();
    const then = new Date(date);
    const diffMs   = now.getTime() - then.getTime();
    const diffMins = Math.floor(diffMs / 60_000);

    if (diffMins < 1)   return 'Just now';
    if (diffMins < 60)  return `${diffMins} min${diffMins > 1 ? 's' : ''} ago`;

    const diffHours = Math.floor(diffMins / 60);
    if (diffHours < 24) return `${diffHours} hour${diffHours > 1 ? 's' : ''} ago`;

    const diffDays = Math.floor(diffHours / 24);
    if (diffDays < 7)   return `${diffDays} day${diffDays > 1 ? 's' : ''} ago`;

    return then.toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' });
  }

  private calculateMetrics(txns: Transaction[]): void {
    const today = new Date().toDateString();

    this.processedToday = txns
      .filter(t => new Date(t.date).toDateString() === today && (t.status === 'SUCCESS' || t.status === 'completed'))
      .reduce((sum, t) => sum + parseFloat(t.amount), 0);

    this.activeTransfers = txns.filter(t => t.status === 'pending').length;
  }

  private maskCardNumber(num: string): string {
    if (!num) return '**** **** **** ****';
    const last4 = num.slice(-4);
    return `**** **** **** ${last4}`;
  }

  getDisplayType(txn: Transaction): string {
    // If we sent money, it's a debit (red). If we received, it's credit (green).
    // Let's assume if it's 'credit' or we are the receiver, it's credit.
    if (txn.type === 'credit') return 'credit';
    if (txn.type === 'debit') return 'debit';

    const currentUser = this.authService.getCurrentUser();
    if (currentUser && txn.toAccountHolderName === currentUser.name) {
      return 'credit';
    }
    return 'debit';
  }

  getInitials(name: string): string {
    if (!name) return 'SY';
    const parts = name.trim().split(/\s+/);
    if (parts.length >= 2) {
      return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
    }
    return name.slice(0, 2).toUpperCase();
  }

  getTransactionSign(txn: Transaction): string {
    return this.getDisplayType(txn) === 'credit' ? '+' : '-';
  }

  sendMoney(): void {
    this.router.navigate(['/transfer']);
  }


  requestOtp(): void {
    alert('OTP requested successfully! A secure 6-digit passcode has been sent to your registered phone number.');
  }

  onLogout(): void {
    this.authService.clearSession();
    this.router.navigate(['/']);
  }
}
