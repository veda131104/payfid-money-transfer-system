import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AnalyticsComponent } from './analytics.component';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { AnalyticsService } from '../services/analytics.service';
import { of } from 'rxjs';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { vi, describe, it, expect, beforeEach } from 'vitest';

describe('AnalyticsComponent', () => {
    let component: AnalyticsComponent;
    let fixture: ComponentFixture<AnalyticsComponent>;
    let analyticsService: AnalyticsService;

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
        (window.HTMLCanvasElement.prototype as any).getContext = vi.fn().mockReturnValue({
            fillRect: vi.fn(),
            clearRect: vi.fn(),
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
            measureText: vi.fn().mockReturnValue({ width: 0 }),
            transform: vi.fn(),
            rect: vi.fn(),
            clip: vi.fn(),
        });

        await TestBed.configureTestingModule({
            imports: [
                AnalyticsComponent,
                HttpClientTestingModule,
                RouterTestingModule,
                NoopAnimationsModule
            ],
            providers: [AnalyticsService]
        }).compileComponents();

        fixture = TestBed.createComponent(AnalyticsComponent);
        component = fixture.componentInstance;
        analyticsService = TestBed.inject(AnalyticsService);
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should load all analytics data on init', () => {
        vi.spyOn(analyticsService, 'getTransactionVolume').mockReturnValue(of([]));
        vi.spyOn(analyticsService, 'getAccountActivity').mockReturnValue(of([]));
        vi.spyOn(analyticsService, 'getSuccessRate').mockReturnValue(of({ success: 0, failed: 0, pending: 0 }));
        vi.spyOn(analyticsService, 'getPeakHours').mockReturnValue(of([]));

        component.ngOnInit();

        expect(analyticsService.getTransactionVolume).toHaveBeenCalled();
        expect(analyticsService.getAccountActivity).toHaveBeenCalled();
        expect(analyticsService.getSuccessRate).toHaveBeenCalled();
        expect(analyticsService.getPeakHours).toHaveBeenCalled();
    });

    it('should handle logout correctly', () => {
        const routerSpy = vi.spyOn((component as any).router, 'navigate');
        component.onLogout();
        expect(routerSpy).toHaveBeenCalledWith(['/']);
    });
});
