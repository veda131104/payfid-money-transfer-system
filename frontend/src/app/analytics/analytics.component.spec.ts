import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AnalyticsComponent } from './analytics.component';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { AnalyticsService } from '../services/analytics.service';
import { AuthService } from '../services/auth.service';
import { of } from 'rxjs';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

describe('AnalyticsComponent', () => {
    let component: AnalyticsComponent;
    let fixture: ComponentFixture<AnalyticsComponent>;
    let analyticsService: AnalyticsService;
    let authService: AuthService;

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

        // Mock Canvas getContext to avoid Chart.js errors
        const dummyCtx = {
            fillRect: jasmine.createSpy('fillRect'),
            clearRect: jasmine.createSpy('clearRect'),
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
        (window.HTMLCanvasElement.prototype as any).getContext = jasmine.createSpy('getContext').and.returnValue(dummyCtx);

        await TestBed.configureTestingModule({
            imports: [
                AnalyticsComponent,
                HttpClientTestingModule,
                RouterTestingModule,
                NoopAnimationsModule
            ],
            providers: [AnalyticsService, AuthService]
        }).compileComponents();

        fixture = TestBed.createComponent(AnalyticsComponent);
        component = fixture.componentInstance;
        analyticsService = TestBed.inject(AnalyticsService);
        authService = TestBed.inject(AuthService);
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should load all analytics data on init', () => {
        spyOn(authService, 'getCurrentUser').and.returnValue({ name: 'testuser', email: 'testuser@company.com' });
        spyOn(analyticsService, 'getTransactionVolume').and.returnValue(of([]));
        spyOn(analyticsService, 'getAccountActivity').and.returnValue(of([]));
        spyOn(analyticsService, 'getSuccessRate').and.returnValue(of({ success: 0, failed: 0, pending: 0 }));
        spyOn(analyticsService, 'getPeakHours').and.returnValue(of([]));

        component.ngOnInit();

        expect(analyticsService.getTransactionVolume).toHaveBeenCalled();
        expect(analyticsService.getAccountActivity).toHaveBeenCalled();
        expect(analyticsService.getSuccessRate).toHaveBeenCalled();
        expect(analyticsService.getPeakHours).toHaveBeenCalled();
    });

    it('should handle logout correctly', () => {
        spyOn(authService, 'clearSession');
        const routerSpy = spyOn((component as any).router, 'navigate');
        component.onLogout();
        expect(routerSpy).toHaveBeenCalledWith(['/']);
        expect(authService.clearSession).toHaveBeenCalled();
    });
});
