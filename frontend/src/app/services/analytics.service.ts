import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

@Injectable({
    providedIn: 'root'
})
export class AnalyticsService {
    private readonly baseUrl = 'http://localhost:8080/api/v1/analytics';

    constructor(private readonly http: HttpClient) { }

    getTransactionVolume(name?: string): Observable<any[]> {
        const url = name ? `${this.baseUrl}/transaction-volume?name=${name}` : `${this.baseUrl}/transaction-volume`;
        return this.http.get<any[]>(url);
    }

    getAccountActivity(name?: string): Observable<any[]> {
        const url = name ? `${this.baseUrl}/account-activity?name=${name}` : `${this.baseUrl}/account-activity`;
        return this.http.get<any[]>(url);
    }

    getSuccessRate(name?: string): Observable<any> {
        const url = name ? `${this.baseUrl}/success-rate?name=${name}` : `${this.baseUrl}/success-rate`;
        return this.http.get<any>(url);
    }

    getPeakHours(name?: string): Observable<any[]> {
        const url = name ? `${this.baseUrl}/peak-hours?name=${name}` : `${this.baseUrl}/peak-hours`;
        return this.http.get<any[]>(url);
    }
}
