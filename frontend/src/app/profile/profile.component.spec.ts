import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ProfileComponent } from './profile.component';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { AuthService } from '../services/auth.service';
import { AccountSetupService } from '../services/account-setup.service';
import { of } from 'rxjs';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';
import { vi, describe, it, expect, beforeEach } from 'vitest';

describe('ProfileComponent', () => {
    let component: ProfileComponent;
    let fixture: ComponentFixture<ProfileComponent>;
    let authService: AuthService;
    let accountSetupService: AccountSetupService;

    beforeEach(async () => {
        // Mock localStorage
        const localStorageMock = (() => {
            let store: any = {};
            return {
                getItem: (key: string) => store[key] || null,
                setItem: (key: string, value: string) => store[key] = value,
                removeItem: (key: string) => delete store[key],
                clear: () => store = {}
            };
        })();
        Object.defineProperty(window, 'localStorage', { value: localStorageMock });

        await TestBed.configureTestingModule({
            imports: [
                ProfileComponent,
                HttpClientTestingModule,
                RouterTestingModule,
                NoopAnimationsModule,
                ReactiveFormsModule,
                FormsModule
            ],
            providers: [AuthService, AccountSetupService]
        }).compileComponents();

        fixture = TestBed.createComponent(ProfileComponent);
        component = fixture.componentInstance;
        authService = TestBed.inject(AuthService);
        accountSetupService = TestBed.inject(AccountSetupService);

        // Add default mocks to prevent subscription errors
        vi.spyOn(authService, 'getCurrentUser').mockReturnValue({ name: 'testuser' });
        vi.spyOn(accountSetupService, 'getAccountByUser').mockReturnValue(of({ holderName: 'testuser', createdAt: new Date().toISOString() }));
        vi.spyOn(accountSetupService, 'updateAccount').mockReturnValue(of({ name: 'updated' }));
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should load profile data on init', () => {
        vi.spyOn(authService, 'getCurrentUser').mockReturnValue({ name: 'testuser' });
        vi.spyOn(accountSetupService, 'getAccountByUser').mockReturnValue(of({ holderName: 'testuser', upiId: 'test@upi' }));

        component.ngOnInit();

        expect(component.profileData.name).toBe('testuser');
    });

    it('should update profile successfully', () => {
        fixture.detectChanges();
        vi.spyOn(authService, 'getCurrentUser').mockReturnValue({ name: 'testuser' });
        vi.spyOn(accountSetupService, 'updateAccount').mockReturnValue(of({ name: 'updated' }));

        component.profileForm.patchValue({
            email: 'test@example.com',
            phoneNumber: '1234567890',
            bankName: 'Test Bank',
            branchName: 'Test Branch',
            address: 'Test Address',
            ifscCode: 'TEST001'
        });

        component.onSave();

        expect(accountSetupService.updateAccount).toHaveBeenCalled();
    });
});
