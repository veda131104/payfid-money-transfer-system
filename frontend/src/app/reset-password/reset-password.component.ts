import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators, AbstractControl } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

@Component({
    selector: 'app-reset-password',
    standalone: true,
    imports: [
        MatButtonModule,
        MatCardModule,
        MatFormFieldModule,
        MatInputModule,
        ReactiveFormsModule
    ],
    templateUrl: './reset-password.component.html',
    styleUrl: './reset-password.component.scss'
})
export class ResetPasswordComponent implements OnInit {
    form!: FormGroup;
    token: string | null = null;
    isSending = false;

    constructor(
        private readonly fb: FormBuilder,
        private readonly authService: AuthService,
        private readonly route: ActivatedRoute,
        private readonly router: Router
    ) {
        this.form = this.fb.nonNullable.group({
            password: ['', [Validators.required, Validators.minLength(8)]],
            confirmPassword: ['', [Validators.required]]
        }, { validators: this.passwordMatchValidator });
    }

    ngOnInit(): void {
        this.token = this.route.snapshot.queryParamMap.get('token');
        if (!this.token) {
            alert('Invalid reset link. Token is missing.');
            this.router.navigate(['/']);
        }
    }

    passwordMatchValidator(control: AbstractControl) {
        const g = control as FormGroup;
        return g.get('password')?.value === g.get('confirmPassword')?.value
            ? null : { mismatch: true };
    }

    submit(): void {
        if (this.form.invalid || !this.token) {
            this.form.markAllAsTouched();
            return;
        }

        this.isSending = true;
        const { password } = this.form.getRawValue();

        this.authService.resetPassword({ token: this.token, newPassword: password }).subscribe({
            next: () => {
                alert('Password has been reset successfully! Redirecting to dashboard...');
                this.router.navigate(['/dashboard']);
            },
            error: (err: any) => {
                console.error('Reset failed:', err);
                this.isSending = false;
                alert('Failed to reset password. Link may be expired.');
            }
        });
    }
}
