import { Injectable, signal } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class PopupService {
  public readonly isOpen = signal(false);
  public readonly message = signal('');
  public readonly title = signal('Alert');

  alert(message: string, title: string = 'Alert'): void {
    this.title.set(title);
    this.message.set(message);
    this.isOpen.set(true);
  }

  close(): void {
    this.isOpen.set(false);
  }
}
