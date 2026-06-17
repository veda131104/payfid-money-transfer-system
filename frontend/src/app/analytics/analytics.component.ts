import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { AuthService } from '../services/auth.service';
import { AnalyticsService } from '../services/analytics.service';
import { Chart, registerables } from 'chart.js';

Chart.register(...registerables);

@Component({
    selector: 'app-analytics',
    standalone: true,
    imports: [CommonModule, MatButtonModule, MatCardModule, RouterLink, RouterLinkActive],
    templateUrl: './analytics.component.html',
    styleUrl: './analytics.component.scss'
})
export class AnalyticsComponent implements OnInit {
    private authService = inject(AuthService);
    private router = inject(Router);
    private analyticsService = inject(AnalyticsService);

    userName = 'User';
    routerLinkActiveOptions = { exact: true };

    // Chart instances
    volumeChart: any;
    activityChart: any;
    successChart: any;
    peakChart: any;

    ngOnInit(): void {
        const user = this.authService.getCurrentUser();
        if (user) {
            this.userName = user.name;
        }
        this.loadAllAnalytics();
    }

    loadAllAnalytics(): void {
        const name = this.userName;
        // 1. Transaction Volume
        this.analyticsService.getTransactionVolume(name).subscribe({
            next: (data) => this.createVolumeChart(data),
            error: (err) => console.error('Error loading volume:', err)
        });

        // 2. Account Activity
        this.analyticsService.getAccountActivity(name).subscribe({
            next: (data) => this.createActivityChart(data),
            error: (err) => console.error('Error loading activity:', err)
        });

        // 3. Success Rate
        this.analyticsService.getSuccessRate(name).subscribe({
            next: (data) => this.createSuccessChart(data),
            error: (err) => console.error('Error loading success rate:', err)
        });

        // 4. Peak Hours
        this.analyticsService.getPeakHours(name).subscribe({
            next: (data) => this.createPeakChart(data),
            error: (err) => console.error('Error loading peak hours:', err)
        });
    }

    createVolumeChart(data: any[]): void {
        const ctx = document.getElementById('volumeChart') as HTMLCanvasElement;
        if (!ctx) return;
        this.volumeChart = new Chart(ctx, {
            type: 'line',
            data: {
                labels: data.map(d => d.date),
                datasets: [{
                    label: 'Transaction Volume ($)',
                    data: data.map(d => d.value),
                    borderColor: '#4285F4',
                    backgroundColor: 'rgba(66, 133, 244, 0.1)',
                    fill: true,
                    tension: 0.4
                }]
            },
            options: { responsive: true }
        });
    }

    createActivityChart(data: any[]): void {
        const ctx = document.getElementById('activityChart') as HTMLCanvasElement;
        if (!ctx) return;
        this.activityChart = new Chart(ctx, {
            type: 'bar',
            data: {
                labels: data.map(d => d.name),
                datasets: [{
                    label: 'Transaction Count',
                    data: data.map(d => d.count),
                    backgroundColor: '#34A853',
                    borderRadius: 8
                }]
            },
            options: {
                indexAxis: 'y',
                responsive: true
            }
        });
    }

    createSuccessChart(data: any): void {
        const ctx = document.getElementById('successChart') as HTMLCanvasElement;
        if (!ctx) return;
        this.successChart = new Chart(ctx, {
            type: 'doughnut',
            data: {
                labels: ['Success', 'Failed', 'Pending'],
                datasets: [{
                    data: [data.success, data.failed, data.pending],
                    backgroundColor: ['#34A853', '#EA4335', '#FBBC05'],
                    borderWidth: 0
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: 'bottom',
                        labels: {
                            padding: 20,
                            font: { size: 12 }
                        }
                    }
                },
                layout: {
                    padding: 10
                }
            }
        });
    }

    createPeakChart(data: any[]): void {
        const ctx = document.getElementById('peakChart') as HTMLCanvasElement;
        if (!ctx) return;
        this.peakChart = new Chart(ctx, {
            type: 'bar',
            data: {
                labels: data.map(d => d.hour),
                datasets: [{
                    label: 'Volume by Hour',
                    data: data.map(d => d.count),
                    backgroundColor: '#FBBC05',
                    borderRadius: 4
                }]
            },
            options: { responsive: true }
        });
    }

    onLogout(): void {
        this.authService.clearSession();
        this.router.navigate(['/']);
    }
}
