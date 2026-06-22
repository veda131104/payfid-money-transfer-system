import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatTabsModule } from '@angular/material/tabs';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { AuthService } from '../services/auth.service';
import { AccountSetupService } from '../services/account-setup.service';
import {
  RewardService, RewardSummary, RewardLedger,
  VoucherItem, VoucherRedemption
} from '../services/reward.service';

interface VoucherCategory {
  key: string;
  label: string;
  icon: string;
}

@Component({
  selector: 'app-rewards',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatCardModule, MatTabsModule,
    MatIconModule, MatChipsModule,
    RouterLink, RouterLinkActive
  ],
  templateUrl: './rewards.component.html',
  styleUrl: './rewards.component.scss'
})
export class RewardsComponent implements OnInit {
  private authService = inject(AuthService);
  private accountSetupService = inject(AccountSetupService);
  private rewardService = inject(RewardService);
  private router = inject(Router);
  private cdr = inject(ChangeDetectorRef);

  userName = 'User';
  accountId: number | null = null;
  routerLinkActiveOptions = { exact: true };

  activeTab = 0;

  points = 0;
  tier = 'Bronze';
  history: RewardLedger[] = [];
  vouchers: VoucherItem[] = [];
  filteredVouchers: VoucherItem[] = [];
  redemptions: VoucherRedemption[] = [];
  loading = true;
  errorMsg = '';

  selectedCategory = 'ALL';
  claimingVoucherId: number | null = null;
  claimSuccess: string | null = null;
  claimError: string | null = null;

  categories: VoucherCategory[] = [
    { key: 'ALL', label: 'All', icon: 'apps' },
    { key: 'SHOPPING', label: 'Shopping', icon: 'shopping_cart' },
    { key: 'FOOD', label: 'Food', icon: 'restaurant' },
    { key: 'TRAVEL', label: 'Travel', icon: 'directions_car' },
    { key: 'ENTERTAINMENT', label: 'Entertainment', icon: 'movie' },
  ];

  ngOnInit(): void {
    const user = this.authService.getCurrentUser();
    if (user) {
      this.userName = user.name;
      this.loadRewardsData(user.name);
    } else {
      this.loading = false;
      this.errorMsg = 'No user logged in. Please sign in to view rewards.';
    }
  }

  loadRewardsData(userName: string): void {
    this.accountSetupService.getAccountByUser(userName).subscribe({
      next: (bankDetails) => {
        if (bankDetails?.accountNumber) {
          this.accountSetupService.getAccountByNumber(bankDetails.accountNumber).subscribe({
            next: (account) => {
              if (account?.id) {
                this.accountId = account.id;
                this.fetchRewards(account.id);
              } else {
                this.loading = false;
                this.errorMsg = 'No active bank account associated with your profile.';
                this.cdr.detectChanges();
              }
            },
            error: () => {
              this.loading = false;
              this.errorMsg = 'Failed to load bank account details.';
              this.cdr.detectChanges();
            }
          });
        } else {
          this.loading = false;
          this.errorMsg = 'Please complete your Account Setup to start earning rewards.';
          this.cdr.detectChanges();
        }
      },
      error: () => {
        this.loading = false;
        this.errorMsg = 'Please complete your Account Setup to start earning rewards.';
        this.cdr.detectChanges();
      }
    });
  }

  fetchRewards(accountId: number): void {
    this.rewardService.initializeRewardAccount(accountId).subscribe({
      next: (summary) => {
        this.points = summary.totalPoints || 0;
        this.tier = this.calculateTier(this.points);
        this.loadHistory(accountId);
        this.loadVouchers(accountId);
        this.loadRedemptions(accountId);
      },
      error: () => {
        this.loading = false;
        this.errorMsg = 'Failed to load or initialize your reward wallet.';
        this.cdr.detectChanges();
      }
    });
  }

  loadHistory(accountId: number): void {
    this.rewardService.getRewardHistory(accountId).subscribe({
      next: (hist) => {
        this.history = hist || [];
        this.checkLoaded();
      },
      error: () => this.checkLoaded()
    });
  }

  loadVouchers(accountId: number): void {
    this.rewardService.getAvailableVouchers(accountId).subscribe({
      next: (vouchers) => {
        this.vouchers = vouchers || [];
        this.filterVouchers();
        this.checkLoaded();
      },
      error: () => this.checkLoaded()
    });
  }

  loadRedemptions(accountId: number): void {
    this.rewardService.getRedemptionHistory(accountId).subscribe({
      next: (redemptions) => {
        this.redemptions = redemptions || [];
        this.checkLoaded();
      },
      error: () => this.checkLoaded()
    });
  }

  private loadedCount = 0;
  private totalLoads = 3;

  checkLoaded(): void {
    this.loadedCount++;
    if (this.loadedCount >= this.totalLoads) {
      this.loading = false;
      this.cdr.detectChanges();
    }
  }

  filterVouchers(): void {
    if (this.selectedCategory === 'ALL') {
      this.filteredVouchers = [...this.vouchers];
    } else {
      this.filteredVouchers = this.vouchers.filter(v => v.category === this.selectedCategory);
    }
  }

  selectCategory(cat: string): void {
    this.selectedCategory = cat;
    this.filterVouchers();
  }

  claimVoucher(voucher: VoucherItem): void {
    if (!this.accountId) return;
    this.claimingVoucherId = voucher.id;
    this.claimSuccess = null;
    this.claimError = null;

    this.rewardService.redeemVoucher({ voucherId: voucher.id, accountId: this.accountId }).subscribe({
      next: (redemption) => {
        this.claimSuccess = `Successfully claimed "${voucher.name}"! Redemption code: ${redemption.redemptionCode}`;
        this.claimingVoucherId = null;
        this.points -= redemption.pointsSpent;
        this.tier = this.calculateTier(this.points);
        this.redemptions.unshift(redemption);
        this.loadVouchers(this.accountId!);
        this.loadHistory(this.accountId!);
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.claimError = err.error?.error || 'Failed to redeem voucher. Please try again.';
        this.claimingVoucherId = null;
        this.cdr.detectChanges();
      }
    });
  }

  copyToClipboard(text: string): void {
    if (navigator.clipboard) {
      navigator.clipboard.writeText(text);
    }
  }

  calculateTier(points: number): string {
    if (points >= 500) return 'Platinum';
    if (points >= 200) return 'Gold';
    if (points >= 100) return 'Silver';
    if (points >= 50) return 'Bronze';
    return 'Starter';
  }

  getTierColor(tier: string): string {
    switch (tier) {
      case 'Platinum': return '#94a3b8';
      case 'Gold': return '#f59e0b';
      case 'Silver': return '#9ca3af';
      case 'Bronze': return '#d97706';
      default: return '#6b7280';
    }
  }

  getTierDiscount(tier: string): string {
    switch (tier) {
      case 'Platinum': return '20% off';
      case 'Gold': return '15% off';
      case 'Silver': return '10% off';
      case 'Bronze': return '5% off';
      default: return '0% off';
    }
  }

  pointsToNextTier(): number {
    if (this.points < 50) return 50 - this.points;
    if (this.points < 100) return 100 - this.points;
    if (this.points < 200) return 200 - this.points;
    if (this.points < 500) return 500 - this.points;
    return 0;
  }

  nextTierName(): string {
    if (this.points < 50) return 'Bronze';
    if (this.points < 100) return 'Silver';
    if (this.points < 200) return 'Gold';
    if (this.points < 500) return 'Platinum';
    return 'Max';
  }

  onLogout(): void {
    this.authService.clearSession();
    this.router.navigate(['/']);
  }
}
