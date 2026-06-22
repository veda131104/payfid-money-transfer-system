import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AnalyticsComponent } from './analytics.component';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { AnalyticsService } from '../services/analytics.service';
import { AuthService } from '../services/auth.service';
import { of, throwError } from 'rxjs';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { PLATFORM_ID } from '@angular/core';

describe('AnalyticsComponent', () => {
    let component: AnalyticsComponent;
    let fixture: ComponentFixture<AnalyticsComponent>;
    let analyticsService: AnalyticsService;
    let authService: AuthService;

    function setupTestBed(platformId: string = 'browser'): Promise<void> {
        return TestBed.configureTestingModule({
            imports: [
                AnalyticsComponent,
                HttpClientTestingModule,
                RouterTestingModule,
                NoopAnimationsModule
            ],
            providers: [
                AnalyticsService,
                AuthService,
                { provide: PLATFORM_ID, useValue: platformId }
            ]
        }).compileComponents();
    }

    beforeEach(async () => {
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

        await setupTestBed('browser');

        fixture = TestBed.createComponent(AnalyticsComponent);
        component = fixture.componentInstance;
        analyticsService = TestBed.inject(AnalyticsService);
        authService = TestBed.inject(AuthService);
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should load all analytics data on init when user is logged in', () => {
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

    it('should not load analytics when user is null', () => {
        spyOn(authService, 'getCurrentUser').and.returnValue(null);
        spyOn(analyticsService, 'getTransactionVolume');
        component.ngOnInit();
        expect(analyticsService.getTransactionVolume).not.toHaveBeenCalled();
    });

    it('should handle errors in analytics API calls gracefully', () => {
        spyOn(authService, 'getCurrentUser').and.returnValue({ name: 'testuser', email: 'testuser@company.com' });
        spyOn(analyticsService, 'getTransactionVolume').and.returnValue(throwError(() => new Error('Volume error')));
        spyOn(analyticsService, 'getAccountActivity').and.returnValue(throwError(() => new Error('Activity error')));
        spyOn(analyticsService, 'getSuccessRate').and.returnValue(throwError(() => new Error('Success error')));
        spyOn(analyticsService, 'getPeakHours').and.returnValue(throwError(() => new Error('Peak error')));
        spyOn(console, 'error');

        component.ngOnInit();

        expect(console.error).toHaveBeenCalledTimes(4);
    });

    it('should handle logout correctly', () => {
        spyOn(authService, 'clearSession');
        const routerSpy = spyOn((component as any).router, 'navigate');
        component.onLogout();
        expect(routerSpy).toHaveBeenCalledWith(['/']);
        expect(authService.clearSession).toHaveBeenCalled();
    });

    it('should not initialize when running on server (isPlatformBrowser=false)', async () => {
        TestBed.resetTestingModule();
        await setupTestBed('server');

        fixture = TestBed.createComponent(AnalyticsComponent);
        component = fixture.componentInstance;
        analyticsService = TestBed.inject(AnalyticsService);
        authService = TestBed.inject(AuthService);

        spyOn(authService, 'getCurrentUser');
        component.ngOnInit();
        expect(authService.getCurrentUser).not.toHaveBeenCalled();
    });

    it('should handle createVolumeChart when canvas element not found', () => {
        // Test with no canvas element in DOM - should return early
        spyOn(document, 'getElementById').and.returnValue(null);
        expect(() => component.createVolumeChart([])).not.toThrow();
    });

    it('should handle createActivityChart when canvas element not found', () => {
        spyOn(document, 'getElementById').and.returnValue(null);
        expect(() => component.createActivityChart([])).not.toThrow();
    });

    it('should handle createSuccessChart when canvas element not found', () => {
        spyOn(document, 'getElementById').and.returnValue(null);
        expect(() => component.createSuccessChart({ success: 0, failed: 0, pending: 0 })).not.toThrow();
    });

    it('should handle createPeakChart when canvas element not found', () => {
        spyOn(document, 'getElementById').and.returnValue(null);
        expect(() => component.createPeakChart([])).not.toThrow();
    });
});
