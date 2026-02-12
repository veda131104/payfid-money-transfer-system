import { HttpClient } from '@angular/common/http';
import { Injectable, Inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { BehaviorSubject, Observable } from 'rxjs';

export interface Transaction {
  id: string;
  accountNumber: string;
  fromAccountNumber?: string;  // Account that sent the money
  toAccountNumber?: string;    // Account that received the money
  amount: string;
  date: Date;
  type: 'debit' | 'credit' | 'transfer';
  status: 'completed' | 'pending' | 'failed' | 'SUCCESS' | 'FAILED';
  referenceId: string;
  description?: string;
  fromAccountHolderName?: string;
  toAccountHolderName?: string;
}

@Injectable({
  providedIn: 'root'
})
export class TransactionService {
  private readonly baseUrl = 'http://localhost:8080/api/v1/transfers';
  private transactionsSubject = new BehaviorSubject<Transaction[]>([]);
  public transactions$ = this.transactionsSubject.asObservable();

  constructor(
    private http: HttpClient,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {
    this.loadTransactionsFromStorage();
  }

  executeTransfer(payload: { fromAccountNumber: string, toAccountNumber: string, amount: number }): Observable<any> {
    return this.http.post(`${this.baseUrl}/by-account`, payload);
  }

  private isBrowser(): boolean {
    return isPlatformBrowser(this.platformId);
  }

  private loadTransactionsFromStorage(): void {
    if (!this.isBrowser()) {
      return;
    }

    const stored = localStorage.getItem('transactions');
    if (stored) {
      try {
        const transactions = JSON.parse(stored).map((t: any) => ({
          ...t,
          date: new Date(t.date)
        }));
        this.transactionsSubject.next(transactions);
      } catch (e) {
        console.error('Error loading transactions from storage', e);
      }
    }
  }

  private mapDtoToTransaction(dto: any): Transaction {
    return {
      id: dto.id.toString(),
      accountNumber: dto.fromAccountNumber || dto.toAccountNumber || '',
      fromAccountNumber: dto.fromAccountNumber,
      toAccountNumber: dto.toAccountNumber,
      amount: dto.amount.toString(),
      date: new Date(dto.transactionDate),
      type: dto.type.toLowerCase() as 'debit' | 'credit' | 'transfer',
      status: dto.status as 'SUCCESS' | 'FAILED' | 'completed' | 'pending' | 'failed',
      referenceId: dto.idempotencyKey || `TXN${dto.id}`,
      description: dto.description,
      fromAccountHolderName: dto.fromAccountHolderName,
      toAccountHolderName: dto.toAccountHolderName
    };
  }

  getAccountHistory(accountId: number): Observable<Transaction[]> {
    return new Observable<Transaction[]>(observer => {
      this.http.get<any>(`${this.baseUrl}/account/${accountId}`).subscribe({
        next: (response) => {
          const transactions = (response.transactions || []).map((dto: any) => this.mapDtoToTransaction(dto));
          this.transactionsSubject.next(transactions);
          observer.next(transactions);
          observer.complete();
        },
        error: (err) => {
          observer.error(err);
        }
      });
    });
  }

  private saveTransactionsToStorage(): void {
    if (!this.isBrowser()) {
      return;
    }

    localStorage.setItem('transactions', JSON.stringify(this.transactionsSubject.value));
  }

  addTransaction(transaction: Transaction): void {
    const current = this.transactionsSubject.value;
    const updated = [transaction, ...current];
    this.transactionsSubject.next(updated);
    this.saveTransactionsToStorage();
  }

  getTransactions(): Transaction[] {
    return this.transactionsSubject.value;
  }

  generateReferenceId(): string {
    return 'TXN' + Date.now().toString().slice(-6);
  }
}
