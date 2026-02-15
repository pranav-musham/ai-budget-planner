import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, AbstractControl, ValidationErrors } from '@angular/forms';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatTabsModule } from '@angular/material/tabs';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

export interface AuthDialogData {
  defaultTab?: 'login' | 'register';
  returnUrl?: string;
}

@Component({
  selector: 'app-auth-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatTabsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './auth-dialog.html',
  styleUrl: './auth-dialog.css'
})
export class AuthDialogComponent {
  loginForm: FormGroup;
  registerForm: FormGroup;
  isLoginLoading = false;
  isRegisterLoading = false;
  loginErrorMessage = '';
  registerErrorMessage = '';
  hideLoginPassword = true;
  hideRegisterPassword = true;
  hideConfirmPassword = true;
  selectedTabIndex = 0;
  returnUrl: string;

  constructor(
    private formBuilder: FormBuilder,
    private authService: AuthService,
    private router: Router,
    public dialogRef: MatDialogRef<AuthDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: AuthDialogData
  ) {
    // Set default tab based on data
    this.selectedTabIndex = data?.defaultTab === 'register' ? 1 : 0;
    this.returnUrl = data?.returnUrl || '/dashboard';

    // Initialize login form
    this.loginForm = this.formBuilder.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]]
    });

    // Initialize register form
    this.registerForm = this.formBuilder.group({
      email: ['', [Validators.required, Validators.email]],
      fullName: [''],
      password: ['', [Validators.required, Validators.minLength(8), Validators.pattern(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$/)]],
      confirmPassword: ['', [Validators.required]]
    }, { validators: this.passwordMatchValidator });
  }

  passwordMatchValidator(control: AbstractControl): ValidationErrors | null {
    const password = control.get('password');
    const confirmPassword = control.get('confirmPassword');

    if (!password || !confirmPassword) {
      return null;
    }

    return password.value === confirmPassword.value ? null : { passwordMismatch: true };
  }

  onLoginSubmit(): void {
    if (this.loginForm.invalid) {
      return;
    }

    this.isLoginLoading = true;
    this.loginErrorMessage = '';

    this.authService.login(this.loginForm.value).subscribe({
      next: () => {
        this.isLoginLoading = false;
        this.dialogRef.close({ success: true });
        this.router.navigateByUrl(this.returnUrl);
      },
      error: (error) => {
        this.isLoginLoading = false;
        this.loginErrorMessage = 'Invalid email or password. Please try again.';
        console.error('Login error:', error);
      }
    });
  }

  onRegisterSubmit(): void {
    if (this.registerForm.invalid) {
      return;
    }

    this.isRegisterLoading = true;
    this.registerErrorMessage = '';

    const { email, password, fullName } = this.registerForm.value;

    this.authService.register({ email, password, fullName }).subscribe({
      next: () => {
        this.isRegisterLoading = false;
        this.dialogRef.close({ success: true });
        this.router.navigate(['/dashboard']);
      },
      error: (error) => {
        this.isRegisterLoading = false;
        this.registerErrorMessage = error.error?.message || 'Registration failed. Please try again.';
        console.error('Registration error:', error);
      }
    });
  }

  onCancel(): void {
    this.dialogRef.close({ success: false });
  }
}
