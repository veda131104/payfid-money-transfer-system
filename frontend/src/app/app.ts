import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterOutlet } from '@angular/router';
import { AuthService } from './services/auth.service';
import { PopupService } from './services/popup.service';

const TAB_SESSION_KEY = 'app_tab_session_id';
const ACTIVE_SESSION_KEY = 'app_active_session_id';
const SESSION_CHANNEL = 'app_single_session_channel';

interface ActiveSessionMessage {
  type: 'ACTIVE_SESSION_CHANGED';
  activeSessionId: string;
}

@Component({
  selector: 'app-root',
  imports: [CommonModule, RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App implements OnInit, OnDestroy {
  protected readonly title = signal('frontend');
  protected readonly sessionExpired = signal(false);
  protected readonly sessionMessage = signal(
    'This session has been opened in another tab or window. This session is no longer active and will be closed. Please continue in the latest session.'
  );
  protected readonly popupService = inject(PopupService);

  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly tabSessionId = this.getOrCreateTabSessionId();
  private broadcastChannel: BroadcastChannel | null = null;
  private expiredRedirectTimer: ReturnType<typeof setTimeout> | null = null;
  private readonly storageListener = this.onStorageEvent.bind(this);

  ngOnInit(): void {
    if (typeof window === 'undefined') {
      return;
    }

    this.startBroadcastChannel();
    window.addEventListener('storage', this.storageListener);

    // Claim this tab as the active session on startup.
    this.setActiveSession(this.tabSessionId);
  }

  ngOnDestroy(): void {
    if (typeof window !== 'undefined') {
      window.removeEventListener('storage', this.storageListener);
    }
    if (this.broadcastChannel) {
      this.broadcastChannel.close();
      this.broadcastChannel = null;
    }
    this.expiredRedirectTimer && clearTimeout(this.expiredRedirectTimer);
  }

  private getOrCreateTabSessionId(): string {
    if (typeof window === 'undefined') {
      return 'unknown-session';
    }

    const existingId = sessionStorage.getItem(TAB_SESSION_KEY);
    if (existingId) {
      return existingId;
    }

    const newId = this.generateSessionId();
    sessionStorage.setItem(TAB_SESSION_KEY, newId);
    return newId;
  }

  private generateSessionId(): string {
    return `${Date.now()}-${Math.random().toString(36).slice(2)}-${crypto.randomUUID?.() ?? ''}`;
  }

  private startBroadcastChannel(): void {
    if (typeof BroadcastChannel === 'undefined') {
      return;
    }

    this.broadcastChannel = new BroadcastChannel(SESSION_CHANNEL);
    this.broadcastChannel.onmessage = ({ data }: MessageEvent) => {
      const message = data as ActiveSessionMessage;
      if (message?.type === 'ACTIVE_SESSION_CHANGED') {
        this.handleSessionChanged(message.activeSessionId);
      }
    };
  }

  private setActiveSession(activeSessionId: string): void {
    try {
      localStorage.setItem(ACTIVE_SESSION_KEY, activeSessionId);
      this.broadcastActiveSession(activeSessionId);
    } catch (error) {
      console.warn('[App] Unable to write active session id to localStorage', error);
    }
  }

  private broadcastActiveSession(activeSessionId: string): void {
    if (!this.broadcastChannel) {
      return;
    }

    const message: ActiveSessionMessage = {
      type: 'ACTIVE_SESSION_CHANGED',
      activeSessionId
    };
    this.broadcastChannel.postMessage(message);
  }

  private onStorageEvent(event: StorageEvent): void {
    if (event.key !== ACTIVE_SESSION_KEY || !event.newValue) {
      return;
    }
    this.handleSessionChanged(event.newValue);
  }

  private handleSessionChanged(activeSessionId: string): void {
    if (activeSessionId === this.tabSessionId) {
      return;
    }

    if (!this.sessionExpired()) {
      this.markSessionExpired();
    }
  }

  private markSessionExpired(): void {
    console.log('[App] Session replaced by another tab. Marking this session as expired.');
    this.sessionExpired.set(true);
    this.authService.clearSession();

    // Disable interaction and redirect after 5 seconds.
    this.expiredRedirectTimer = setTimeout(() => {
      this.router.navigate(['/']);
    }, 5000);
  }
}
