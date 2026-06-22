import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { PopupService } from '../services/popup.service';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    ReactiveFormsModule
  ],
  templateUrl: './forgot-password.component.html',
  styleUrl: './forgot-password.component.scss'
})
export class ForgotPasswordComponent {
  form: FormGroup;
  isSending = false;
  private readonly popupService = inject(PopupService);

  constructor(
    private readonly fb: FormBuilder,
    private readonly authService: AuthService,
    private readonly router: Router
  ) {
    this.form = this.fb.nonNullable.group({
      name: ['', [Validators.required, Validators.minLength(3)]]
    });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isSending = true;
    const { name } = this.form.getRawValue();
    this.authService.forgotPassword(name).subscribe({
      next: (resp) => {
        this.isSending = false;
        this.popupService.alert(resp?.message || 'If an account exists, a reset link has been sent.', 'Password Reset Link');
        this.router.navigate(['/']);
      },
      error: (err) => {
        this.isSending = false;
        this.popupService.alert('Failed to send reset email. Please try again later.', 'Error');
      }
    });
  }
}
