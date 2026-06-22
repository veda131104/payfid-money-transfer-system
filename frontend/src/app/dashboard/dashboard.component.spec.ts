import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DashboardComponent } from './dashboard.component';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { AuthService } from '../services/auth.service';
import { AccountSetupService } from '../services/account-setup.service';
import { TransactionService } from '../services/transaction.service';
import { of } from 'rxjs';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

describe('DashboardComponent', () => {
    let component: DashboardComponent;
    let fixture: ComponentFixture<DashboardComponent>;
    let authService: AuthService;
    let accountSetupService: AccountSetupService;
    let transactionService: TransactionService;
    let getCurrentUserSpy: jasmine.Spy;
    let getAccountByUserSpy: jasmine.Spy;
    let getAccountHistorySpy: jasmine.Spy;

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
        const dummyCtx = {
            clearRect: jasmine.createSpy('clearRect'),
            fillRect: jasmine.createSpy('fillRect'),
            getImageData: jasmine.createSpy('getImageData'),
            putImageData: jasmine.createSpy('putImageData'),
            createImageData: jasmine.createSpy('createImageData'),
            setTransform: jasmine.createSpy('setTransform'),
            drawImage: jasmine.createSpy('drawImage'),
            save: jasmine.createSpy('save'),
            restore: jasmine.createSpy('restore'),
            beginPath: jasmine.createSpy('beginPath'),
            moveTo: jasmine.createSpy('moveTo'),
            lineTo: jasmine.createSpy('lineTo'),
            closePath: jasmine.createSpy('closePath'),
            stroke: jasmine.createSpy('stroke'),
            translate: jasmine.createSpy('translate'),
            scale: jasmine.createSpy('scale'),
            rotate: jasmine.createSpy('rotate'),
            arc: jasmine.createSpy('arc'),
            fill: jasmine.createSpy('fill'),
            measureText: jasmine.createSpy('measureText').and.returnValue({ width: 0 }),
            transform: jasmine.createSpy('transform'),
            rect: jasmine.createSpy('rect'),
            clip: jasmine.createSpy('clip'),
        };
        Object.defineProperty(HTMLCanvasElement.prototype, 'getContext', {
            value: jasmine.createSpy('getContext').and.returnValue(dummyCtx)
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
        getCurrentUserSpy = spyOn(authService, 'getCurrentUser').and.returnValue({ name: 'testuser', email: 'testuser@company.com' });
        getAccountByUserSpy = spyOn(accountSetupService, 'getAccountByUser').and.returnValue(of({ holderName: 'testuser' }));
        getAccountHistorySpy = spyOn(transactionService, 'getAccountHistory').and.returnValue(of([]));
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should load account data on init', () => {
        getCurrentUserSpy.and.returnValue({ name: 'testuser', email: 'testuser@company.com' });
        getAccountByUserSpy.and.returnValue(of({ accountNumber: '123456789012', balance: 1000 }));
        spyOn(accountSetupService, 'getAccountByNumber').and.returnValue(of({ accountNumber: '123456789012', balance: 1000, id: 1 }));

        component.ngOnInit();

        expect(accountSetupService.getAccountByUser).toHaveBeenCalledWith('testuser');
        expect(accountSetupService.getAccountByNumber).toHaveBeenCalledWith('123456789012');
        expect(component.balance).toBe(1000);
    });
});
