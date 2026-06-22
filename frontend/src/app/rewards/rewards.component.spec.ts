import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RewardsComponent } from './rewards.component';
import { AuthService } from '../services/auth.service';
import { AccountSetupService } from '../services/account-setup.service';
import { RewardService } from '../services/reward.service';
import { Router } from '@angular/router';
import { ChangeDetectorRef } from '@angular/core';
import { of, throwError } from 'rxjs';
import { RouterTestingModule } from '@angular/router/testing';

describe('RewardsComponent', () => {
  let component: RewardsComponent;
  let fixture: ComponentFixture<RewardsComponent>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let accountSetupServiceSpy: jasmine.SpyObj<AccountSetupService>;
  let rewardServiceSpy: jasmine.SpyObj<RewardService>;
  let router: Router;

  beforeEach(async () => {
    authServiceSpy = jasmine.createSpyObj('AuthService', ['getCurrentUser', 'clearSession']);
    accountSetupServiceSpy = jasmine.createSpyObj('AccountSetupService', ['getAccountByUser', 'getAccountByNumber']);
    rewardServiceSpy = jasmine.createSpyObj('RewardService', ['initializeRewardAccount', 'getRewardHistory']);

    await TestBed.configureTestingModule({
      imports: [RewardsComponent, RouterTestingModule],
      providers: [
        { provide: AuthService, useValue: authServiceSpy },
        { provide: AccountSetupService, useValue: accountSetupServiceSpy },
        { provide: RewardService, useValue: rewardServiceSpy }
      ]
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(RewardsComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    spyOn(router, 'navigate');
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should show error message if no user is logged in', () => {
    authServiceSpy.getCurrentUser.and.returnValue(null);
    fixture.detectChanges();

    expect(component.loading).toBeFalse();
    expect(component.errorMsg).toBe('No user logged in. Please sign in to view rewards.');
  });

  it('should load rewards data successfully for logged in user', () => {
    authServiceSpy.getCurrentUser.and.returnValue({ name: 'Bob', email: 'bob@company.com' });
    accountSetupServiceSpy.getAccountByUser.and.returnValue(of({ accountNumber: '111122223333' }));
    accountSetupServiceSpy.getAccountByNumber.and.returnValue(of({ id: 99 }));
    rewardServiceSpy.initializeRewardAccount.and.returnValue(of({
      rewardAccountId: 1,
      accountId: 99,
      totalPoints: 75,
      createdAt: '2026-01-01',
      updatedAt: '2026-01-01'
    }));
    rewardServiceSpy.getRewardHistory.and.returnValue(of([{ id: 1 } as any]));

    fixture.detectChanges();

    expect(component.userName).toBe('Bob');
    expect(component.accountId).toBe(99);
    expect(component.points).toBe(75);
    expect(component.tier).toBe('Gold');
    expect(component.history.length).toBe(1);
    expect(component.loading).toBeFalse();
  });

  it('should handle getAccountByUser error', () => {
    authServiceSpy.getCurrentUser.and.returnValue({ name: 'Bob', email: 'bob@company.com' });
    accountSetupServiceSpy.getAccountByUser.and.returnValue(throwError(() => new Error('Not found')));

    fixture.detectChanges();

    expect(component.loading).toBeFalse();
    expect(component.errorMsg).toBe('Please complete your Account Setup to start earning rewards.');
  });

  it('should handle getAccountByUser empty account number', () => {
    authServiceSpy.getCurrentUser.and.returnValue({ name: 'Bob', email: 'bob@company.com' });
    accountSetupServiceSpy.getAccountByUser.and.returnValue(of({ accountNumber: '' }));

    fixture.detectChanges();

    expect(component.loading).toBeFalse();
    expect(component.errorMsg).toBe('Please complete your Account Setup to start earning rewards.');
  });

  it('should handle getAccountByNumber error', () => {
    authServiceSpy.getCurrentUser.and.returnValue({ name: 'Bob', email: 'bob@company.com' });
    accountSetupServiceSpy.getAccountByUser.and.returnValue(of({ accountNumber: '111122223333' }));
    accountSetupServiceSpy.getAccountByNumber.and.returnValue(throwError(() => new Error('Account error')));

    fixture.detectChanges();

    expect(component.loading).toBeFalse();
    expect(component.errorMsg).toBe('Failed to load bank account details.');
  });

  it('should handle getAccountByNumber empty account id', () => {
    authServiceSpy.getCurrentUser.and.returnValue({ name: 'Bob', email: 'bob@company.com' });
    accountSetupServiceSpy.getAccountByUser.and.returnValue(of({ accountNumber: '111122223333' }));
    accountSetupServiceSpy.getAccountByNumber.and.returnValue(of({ id: undefined }));

    fixture.detectChanges();

    expect(component.loading).toBeFalse();
    expect(component.errorMsg).toBe('No active bank account associated with your profile.');
  });

  it('should handle reward wallet initialization error', () => {
    authServiceSpy.getCurrentUser.and.returnValue({ name: 'Bob', email: 'bob@company.com' });
    accountSetupServiceSpy.getAccountByUser.and.returnValue(of({ accountNumber: '111122223333' }));
    accountSetupServiceSpy.getAccountByNumber.and.returnValue(of({ id: 99 }));
    rewardServiceSpy.initializeRewardAccount.and.returnValue(throwError(() => new Error('Init error')));

    fixture.detectChanges();

    expect(component.loading).toBeFalse();
    expect(component.errorMsg).toBe('Failed to load or initialize your reward wallet.');
  });

  it('should handle reward history fetch error', () => {
    authServiceSpy.getCurrentUser.and.returnValue({ name: 'Bob', email: 'bob@company.com' });
    accountSetupServiceSpy.getAccountByUser.and.returnValue(of({ accountNumber: '111122223333' }));
    accountSetupServiceSpy.getAccountByNumber.and.returnValue(of({ id: 99 }));
    rewardServiceSpy.initializeRewardAccount.and.returnValue(of({
      rewardAccountId: 1,
      accountId: 99,
      totalPoints: 10,
      createdAt: '2026-01-01',
      updatedAt: '2026-01-01'
    }));
    rewardServiceSpy.getRewardHistory.and.returnValue(throwError(() => new Error('History error')));

    fixture.detectChanges();

    expect(component.loading).toBeFalse();
    expect(component.points).toBe(10);
    expect(component.tier).toBe('Bronze');
  });

  it('should calculate tiers correctly', () => {
    expect(component.calculateTier(150)).toBe('Platinum');
    expect(component.calculateTier(100)).toBe('Platinum');
    expect(component.calculateTier(75)).toBe('Gold');
    expect(component.calculateTier(50)).toBe('Gold');
    expect(component.calculateTier(30)).toBe('Silver');
    expect(component.calculateTier(20)).toBe('Silver');
    expect(component.calculateTier(10)).toBe('Bronze');
  });

  it('should return tier colors correctly', () => {
    expect(component.getTierColor('Platinum')).toBe('#e5e5e5');
    expect(component.getTierColor('Gold')).toBe('#ffd700');
    expect(component.getTierColor('Silver')).toBe('#c0c0c0');
    expect(component.getTierColor('Bronze')).toBe('#cd7f32');
    expect(component.getTierColor('Unknown')).toBe('#cd7f32');
  });

  it('should logout successfully', () => {
    component.onLogout();
    expect(authServiceSpy.clearSession).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/']);
  });
});
