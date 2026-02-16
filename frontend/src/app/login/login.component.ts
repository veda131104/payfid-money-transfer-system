import { Component, OnInit } from '@angular/core';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators, FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    RouterLink,
    ReactiveFormsModule,
    FormsModule
  ],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent implements OnInit {
  form!: FormGroup;
  isForgotModalOpen = false;
  forgotUsernameValue = '';
  isSending = false;

  constructor(
    private readonly fb: FormBuilder,
    private readonly authService: AuthService,
    private readonly router: Router,
    private readonly route: ActivatedRoute
  ) {
    this.form = this.fb.nonNullable.group({
      name: ['', [Validators.required]],
      password: ['', [Validators.required]],
      rememberMe: [true]
    });
  }

  ngOnInit(): void {
    const token = this.route.snapshot.queryParamMap.get('token');
    if (token) {
      console.log('✨ [Login] Magic token detected, attempting auto-login...');
      this.isSending = true; // Show loading state if needed
      this.authService.loginWithToken(token).subscribe({
        next: () => {
          console.log('✅ [Login] Magic login successful!');
          this.router.navigate(['/dashboard']);
        },
        error: (err) => {
          console.error('❌ [Login] Magic login failed:', err);
          this.isSending = false;
          alert('Magic link is invalid or expired. Please try again.');
        }
      });
    }

    // Check for remember token
    const rememberToken = localStorage.getItem('remember_token');
    if (rememberToken && !token) {
      this.isSending = true;
      this.authService.getCredentialsByToken(rememberToken).subscribe({
        next: (creds) => {
          this.form.patchValue({
            name: creds.name,
            password: creds.password,
            rememberMe: true
          });
          this.isSending = false;
        },
        error: () => {
          localStorage.removeItem('remember_token');
          this.isSending = false;
        }
      });
    }
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      let errorMsg = 'Please check the following:\n';
      if (this.form.get('name')?.invalid) errorMsg += '- Username must be at least 3 characters.\n';
      if (this.form.get('password')?.invalid) errorMsg += '- Password must be at least 8 characters.\n';
      alert(errorMsg);
      console.log('Form Errors:', {
        name: this.form.get('name')?.errors,
        password: this.form.get('password')?.errors
      });
      return;
    }

    const { name, password, rememberMe } = this.form.getRawValue();
    this.isSending = true;
    this.authService.login({ name, password, rememberMe }).subscribe({
      next: (res) => {
        this.isSending = false;
        if (rememberMe && res.rememberToken) {
          localStorage.setItem('remember_token', res.rememberToken);
        } else {
          localStorage.removeItem('remember_token');
        }
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.isSending = false;
        console.error('Login error:', err);
        alert(err.error?.message || 'Invalid username or password.');
        this.form.get('name')?.setErrors({ invalidLogin: true });
        this.form.get('password')?.setErrors({ invalidLogin: true });
      }
    });
  }

  onForgotPassword(): void {
    if (!this.forgotUsernameValue || this.forgotUsernameValue.length < 3) {
      alert('Please enter a valid username.');
      return;
    }

    this.isSending = true;
    this.authService.forgotPassword(this.forgotUsernameValue).subscribe({
      next: () => {
        alert('If the username exists, a recovery email has been sent!');
        this.closeForgotModal();
      },
      error: () => {
        alert('Something went wrong. Please try again later.');
        this.isSending = false;
      }
    });
  }

  openForgotModal(): void {
    this.isForgotModalOpen = true;
    this.forgotUsernameValue = this.form.get('name')?.value || '';
  }

  closeForgotModal(): void {
    this.isForgotModalOpen = false;
    this.isSending = false;
    this.forgotUsernameValue = '';
  }
}

