import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { AuthService } from '../services/auth.service';
import { AccountSetupService } from '../services/account-setup.service';
import { RewardService, RewardSummary, RewardLedger } from '../services/reward.service';

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
              console.error('Error fetching account details:', err);
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
        console.error('Error fetching bank details:', err);
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
            console.error('Error fetching reward history:', err);
            this.loading = false;
            this.cdr.detectChanges();
          }
        });
      },
      error: (err) => {
        console.error('Error initializing reward account:', err);
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

  getTierColor(tier: string): string {
    switch (tier) {
      case 'Platinum': return '#e5e5e5';
      case 'Gold': return '#ffd700';
      case 'Silver': return '#c0c0c0';
      default: return '#cd7f32'; // Bronze
    }
  }

  onLogout(): void {
    this.authService.clearSession();
    this.router.navigate(['/']);
  }
}
