import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface RewardSummary {
  rewardAccountId: number;
  accountId: number;
  totalPoints: number;
  createdAt: string;
  updatedAt: string;
}

export interface RewardLedger {
  id: number;
  accountId: number;
  transactionId: number;
  transactionAmount: number;
  pointsAwarded: number;
  description: string;
  grantedAt: string;
}

@Injectable({
  providedIn: 'root'
})
export class RewardService {
  private readonly baseUrl = 'http://localhost:8080/api/v1/rewards';

  constructor(private readonly http: HttpClient) {}

  getRewardSummary(accountId: number): Observable<RewardSummary> {
    return this.http.get<RewardSummary>(`${this.baseUrl}/${accountId}/summary`);
  }

  getRewardHistory(accountId: number): Observable<RewardLedger[]> {
    return this.http.get<RewardLedger[]>(`${this.baseUrl}/${accountId}/history`);
  }

  initializeRewardAccount(accountId: number): Observable<RewardSummary> {
    return this.http.post<RewardSummary>(`${this.baseUrl}/${accountId}/initialize`, {});
  }
}
