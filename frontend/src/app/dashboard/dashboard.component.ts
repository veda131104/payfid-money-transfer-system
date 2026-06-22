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
        this.cdr.detectChanges();
      }
    });
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

  getTransactionSign(txn: Transaction): string {
    return this.getDisplayType(txn) === 'credit' ? '+' : '-';
  }

  onLogout(): void {
    this.authService.clearSession();
    this.router.navigate(['/']);
  }
}
