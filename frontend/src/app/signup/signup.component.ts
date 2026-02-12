import { Component } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import {
  AbstractControl,
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  Validators
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-signup',
  standalone: true,
  imports: [
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    RouterLink,
    ReactiveFormsModule
  ],
  templateUrl: './signup.component.html',
  styleUrl: './signup.component.scss'
})
export class SignupComponent {
  form!: FormGroup;
  loading = false;

  constructor(
    private readonly fb: FormBuilder,
    private readonly authService: AuthService,
    private readonly router: Router
  ) {
    this.form = this.fb.nonNullable.group(
      {
        name: ['', [Validators.required, Validators.minLength(3)]],
        email: ['', [Validators.required, Validators.email]],
        password: ['', [Validators.required, Validators.minLength(8)]],
        confirmPassword: ['', [Validators.required]]
      },
      { validators: [passwordsMatch] }
    );
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      alert('Please fill in all fields correctly.');
      return;
    }

    this.loading = true;
    const { name, email, password } = this.form.getRawValue();
    this.authService.signup({ name, email, password }).subscribe({
      next: () => {
        this.loading = false;
        alert('Signup successful! Please login.');
        this.router.navigate(['/login']);
      },
      error: error => {
        this.loading = false;
        if (error?.status === 409) {
          alert('Email is already registered.');
          this.form.get('email')?.setErrors({ emailTaken: true });
          return;
        }
        alert('Signup failed. Please try again.');
        this.form.setErrors({ signupFailed: true });
      }
    });
  }
}

function passwordsMatch(control: AbstractControl): ValidationErrors | null {
  const password = control.get('password')?.value;
  const confirmPassword = control.get('confirmPassword')?.value;
  if (!password || !confirmPassword) {
    return null;
  }
  return password === confirmPassword ? null : { passwordMismatch: true };
}
