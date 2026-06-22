import { TestBed, ComponentFixture, fakeAsync, tick } from '@angular/core/testing';
import { App } from './app';
import { AuthService } from './services/auth.service';
import { Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';

describe('App', () => {
  let component: App;
  let fixture: ComponentFixture<App>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let router: Router;
  let broadcastChannelMock: any;

  beforeEach(async () => {
    authServiceSpy = jasmine.createSpyObj('AuthService', ['clearSession']);
    
    // Mock BroadcastChannel
    broadcastChannelMock = {
      postMessage: jasmine.createSpy('postMessage'),
      close: jasmine.createSpy('close'),
      onmessage: null
    };
    (window as any).BroadcastChannel = jasmine.createSpy('BroadcastChannel').and.returnValue(broadcastChannelMock);

    // Mock sessionStorage
    const sessionStore: any = {};
    spyOn(sessionStorage, 'getItem').and.callFake((key) => sessionStore[key] || null);
    spyOn(sessionStorage, 'setItem').and.callFake((key, val) => sessionStore[key] = val);

    // Mock localStorage
    const localStore: any = {};
    spyOn(localStorage, 'getItem').and.callFake((key) => localStore[key] || null);
    spyOn(localStorage, 'setItem').and.callFake((key, val) => localStore[key] = val);

    await TestBed.configureTestingModule({
      imports: [App, RouterTestingModule],
      providers: [
        { provide: AuthService, useValue: authServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(App);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    spyOn(router, 'navigate');
  });

  afterEach(() => {
    fixture.destroy();
  });

  it('should create the app', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
    expect(component['title']()).toEqual('frontend');
    expect(component['sessionExpired']()).toBeFalse();
  });

  it('should claim tab session ID and set active session in localStorage on init', () => {
    fixture.detectChanges();
    expect(sessionStorage.setItem).toHaveBeenCalledWith('app_tab_session_id', jasmine.any(String));
    expect(localStorage.setItem).toHaveBeenCalledWith('app_active_session_id', jasmine.any(String));
    expect(broadcastChannelMock.postMessage).toHaveBeenCalledWith(jasmine.objectContaining({
      type: 'ACTIVE_SESSION_CHANGED'
    }));
  });

  it('should handle session change broadcast message and expire session', fakeAsync(() => {
    fixture.detectChanges();
    
    // Trigger broadcast message with a different session ID
    if (broadcastChannelMock.onmessage) {
      broadcastChannelMock.onmessage({
        data: {
          type: 'ACTIVE_SESSION_CHANGED',
          activeSessionId: 'different-session-id'
        }
      } as MessageEvent);
    }

    expect(component['sessionExpired']()).toBeTrue();
    expect(authServiceSpy.clearSession).toHaveBeenCalled();

    tick(5000);
    expect(router.navigate).toHaveBeenCalledWith(['/']);
  }));

  it('should ignore broadcast message with same session ID', () => {
    fixture.detectChanges();
    const mySessionId = component['tabSessionId'];
    
    if (broadcastChannelMock.onmessage) {
      broadcastChannelMock.onmessage({
        data: {
          type: 'ACTIVE_SESSION_CHANGED',
          activeSessionId: mySessionId
        }
      } as MessageEvent);
    }

    expect(component['sessionExpired']()).toBeFalse();
    expect(authServiceSpy.clearSession).not.toHaveBeenCalled();
  });

  it('should handle storage events for active session changes', fakeAsync(() => {
    fixture.detectChanges();

    // Trigger storage event listener directly
    const storageEvent = new StorageEvent('storage', {
      key: 'app_active_session_id',
      newValue: 'another-tab-session'
    });
    window.dispatchEvent(storageEvent);

    expect(component['sessionExpired']()).toBeTrue();
    expect(authServiceSpy.clearSession).toHaveBeenCalled();

    tick(5000);
  }));

  it('should ignore storage events with different key or empty value', () => {
    fixture.detectChanges();

    const storageEvent = new StorageEvent('storage', {
      key: 'some_other_key',
      newValue: 'another-tab-session'
    });
    window.dispatchEvent(storageEvent);

    expect(component['sessionExpired']()).toBeFalse();
  });
});
