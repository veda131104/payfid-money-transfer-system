import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../services/auth.service';
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
        if (typeof window !== 'undefined') {
          alert(resp?.message || 'If an account exists, a reset link has been sent.');
        }
        this.router.navigate(['/']);
      },
      error: (err) => {
        console.error('Forgot password failed:', err);
        this.isSending = false;
        if (typeof window !== 'undefined') {
          alert('Failed to send reset email. Please try again later.');
        }
      }
    });
  }
}
