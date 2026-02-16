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
  currentUserAccountNumber: string = '';
  isFirstLogin: boolean = false;

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
      this.isFirstLogin = !!user.firstLogin;
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
            this.balance = acc.balance;
            this.currentUserAccountNumber = acc.accountNumber;
            this.cardDetails.expiry = data.expiryDate || '12/28';
            this.fetchTransactions(acc.id);
            this.cdr.detectChanges();
          }
        });
      },
      error: (err) => console.error('Dashboard Load Error:', err)
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

  getDisplayType(txn: Transaction): 'credit' | 'debit' {
    if (!this.currentUserAccountNumber) return txn.type === 'credit' ? 'credit' : 'debit';
    if (txn.fromAccountNumber && txn.toAccountNumber) {
      return txn.toAccountNumber === this.currentUserAccountNumber ? 'credit' : 'debit';
    }
    return txn.type === 'credit' ? 'credit' : 'debit';
  }

  getTransactionSign(txn: Transaction): string {
    return this.getDisplayType(txn) === 'credit' ? '+' : '-';
  }

  onLogout(): void {
    this.authService.clearSession();
    this.router.navigate(['/']);
  }
}
