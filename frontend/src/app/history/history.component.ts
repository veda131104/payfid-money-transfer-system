import { Component, OnInit, OnDestroy, ChangeDetectorRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive, NavigationEnd } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { TransactionService, Transaction } from '../services/transaction.service';
import { AccountSetupService } from '../services/account-setup.service';
import { AuthService } from '../services/auth.service';
import { Subject, filter, switchMap, takeUntil, of, tap } from 'rxjs';

@Component({
  selector: 'app-history',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatCardModule, RouterLink, RouterLinkActive],
  templateUrl: './history.component.html',
  styleUrl: './history.component.scss'
})
export class HistoryComponent implements OnInit, OnDestroy {
  private cdr = inject(ChangeDetectorRef);
  private destroy$ = new Subject<void>();
  private refresh$ = new Subject<void>();
  routerLinkActiveOptions = { exact: true };
  allTransactions: Transaction[] = [];
  displayedTransactions: Transaction[] = [];
  selectedTransaction: Transaction | null = null;
  itemsPerPage: number = 5;
  currentPage: number = 0;
  hasMoreTransactions: boolean = true;
  currentUserAccountNumber: string = 'loading...';
  userName: string = 'User';
  currentBalance: string = '--.--';
  isLoading: boolean = false;
  currentFilter: 'all' | 'today' | 'month' | 'year' = 'all';
  rawTransactions: Transaction[] = [];

  constructor(
    private router: Router,
    private transactionService: TransactionService,
    private accountSetupService: AccountSetupService,
    private authService: AuthService
  ) { }

  ngOnInit(): void {
    console.log('🔍 [History] Component initializing...');

    // 1. Listen for router events to refresh data on every navigation to this component
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd),
      filter(() => this.router.url.includes('/history')),
      takeUntil(this.destroy$)
    ).subscribe(() => {
      console.log('🚀 [History] Navigation detected, triggering refresh...');
      this.refresh$.next();
    });

    // 2. Main reactive data pipeline using switchMap for sequential requests
    this.refresh$.pipe(
      tap(() => {
        this.isLoading = true;
        this.cdr.detectChanges();
        console.log('📡 [History] Starting fresh data fetch...');
      }),
      switchMap(() => {
        const user = this.authService.getCurrentUser();
        if (!user) {
          console.warn('⚠️ [History] No user logged in');
          return of(null);
        }
        this.userName = user.name;
        return this.accountSetupService.getAccountByUser(user.name);
      }),
      switchMap(details => {
        if (!details || !details.accountNumber) {
          console.warn('⚠️ [History] Bank details missing account number');
          return of(null);
        }
        this.currentUserAccountNumber = details.accountNumber;
        // Balance UI update might happen here if we had partial data, but we'll wait for account
        return this.accountSetupService.getAccountByNumber(details.accountNumber);
      }),
      switchMap(account => {
        if (!account || account.balance === undefined) {
          console.warn('⚠️ [History] Account data missing or invalid');
          return of(null);
        }
        this.currentBalance = account.balance.toFixed(2);
        console.log('💰 [History] Balance loaded:', this.currentBalance);
        this.cdr.detectChanges(); // Update UI with balance immediately

        if (account.id) {
          console.log('📡 [History] Fetching transactions for account ID:', account.id);
          return this.transactionService.getAccountHistory(account.id);
        }
        return of([]);
      }),
      takeUntil(this.destroy$)
    ).subscribe({
      next: (txns) => {
        this.isLoading = false;
        if (txns) {
          console.log('✅ [History] Data pipeline complete, transactions count:', txns.length);
          this.rawTransactions = txns;
          this.applyFilter();
        }
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.isLoading = false;
        console.error('❌ [History] Data pipeline error:', err);
        this.cdr.detectChanges();
      }
    });

    // 3. Subscribe to service-side transaction updates (like after a transfer confirms)
    this.transactionService.transactions$.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      console.log('🔄 [History] Service transactions updated, syncing UI...');
      this.rawTransactions = this.transactionService.getTransactions();
      this.applyFilter();
      this.cdr.detectChanges();
    });

    // Initial trigger
    this.refresh$.next();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadComponentData(userName: string): void {
    // Legacy method, moved logic to refresh$ pipeline
  }

  initializeTransactions(): void {
    const sampleTransactions: Transaction[] = [
      {
        id: '1',
        accountNumber: '9876543210',
        amount: '5000',
        date: new Date(2026, 1, 10, 14, 30),
        type: 'debit',
        status: 'completed',
        referenceId: 'TXN001',
        description: 'Payment to Vendor'
      },
      {
        id: '2',
        accountNumber: '1234567890',
        amount: '15000',
        date: new Date(2026, 1, 9, 10, 15),
        type: 'credit',
        status: 'completed',
        referenceId: 'TXN002',
        description: 'Salary credit'
      },
      {
        id: '3',
        accountNumber: '5555666677',
        amount: '2500',
        date: new Date(2026, 1, 8, 16, 45),
        type: 'debit',
        status: 'completed',
        referenceId: 'TXN003',
        description: 'Utility bill payment'
      },
      {
        id: '4',
        accountNumber: '1111222233',
        amount: '10000',
        date: new Date(2026, 1, 7, 9, 20),
        type: 'credit',
        status: 'completed',
        referenceId: 'TXN004',
        description: 'Refund processed'
      },
      {
        id: '5',
        accountNumber: '9999888877',
        amount: '3000',
        date: new Date(2026, 1, 6, 13, 10),
        type: 'debit',
        status: 'completed',
        referenceId: 'TXN005',
        description: 'Transfer to friend'
      },
      {
        id: '6',
        accountNumber: '4444555566',
        amount: '7500',
        date: new Date(2026, 1, 5, 11, 55),
        type: 'credit',
        status: 'completed',
        referenceId: 'TXN006',
        description: 'Freelance payment'
      },
      {
        id: '7',
        accountNumber: '7777888899',
        amount: '1200',
        date: new Date(2026, 1, 4, 15, 30),
        type: 'debit',
        status: 'pending',
        referenceId: 'TXN007',
        description: 'Online shopping'
      },
      {
        id: '8',
        accountNumber: '2222333344',
        amount: '8000',
        date: new Date(2026, 1, 3, 12, 0),
        type: 'credit',
        status: 'completed',
        referenceId: 'TXN008',
        description: 'Investment return'
      },
      {
        id: '9',
        accountNumber: '6666777788',
        amount: '25000',
        date: new Date(2026, 1, 2, 8, 45),
        type: 'credit',
        status: 'completed',
        referenceId: 'TXN009',
        description: 'Bonus credit'
      },
      {
        id: '10',
        accountNumber: '3333444455',
        amount: '4500',
        date: new Date(2026, 0, 31, 14, 20),
        type: 'debit',
        status: 'completed',
        referenceId: 'TXN010',
        description: 'Insurance premium'
      },
      {
        id: '11',
        accountNumber: '5555666688',
        amount: '12000',
        date: new Date(2026, 0, 30, 10, 10),
        type: 'credit',
        status: 'completed',
        referenceId: 'TXN011',
        description: 'Dividend payment'
      },
      {
        id: '12',
        accountNumber: '8888999900',
        amount: '6000',
        date: new Date(2026, 0, 29, 16, 35),
        type: 'debit',
        status: 'completed',
        referenceId: 'TXN012',
        description: 'Payment to contractor'
      },
      {
        id: '13',
        accountNumber: '1010202030',
        amount: '18500',
        date: new Date(2026, 0, 28, 9, 0),
        type: 'credit',
        status: 'completed',
        referenceId: 'TXN013',
        description: 'Project completion payment'
      },
      {
        id: '14',
        accountNumber: '9090919192',
        amount: '3200',
        date: new Date(2026, 0, 27, 13, 45),
        type: 'debit',
        status: 'completed',
        referenceId: 'TXN014',
        description: 'Groceries and supplies'
      },
      {
        id: '15',
        accountNumber: '4545464748',
        amount: '9500',
        date: new Date(2026, 0, 26, 11, 20),
        type: 'credit',
        status: 'completed',
        referenceId: 'TXN015',
        description: 'Consulting fee received'
      }
    ];

    sampleTransactions.forEach(t => this.transactionService.addTransaction(t));
  }

  loadMore(): void {
    const filtered = this.getFilteredTransactions();
    const startIndex = this.currentPage * this.itemsPerPage;
    const endIndex = startIndex + this.itemsPerPage;
    const newTransactions = filtered.slice(startIndex, endIndex);

    this.displayedTransactions = [...this.displayedTransactions, ...newTransactions];
    this.currentPage++;

    this.hasMoreTransactions = endIndex < filtered.length;
  }

  setFilter(filter: 'all' | 'today' | 'month' | 'year'): void {
    this.currentFilter = filter;
    this.applyFilter();
  }

  private applyFilter(): void {
    this.displayedTransactions = [];
    this.currentPage = 0;
    this.loadMore();
    this.cdr.detectChanges();
  }

  private getFilteredTransactions(): Transaction[] {
    const now = new Date();
    const today = now.toDateString();
    const currentMonth = now.getMonth();
    const currentYear = now.getFullYear();

    switch (this.currentFilter) {
      case 'today':
        return this.rawTransactions.filter(t => new Date(t.date).toDateString() === today);
      case 'month':
        return this.rawTransactions.filter(t => {
          const d = new Date(t.date);
          return d.getMonth() === currentMonth && d.getFullYear() === currentYear;
        });
      case 'year':
        return this.rawTransactions.filter(t => new Date(t.date).getFullYear() === currentYear);
      default:
        return this.rawTransactions;
    }
  }

  openTransactionDetail(transaction: Transaction, index: number): void {
    this.selectedTransaction = transaction;
  }

  closeTransactionDetail(): void {
    this.selectedTransaction = null;
  }

  addTransaction(transaction: Transaction): void {
    this.allTransactions.unshift(transaction);
    this.displayedTransactions = [];
    this.currentPage = 0;
    this.loadMore();
  }

  /**
   * Determines the sign (+/-) for a transaction based on user perspective
   * For transfers: + if user is receiver, - if user is sender
   * For credit/debit: follows the type
   */
  getTransactionSign(transaction: Transaction): string {
    if (!this.currentUserAccountNumber || this.currentUserAccountNumber === 'loading...') {
      return '';
    }

    if (transaction.fromAccountNumber && transaction.toAccountNumber) {
      if (transaction.toAccountNumber === this.currentUserAccountNumber) {
        return '+';
      }
      if (transaction.fromAccountNumber === this.currentUserAccountNumber) {
        return '-';
      }
    }

    return transaction.type === 'credit' ? '+' : '-';
  }

  /**
   * Determines the display type (credit/debit) based on user perspective
   */
  getDisplayType(transaction: Transaction): 'credit' | 'debit' {
    if (!this.currentUserAccountNumber || this.currentUserAccountNumber === 'loading...') {
      return transaction.type === 'credit' ? 'credit' : 'debit';
    }

    if (transaction.fromAccountNumber && transaction.toAccountNumber) {
      if (transaction.toAccountNumber === this.currentUserAccountNumber) {
        return 'credit';
      }
      if (transaction.fromAccountNumber === this.currentUserAccountNumber) {
        return 'debit';
      }
    }

    return transaction.type === 'credit' ? 'credit' : 'debit';
  }

  onLogout(): void {
    this.authService.clearSession();
    this.router.navigate(['/']);
  }
}
