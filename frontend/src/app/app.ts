import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { AuthService } from './services/auth.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App implements OnInit {
  protected readonly title = signal('frontend');
  private readonly authService = inject(AuthService);

  ngOnInit(): void {
    if (typeof window === 'undefined') {
      return;
    }

    const rememberToken = localStorage.getItem('remember_token');
    if (!rememberToken) {
      return;
    }

    // Force a refresh of the JWT on every application startup when a remember token exists.
    this.authService.loginWithToken(rememberToken).subscribe({
      next: () => {
        console.log('[App] Refreshed JWT from remember_token successfully');
      },
      error: (err) => {
        console.warn('[App] Failed to refresh JWT from remember_token', err);
        localStorage.removeItem('remember_token');
      }
    });
  }
}
