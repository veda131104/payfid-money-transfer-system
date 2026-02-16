import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DashboardComponent } from './dashboard.component';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { AuthService } from '../services/auth.service';
import { AccountSetupService } from '../services/account-setup.service';
import { TransactionService } from '../services/transaction.service';
import { of } from 'rxjs';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { vi, describe, it, expect, beforeEach } from 'vitest';

describe('DashboardComponent', () => {
    let component: DashboardComponent;
    let fixture: ComponentFixture<DashboardComponent>;
    let authService: AuthService;
    let accountSetupService: AccountSetupService;
    let transactionService: TransactionService;

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

        // Mock Canvas (if needed for child components)
        Object.defineProperty(HTMLCanvasElement.prototype, 'getContext', {
            value: vi.fn(() => ({
                clearRect: vi.fn(),
                fillRect: vi.fn(),
                getImageData: vi.fn(),
                putImageData: vi.fn(),
                createImageData: vi.fn(),
                setTransform: vi.fn(),
                drawImage: vi.fn(),
                save: vi.fn(),
                restore: vi.fn(),
                beginPath: vi.fn(),
                moveTo: vi.fn(),
                lineTo: vi.fn(),
                closePath: vi.fn(),
                stroke: vi.fn(),
                translate: vi.fn(),
                scale: vi.fn(),
                rotate: vi.fn(),
                arc: vi.fn(),
                fill: vi.fn(),
                measureText: vi.fn(() => ({ width: 0 })),
                transform: vi.fn(),
                rect: vi.fn(),
                clip: vi.fn(),
            }))
        });

        await TestBed.configureTestingModule({
            imports: [
                DashboardComponent,
                HttpClientTestingModule,
                RouterTestingModule,
                NoopAnimationsModule
            ],
            providers: [AuthService, AccountSetupService, TransactionService]
        }).compileComponents();

        fixture = TestBed.createComponent(DashboardComponent);
        component = fixture.componentInstance;
        authService = TestBed.inject(AuthService);
        accountSetupService = TestBed.inject(AccountSetupService);
        transactionService = TestBed.inject(TransactionService);

        // Add default mocks
        vi.spyOn(authService, 'getCurrentUser').mockReturnValue({ name: 'testuser' });
        vi.spyOn(accountSetupService, 'getAccountByUser').mockReturnValue(of({ holderName: 'testuser' }));
        vi.spyOn(transactionService, 'getAccountHistory').mockReturnValue(of([]));
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should load account data on init', () => {
        vi.spyOn(authService, 'getCurrentUser').mockReturnValue({ name: 'testuser' });
        vi.spyOn(accountSetupService, 'getAccountByUser').mockReturnValue(of({ accountNumber: '123456789012', balance: 1000 }));
        vi.spyOn(accountSetupService, 'getAccountByNumber').mockReturnValue(of({ accountNumber: '123456789012', balance: 1000, id: 1 }));

        component.ngOnInit();

        expect(accountSetupService.getAccountByUser).toHaveBeenCalledWith('testuser');
        expect(component.currentUserAccountNumber).toBe('123456789012');
    });
});
