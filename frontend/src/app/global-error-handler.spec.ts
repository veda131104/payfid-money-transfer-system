import { TestBed } from '@angular/core/testing';
import { GlobalErrorHandler } from './global-error-handler';

describe('GlobalErrorHandler', () => {
  let errorHandler: GlobalErrorHandler;
  let consoleWarnSpy: jasmine.Spy;
  let consoleErrorSpy: jasmine.Spy;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [GlobalErrorHandler]
    });
    errorHandler = TestBed.inject(GlobalErrorHandler);
    consoleWarnSpy = spyOn(console, 'warn');
    consoleErrorSpy = spyOn(console, 'error');
  });

  it('should be created', () => {
    expect(errorHandler).toBeTruthy();
  });

  it('should ignore telemetry error (sendUsage) and log as warning', () => {
    const err = new Error('Something about sendUsage here');
    errorHandler.handleError(err);
    expect(consoleWarnSpy).toHaveBeenCalledWith('Ignored telemetry/external error:', err);
    expect(consoleErrorSpy).not.toHaveBeenCalled();
  });

  it('should ignore telemetry error (stopTracking) and log as warning', () => {
    const err = new Error('Call stopTracking please');
    errorHandler.handleError(err);
    expect(consoleWarnSpy).toHaveBeenCalledWith('Ignored telemetry/external error:', err);
    expect(consoleErrorSpy).not.toHaveBeenCalled();
  });

  it('should ignore telemetry error (cert_Authority) and log as warning', () => {
    const err = { message: 'cert_Authority failure' };
    errorHandler.handleError(err);
    expect(consoleWarnSpy).toHaveBeenCalledWith('Ignored telemetry/external error:', err);
    expect(consoleErrorSpy).not.toHaveBeenCalled();
  });

  it('should ignore telemetry error (CERT_AUTHORITY_INVALID) and log as warning', () => {
    const err = 'CERT_AUTHORITY_INVALID has occurred';
    errorHandler.handleError(err);
    expect(consoleWarnSpy).toHaveBeenCalledWith('Ignored telemetry/external error:', err);
    expect(consoleErrorSpy).not.toHaveBeenCalled();
  });

  it('should ignore telemetry error (Failed to fetch with usage) and log as warning', () => {
    const err = new Error('Failed to fetch tracking usage data');
    errorHandler.handleError(err);
    expect(consoleWarnSpy).toHaveBeenCalledWith('Ignored telemetry/external error:', err);
    expect(consoleErrorSpy).not.toHaveBeenCalled();
  });

  it('should log actual application error as error', () => {
    const err = new Error('Null reference exception in dashboard');
    errorHandler.handleError(err);
    expect(consoleErrorSpy).toHaveBeenCalledWith('Unhandled Application Error:', err);
    expect(consoleWarnSpy).not.toHaveBeenCalled();
  });
});
