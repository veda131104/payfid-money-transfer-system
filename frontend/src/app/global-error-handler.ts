import { ErrorHandler, Injectable } from '@angular/core';

@Injectable()
export class GlobalErrorHandler implements ErrorHandler {
  handleError(error: any): void {
    const errorStr = String(error?.message || error?.stack || error);
    
    // Ignore external telemetry/tracking/Chrome-extension errors
    if (
      errorStr.includes('sendUsage') ||
      errorStr.includes('stopTracking') ||
      errorStr.includes('cert_Authority') ||
      errorStr.includes('CERT_AUTHORITY_INVALID') ||
      (errorStr.includes('Failed to fetch') && (
        errorStr.includes('usage') || 
        errorStr.includes('tracking') || 
        errorStr.includes('analytics')
      ))
    ) {
      // Log as a warning rather than throwing/blocking
      console.warn('Ignored telemetry/external error:', error);
      return;
    }
    
    // Log actual application errors
    console.error('Unhandled Application Error:', error);
  }
}
