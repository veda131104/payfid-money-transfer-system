import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ProfileComponent } from './profile.component';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { AuthService } from '../services/auth.service';
import { AccountSetupService } from '../services/account-setup.service';
import { of, throwError } from 'rxjs';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

describe('ProfileComponent', () => {
    let component: ProfileComponent;
    let fixture: ComponentFixture<ProfileComponent>;
    let authService: AuthService;
    let accountSetupService: AccountSetupService;
    let router: Router;
    let getCurrentUserSpy: jasmine.Spy;
    let getAccountByUserSpy: jasmine.Spy;
    let updateAccountSpy: jasmine.Spy;
    let alertSpy: jasmine.Spy;

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
        alertSpy = spyOn(window, 'alert');

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
        router = TestBed.inject(Router);
        spyOn(router, 'navigate');

        // Add default mocks to prevent subscription errors
        getCurrentUserSpy = spyOn(authService, 'getCurrentUser').and.returnValue({ name: 'testuser', email: 'testuser@company.com' });
        getAccountByUserSpy = spyOn(accountSetupService, 'getAccountByUser').and.returnValue(of({ holderName: 'testuser', createdAt: new Date().toISOString() }));
        updateAccountSpy = spyOn(accountSetupService, 'updateAccount').and.returnValue(of({ name: 'updated' }));

        fixture.detectChanges();
    });

    afterEach(() => {
        TestBed.resetTestingModule();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should redirect to root if user is not logged in on init', () => {
        getCurrentUserSpy.and.returnValue(null);
        component.ngOnInit();
        expect(router.navigate).toHaveBeenCalledWith(['/']);
    });

    it('should load profile data on init', () => {
        getCurrentUserSpy.and.returnValue({ name: 'testuser', email: 'testuser@company.com' });
        getAccountByUserSpy.and.returnValue(of({ holderName: 'testuser', upiId: 'test@upi', createdAt: '2026-01-10T12:00:00Z', accountNumber: '123' }));
        spyOn(accountSetupService, 'getAccountByNumber').and.returnValue(of({ balance: 250 }));

        component.ngOnInit();

        expect(component.profileData.name).toBe('testuser');
        expect(component.profileData.joinDate).toContain('January');
        expect(component.currentBalance).toBe(250);
    });

    it('should handle getAccountByNumber error during init', () => {
        getCurrentUserSpy.and.returnValue({ name: 'testuser', email: 'testuser@company.com' });
        getAccountByUserSpy.and.returnValue(of({ holderName: 'testuser', accountNumber: '123' }));
        spyOn(accountSetupService, 'getAccountByNumber').and.returnValue(throwError(() => new Error('Balance error')));

        component.ngOnInit();

        expect(component.currentBalance).toBe(0);
    });

    it('should handle getAccountByUser error on init', () => {
        getCurrentUserSpy.and.returnValue({ name: 'testuser', email: 'testuser@company.com' });
        getAccountByUserSpy.and.returnValue(throwError(() => new Error('Not setup yet')));

        component.ngOnInit();

        expect(component.isNewUser).toBeTrue();
        expect(component.profileData.accountStatus).toBe('Pending Setup');
    });

    it('should clear session and redirect to root on logout', () => {
        spyOn(authService, 'clearSession');
        component.onLogout();
        expect(authService.clearSession).toHaveBeenCalled();
        expect(router.navigate).toHaveBeenCalledWith(['/']);
    });

    it('should redirect to account-setup on edit profile if user is new', () => {
        component.isNewUser = true;
        component.onEditProfile();
        expect(router.navigate).toHaveBeenCalledWith(['/account-setup']);
    });

    it('should toggle editing state on edit profile if user is not new', () => {
        component.isNewUser = false;
        component.onEditProfile();
        expect(component.isEditing).toBeTrue();
    });

    it('should reset isEditing and patch value on cancel edit', () => {
        component.isEditing = true;
        component.profileData = { ...component.profileData, email: 'original@mail.com' } as any;
        component.profileForm.patchValue({ email: 'changed@mail.com' });
        component.onCancel();
        expect(component.isEditing).toBeFalse();
        expect(component.profileForm.get('email')?.value).toBe('original@mail.com');
    });

    it('should update profile successfully', () => {
        fixture.detectChanges();
        getCurrentUserSpy.and.returnValue({ name: 'testuser', email: 'testuser@company.com' });
        updateAccountSpy.and.returnValue(of({ name: 'updated', createdAt: '2026-02-20T10:00:00Z' }));

        component.profileForm.patchValue({
            email: 'test@example.com',
            phoneNumber: '1234567890',
            bankName: 'Test Bank',
            branchName: 'Test Branch',
            address: 'Test Address',
            ifscCode: 'TEST001',
            creditCardNumber: '1111222233334444',
            cvv: '123',
            expiryDate: '12/28'
        });

        component.onSave();

        expect(accountSetupService.updateAccount).toHaveBeenCalled();
        expect(component.isEditing).toBeFalse();
        expect(alertSpy).toHaveBeenCalledWith('Profile updated successfully!');
    });

    it('should return immediately if form is invalid or user missing on save', () => {
        fixture.detectChanges();
        component.profileForm.patchValue({ email: 'invalid-email' });
        component.onSave();
        expect(accountSetupService.updateAccount).not.toHaveBeenCalled();

        component.profileForm.patchValue({ email: 'test@test.com' }); // make form valid
        getCurrentUserSpy.and.returnValue(null);
        component.onSave();
        expect(accountSetupService.updateAccount).not.toHaveBeenCalled();
    });

    it('should alert error if update profile API fails', () => {
        fixture.detectChanges();
        updateAccountSpy.and.returnValue(throwError(() => new Error('Save error')));
        component.profileForm.patchValue({
            email: 'test@example.com',
            phoneNumber: '1234567890',
            bankName: 'Test Bank',
            branchName: 'Test Branch',
            address: 'Test Address',
            ifscCode: 'TEST001',
            creditCardNumber: '1111222233334444',
            cvv: '123',
            expiryDate: '12/28'
        });

        component.onSave();
        expect(alertSpy).toHaveBeenCalledWith('Update failed: Save error');
    });

    it('should return initials correctly', () => {
        component.profileData.name = 'John Doe';
        expect(component.getInitials()).toBe('JD');

        component.profileData.name = 'Single';
        expect(component.getInitials()).toBe('S');

        component.profileData.name = '';
        expect(component.getInitials()).toBe('U');
    });

    it('should open and close PIN modal', () => {
        component.onOpenPinModal();
        expect(component.isPinModalOpen).toBeTrue();
        expect(component.pinForm.get('pin')?.value).toBe('');

        component.onClosePinModal();
        expect(component.isPinModalOpen).toBeFalse();
    });

    it('should validate and save PIN successfully', () => {
        component.onOpenPinModal();
        component.pinForm.patchValue({ pin: '1234' });
        spyOn(accountSetupService, 'setPin').and.returnValue(of({}));

        component.onSavePin();

        expect(accountSetupService.setPin).toHaveBeenCalledWith('testuser', '1234');
        expect(alertSpy).toHaveBeenCalledWith('Success! Your PIN has been set.');
        expect(component.isPinModalOpen).toBeFalse();
    });

    it('should handle PIN form validation errors', () => {
        component.onOpenPinModal();
        component.pinForm.patchValue({ pin: '' });
        component.onSavePin();
        expect(alertSpy).toHaveBeenCalledWith('Validation Error: PIN is required');

        component.pinForm.patchValue({ pin: '12' });
        component.onSavePin();
        expect(alertSpy).toHaveBeenCalledWith('Validation Error: PIN must be exactly 4 digits');
    });

    it('should handle missing session on save PIN', () => {
        component.onOpenPinModal();
        component.pinForm.patchValue({ pin: '1234' });
        getCurrentUserSpy.and.returnValue(null);

        component.onSavePin();
        expect(alertSpy).toHaveBeenCalledWith('User session not found. Please log in again.');
    });

    it('should handle PIN save bank details error and generic error', () => {
        component.onOpenPinModal();
        component.pinForm.patchValue({ pin: '1234' });
        const setPinSpy = spyOn(accountSetupService, 'setPin');

        setPinSpy.and.returnValue(throwError(() => new Error('Bank details not found')));
        component.onSavePin();
        expect(alertSpy).toHaveBeenCalledWith('Error: Bank details not found. Please complete Account Setup first.');

        setPinSpy.and.returnValue(throwError(() => new Error('Internal error')));
        component.onSavePin();
        expect(alertSpy).toHaveBeenCalledWith('Error: Failed to set PIN: Internal error');
    });
});

