import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private router = inject(Router);

  error = '';
  loading = false;

  form = this.fb.group({
    username: ['', Validators.required],
    password: ['', Validators.required]
  });

  submit(): void {
    if (this.form.invalid) return;
    this.loading = true;
    this.error = '';
    this.auth.login(this.form.getRawValue() as { username: string; password: string }).subscribe({
      next: res => {
        this.loading = false;
        if (res.success) {
          const role = this.auth.currentUser()?.role;
          if (role === 'ADMIN' || role === 'EMPLOYEE') {
            this.router.navigate(['/admin']);
          } else {
            this.router.navigate(['/products']);
          }
        } else {
          this.error = res.message;
        }
      },
      error: err => {
        this.loading = false;
        this.error = err.error?.error_description || err.error?.message || 'Đăng nhập thất bại';
      }
    });
  }
}
