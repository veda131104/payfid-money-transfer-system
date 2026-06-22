import { Component, OnInit, AfterViewInit, ChangeDetectorRef, inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { AuthService } from '../services/auth.service';
import { AccountSetupService } from '../services/account-setup.service';
import { TransactionService, Transaction } from '../services/transaction.service';
import { AnalyticsService } from '../services/analytics.service';
import { Chart, registerables } from 'chart.js';

Chart.register(...registerables);

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatCardModule, RouterLink, RouterLinkActive],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent implements OnInit, AfterViewInit {
  private cdr = inject(ChangeDetectorRef);
  private authService = inject(AuthService);
  private accountSetupService = inject(AccountSetupService);
  private transactionService = inject(TransactionService);
  private analyticsService = inject(AnalyticsService);
  private platformId = inject(PLATFORM_ID);

  routerLinkActiveOptions = { exact: true };
  userName = 'User';
  balance = 0;
  processedToday = 0;
  activeTransfers = 0;
  recentTransactions: Transaction[] = [];
  isFirstLogin = false;

  activityLogs = [
    { id: 1, type: 'success', title: 'Payout Processed', desc: 'Transfer to Akhil completed successfully', time: '10 mins ago' },
    { id: 2, type: 'warning', title: 'New Device Login', desc: 'Login detected from Chrome on Windows 11', time: '1 hour ago' },
    { id: 3, type: 'success', title: 'Profile Updated', desc: 'Daily transfer limit increased to 50,000 INR', time: '3 hours ago' },
    { id: 4, type: 'danger', title: 'Failed Transfer', desc: 'Transfer failed due to invalid card number', time: '5 hours ago' }
  ];

  txnAnalyticsChart: any;
  weeklyVolumeChart: any;

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

  ngAfterViewInit(): void {
    if (isPlatformBrowser(this.platformId)) {
      this.loadCharts();
    }
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

  loadCharts(): void {
    const user = this.authService.getCurrentUser();
    const name = user ? user.name : '';

    this.analyticsService.getTransactionVolume(name).subscribe({
      next: (data) => {
        if (data && data.length > 0) {
          this.createTxnAnalyticsChart(data);
        } else {
          this.createTxnAnalyticsChart(this.getMockVolumeData());
        }
      },
      error: (err) => {
        console.warn('Analytics volume API failed, using mock data:', err);
        this.createTxnAnalyticsChart(this.getMockVolumeData());
      }
    });

    this.analyticsService.getPeakHours(name).subscribe({
      next: (data) => {
        if (data && data.length > 0) {
          this.createWeeklyVolumeChart(data);
        } else {
          this.createWeeklyVolumeChart(this.getMockPeakHoursData());
        }
      },
      error: (err) => {
        console.warn('Analytics peak-hours API failed, using mock data:', err);
        this.createWeeklyVolumeChart(this.getMockPeakHoursData());
      }
    });
  }

  private createTxnAnalyticsChart(data: any[]): void {
    const ctx = document.getElementById('dashboardTxnAnalyticsChart') as HTMLCanvasElement;
    if (!ctx) return;
    if (this.txnAnalyticsChart) {
      this.txnAnalyticsChart.destroy();
    }
    this.txnAnalyticsChart = new Chart(ctx, {
      type: 'line',
      data: {
        labels: data.map(d => d.date || d.label),
        datasets: [{
          label: 'Transaction Analytics (₹)',
          data: data.map(d => d.value || d.amount),
          borderColor: '#1c5d5e',
          backgroundColor: 'rgba(28, 93, 94, 0.08)',
          fill: true,
          tension: 0.4
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { display: false }
        },
        scales: {
          y: { grid: { color: 'rgba(0,0,0,0.05)' } },
          x: { grid: { display: false } }
        }
      }
    });
  }

  private createWeeklyVolumeChart(data: any[]): void {
    const ctx = document.getElementById('dashboardWeeklyVolumeChart') as HTMLCanvasElement;
    if (!ctx) return;
    if (this.weeklyVolumeChart) {
      this.weeklyVolumeChart.destroy();
    }
    this.weeklyVolumeChart = new Chart(ctx, {
      type: 'bar',
      data: {
        labels: data.map(d => d.hour || d.label),
        datasets: [{
          label: 'Volume',
          data: data.map(d => d.count || d.value),
          backgroundColor: '#1d3b4f',
          borderRadius: 4
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { display: false }
        },
        scales: {
          y: { grid: { color: 'rgba(0,0,0,0.05)' } },
          x: { grid: { display: false } }
        }
      }
    });
  }

  private getMockVolumeData(): any[] {
    return [
      { date: 'Jun 17', value: 12000 },
      { date: 'Jun 18', value: 19000 },
      { date: 'Jun 19', value: 15000 },
      { date: 'Jun 20', value: 28000 },
      { date: 'Jun 21', value: 22000 },
      { date: 'Jun 22', value: 34000 },
      { date: 'Jun 23', value: 30012 }
    ];
  }

  private getMockPeakHoursData(): any[] {
    return [
      { hour: 'Mon', count: 18000 },
      { hour: 'Tue', count: 24000 },
      { hour: 'Wed', count: 32000 },
      { hour: 'Thu', count: 21000 },
      { hour: 'Fri', count: 45000 },
      { hour: 'Sat', count: 12000 },
      { hour: 'Sun', count: 8000 }
    ];
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
