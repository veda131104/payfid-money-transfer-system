import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { AuthService } from '../services/auth.service';
import { AccountSetupService } from '../services/account-setup.service';
import { RewardService, RewardSummary, RewardLedger } from '../services/reward.service';
import { PopupService } from '../services/popup.service';

@Component({
  selector: 'app-rewards',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatCardModule, RouterLink, RouterLinkActive],
  templateUrl: './rewards.component.html',
  styleUrl: './rewards.component.scss'
})
export class RewardsComponent implements OnInit {
  private authService = inject(AuthService);
  private accountSetupService = inject(AccountSetupService);
  private rewardService = inject(RewardService);
  private popupService = inject(PopupService);
  private router = inject(Router);
  private cdr = inject(ChangeDetectorRef);

  userName = 'User';
  accountId: number | null = null;
  routerLinkActiveOptions = { exact: true };
  
  points = 0;
  tier = 'Bronze';
  history: RewardLedger[] = [];
  loading = true;
  errorMsg = '';

  // Discrete view states
  showRules = false;
  
  // Scratch Card Modal States
  showScratchModal = false;
  scratchRevealed = false;
  scratchSelectedCoupon: any = null;
  scratchRedeemedCode = '';

  coupons = [
    { id: 'amz_100', provider: 'Amazon', cost: 50, rewardVal: '₹100 Gift Card', icon: '🛍️', themeColor: '#ff9900' },
    { id: 'myn_150', provider: 'Myntra', cost: 70, rewardVal: '₹150 Voucher', icon: '👗', themeColor: '#ff3f6c' },
    { id: 'zom_50', provider: 'Zomato', cost: 25, rewardVal: '₹50 Off Meals', icon: '🍕', themeColor: '#cb202d' },
    { id: 'sbx_tall', provider: 'Starbucks', cost: 40, rewardVal: 'Free Tall Coffee', icon: '☕', themeColor: '#00704a' },
    { id: 'bms_100', provider: 'BookMyShow', cost: 45, rewardVal: '₹100 Off Movie', icon: '🎬', themeColor: '#f84464' }
  ];

  tiersList = [
    { name: 'Bronze', points: '0-19 pts', multiplier: '1.0x Points', perk: 'Standard Instant Processing Speed', color: '#cd7f32' },
    { name: 'Silver', points: '20-49 pts', multiplier: '1.1x Points', perk: 'Priority Support Access & Extra Points', color: '#c0c0c0' },
    { name: 'Gold', points: '50-99 pts', multiplier: '1.2x Points', perk: 'Zero Charges on Instant Transfers & Exclusive Brand Vouchers', color: '#ffd700' },
    { name: 'Platinum', points: '100+ pts', multiplier: '1.5x Points', perk: 'Dedicated 24/7 Personal Manager & Free High-Value Transfers', color: '#e5e5e5' }
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
            error: (err) => {
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
      error: (err) => {
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
        
        this.rewardService.getRewardHistory(accountId).subscribe({
          next: (hist) => {
            this.history = hist || [];
            this.loading = false;
            this.cdr.detectChanges();
          },
          error: (err) => {
            this.loading = false;
            this.cdr.detectChanges();
          }
        });
      },
      error: (err) => {
        this.loading = false;
        this.errorMsg = 'Failed to load or initialize your reward wallet.';
        this.cdr.detectChanges();
      }
    });
  }

  calculateTier(points: number): string {
    if (points >= 100) return 'Platinum';
    if (points >= 50) return 'Gold';
    if (points >= 20) return 'Silver';
    return 'Bronze';
  }

  getTierColor(tierName: string): string {
    switch (tierName) {
      case 'Platinum': return '#e5e5e5';
      case 'Gold': return '#ffd700';
      case 'Silver': return '#c0c0c0';
      default: return '#cd7f32'; // Bronze
    }
  }

  redeemCoupon(coupon: any): void {
    if (!this.accountId) {
      this.popupService.alert('You must have an active bank account to redeem points.', 'Error');
      return;
    }

    if (this.points < coupon.cost) {
      this.popupService.alert(`Insufficient points! You need ${coupon.cost} points for this coupon. Currently you have ${this.points}.`, 'Redemption Failed');
      return;
    }

    const description = `Redeemed ${coupon.provider} ${coupon.rewardVal}`;
    this.rewardService.redeemRewards(this.accountId, coupon.cost, description).subscribe({
      next: (ledgerEntry) => {
        this.points -= coupon.cost;
        this.tier = this.calculateTier(this.points);
        this.fetchRewards(this.accountId!);

        this.scratchSelectedCoupon = coupon;
        this.scratchRedeemedCode = this.generatePromoCode(coupon.provider);
        this.scratchRevealed = false;
        this.showScratchModal = true;
        this.cdr.detectChanges();
      },
      error: (err) => {
        const msg = err.error?.message || 'Failed to redeem points. Please try again.';
        this.popupService.alert(msg, 'Redemption Error');
      }
    });
  }

  generatePromoCode(provider: string): string {
    const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
    let code = '';
    for (let i = 0; i < 8; i++) {
      code += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    return `PF-${provider.substring(0, 3).toUpperCase()}-${code}`;
  }

  copyPromoCode(): void {
    if (typeof navigator !== 'undefined' && navigator.clipboard) {
      navigator.clipboard.writeText(this.scratchRedeemedCode).then(() => {
        this.popupService.alert('Promo code copied to clipboard!', 'Success');
      });
    }
  }

  closeScratchModal(): void {
    this.showScratchModal = false;
    this.scratchSelectedCoupon = null;
    this.scratchRedeemedCode = '';
    this.scratchRevealed = false;
  }

  onLogout(): void {
    this.authService.clearSession();
    this.router.navigate(['/']);
  }
}
