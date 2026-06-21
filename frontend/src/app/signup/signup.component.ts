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
        username: ['', [Validators.required, Validators.minLength(3)]],
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
      
      const confirmPasswordControl = this.form.get('confirmPassword');
      if (confirmPasswordControl?.hasError('passwordMismatch')) {
        alert('Validation Error: Passwords do not match.');
        return;
      }
      
      const usernameControl = this.form.get('username');
      if (usernameControl?.hasError('minlength')) {
        alert('Validation Error: Username must be at least 3 characters long.');
        return;
      }
      
      const emailControl = this.form.get('email');
      if (emailControl?.hasError('email')) {
        alert('Validation Error: Please enter a valid email address.');
        return;
      }
      
      const passwordControl = this.form.get('password');
      if (passwordControl?.hasError('minlength')) {
        alert('Validation Error: Password must be at least 8 characters long.');
        return;
      }

      alert('Please fill in all fields correctly.');
      return;
    }

    this.loading = true;
    const { username, email, password } = this.form.getRawValue();
    this.authService.signup({ name: username, email, password }).subscribe({
      next: () => {
        this.loading = false;
        alert('Signup successful! Please log in to complete your account setup.');
        this.router.navigate(['/']);
      },
      error: error => {
        this.loading = false;
        if (error?.status === 409) {
          alert(error.error?.message || 'Username or email is already in use.');
          this.form.reset();
          return;
        }
        alert(error.error?.message || 'Signup failed. Please try again.');
        this.form.reset();
      }
    });
  }
}

function passwordsMatch(control: AbstractControl): ValidationErrors | null {
  const password = control.get('password')?.value;
  const confirmPassword = control.get('confirmPassword')?.value;
  const confirmPasswordControl = control.get('confirmPassword');
  
  if (!password || !confirmPassword) {
    return null;
  }
  
  if (password !== confirmPassword) {
    confirmPasswordControl?.setErrors({ passwordMismatch: true });
    return { passwordMismatch: true };
  } else {
    if (confirmPasswordControl?.hasError('passwordMismatch')) {
      const currentErrors = confirmPasswordControl.errors;
      if (currentErrors) {
        delete currentErrors['passwordMismatch'];
        confirmPasswordControl.setErrors(Object.keys(currentErrors).length ? currentErrors : null);
      }
    }
  }
  return null;
}
