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
    { id: 'amz_100', provider: 'Amazon', cost: 50, rewardVal: '₹100 Gift Card', icon: '🛍️', themeColor: '#ff9900', storeUrl: 'https://www.amazon.in' },
    { id: 'myn_150', provider: 'Myntra', cost: 70, rewardVal: '₹150 Voucher', icon: '👗', themeColor: '#ff3f6c', storeUrl: 'https://www.myntra.com' },
    { id: 'zom_50', provider: 'Zomato', cost: 25, rewardVal: '₹50 Off Meals', icon: '🍕', themeColor: '#cb202d', storeUrl: 'https://www.zomato.com' },
    { id: 'sbx_tall', provider: 'Starbucks', cost: 40, rewardVal: 'Free Tall Coffee', icon: '☕', themeColor: '#00704a', storeUrl: 'https://www.starbucks.in' },
    { id: 'bms_100', provider: 'BookMyShow', cost: 45, rewardVal: '₹100 Off Movie', icon: '🎬', themeColor: '#f84464', storeUrl: 'https://in.bookmyshow.com' },
    { id: 'swg_60', provider: 'Swiggy', cost: 30, rewardVal: '₹60 Food Voucher', icon: '🍔', themeColor: '#fc8019', storeUrl: 'https://www.swiggy.com' },
    { id: 'ubr_75', provider: 'Uber', cost: 35, rewardVal: '₹75 Ride Discount', icon: '🚗', themeColor: '#090909', storeUrl: 'https://www.uber.com' },
    { id: 'spt_pre', provider: 'Spotify', cost: 60, rewardVal: '1-Month Premium', icon: '🎵', themeColor: '#1db954', storeUrl: 'https://www.spotify.com' },
    { id: 'flp_200', provider: 'Flipkart', cost: 90, rewardVal: '₹200 Gift Card', icon: '🛒', themeColor: '#2874f0', storeUrl: 'https://www.flipkart.com' }
  ];

  tiersList = [
    { name: 'Bronze', points: '0-19 pts', multiplier: '1.0x', perk: 'Standard payouts & basic support speed', color: '#cd7f32' },
    { name: 'Silver', points: '20-49 pts', multiplier: '1.1x', perk: 'Priority email query handling & basic brand coupons access', color: '#8a9ba8' },
    { name: 'Gold', points: '50-99 pts', multiplier: '1.25x', perk: 'Zero fee on all domestic transfers & premium high-value coupons', color: '#d4af37' },
    { name: 'Platinum', points: '100+ pts', multiplier: '1.5x', perk: 'Dedicated VIP relationship manager & unlimited zero-charge payouts', color: '#b4c4cc' }
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

  get nextTierName(): string {
    if (this.points < 20) return 'Silver';
    if (this.points < 50) return 'Gold';
    if (this.points < 100) return 'Platinum';
    return '';
  }

  get nextTierPoints(): number {
    if (this.points < 20) return 20;
    if (this.points < 50) return 50;
    if (this.points < 100) return 100;
    return 100;
  }

  get pointsToNextTier(): number {
    if (this.points < 20) return 20 - this.points;
    if (this.points < 50) return 50 - this.points;
    if (this.points < 100) return 100 - this.points;
    return 0;
  }

  get tierProgressPercent(): number {
    if (this.points >= 100) return 100;
    if (this.points < 20) return Math.min(100, Math.max(0, (this.points / 20) * 100));
    if (this.points < 50) return Math.min(100, Math.max(0, ((this.points - 20) / 30) * 100));
    return Math.min(100, Math.max(0, ((this.points - 50) / 50) * 100));
  }

  getTierColor(tierName: string): string {
    switch (tierName) {
      case 'Platinum': return '#b4c4cc';
      case 'Gold': return '#d4af37';
      case 'Silver': return '#8a9ba8';
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
