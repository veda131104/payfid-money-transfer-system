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

export interface VoucherItem {
  id: number;
  code: string;
  name: string;
  description: string;
  pointCost: number;
  cashValue: number;
  category: string;
  icon: string;
  active: boolean;
  stock: number;
  effectiveCost: number;
  canAfford: boolean;
}

export interface VoucherRedemption {
  id: number;
  accountId: number;
  voucherId: number;
  voucherCode: string;
  voucherName: string;
  pointsSpent: number;
  redemptionCode: string;
  redeemedAt: string;
  status: string;
}

export interface VoucherClaimRequest {
  voucherId: number;
  accountId: number;
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

  getAvailableVouchers(accountId: number): Observable<VoucherItem[]> {
    return this.http.get<VoucherItem[]>(`${this.baseUrl}/vouchers?accountId=${accountId}`);
  }

  redeemVoucher(request: VoucherClaimRequest): Observable<VoucherRedemption> {
    return this.http.post<VoucherRedemption>(`${this.baseUrl}/vouchers/redeem`, request);
  }

  getRedemptionHistory(accountId: number): Observable<VoucherRedemption[]> {
    return this.http.get<VoucherRedemption[]>(`${this.baseUrl}/${accountId}/redemptions`);
  }
}
