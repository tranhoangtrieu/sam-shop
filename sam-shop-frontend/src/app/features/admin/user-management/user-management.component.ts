import { DatePipe } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { User, UserRole } from '../../../core/models/user.model';
import { CreateUserRequest, UserAdminService } from '../../../core/services/user-admin.service';
import { LoadingComponent } from '../../../shared/components/loading/loading.component';

@Component({
  selector: 'app-user-management',
  standalone: true,
  imports: [ReactiveFormsModule, DatePipe, LoadingComponent],
  templateUrl: './user-management.component.html',
  styleUrl: './user-management.component.scss'
})
export class UserManagementComponent implements OnInit {
  private fb = inject(FormBuilder);
  private userAdmin = inject(UserAdminService);

  loading = true;
  saving = false;
  users: User[] = [];
  error = '';
  success = '';

  roles: UserRole[] = ['USER', 'EMPLOYEE', 'ADMIN'];

  form = this.fb.group({
    username: ['', [Validators.required, Validators.minLength(3)]],
    password: ['', [Validators.required, Validators.minLength(6)]],
    email: ['', [Validators.required, Validators.email]],
    phone: [''],
    role: ['USER' as UserRole, Validators.required]
  });

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.loading = true;
    this.error = '';
    this.userAdmin.listUsers().subscribe({
      next: res => {
        this.users = (res.data || []).map(u => {
          const row = u as User & { userId?: number };
          return {
            id: row.userId ?? row.id ?? 0,
            username: row.username,
            email: row.email,
            phone: row.phone,
            role: (row.role || 'USER') as UserRole
          };
        });
        this.loading = false;
      },
      error: err => {
        this.loading = false;
        this.error = err.error?.message || 'Không tải được danh sách người dùng';
      }
    });
  }

  createUser(): void {
    if (this.form.invalid) return;
    this.saving = true;
    this.error = '';
    this.success = '';
    const raw = this.form.getRawValue();
    const body: CreateUserRequest = {
      username: raw.username!,
      password: raw.password!,
      email: raw.email!,
      phone: raw.phone || undefined,
      role: raw.role as UserRole
    };
    this.userAdmin.createUser(body).subscribe({
      next: res => {
        this.saving = false;
        if (res.success) {
          this.success = `Đã tạo tài khoản ${body.username} (${body.role})`;
          this.form.reset({ role: 'USER' });
          this.loadUsers();
        } else {
          this.error = res.message || 'Tạo tài khoản thất bại';
        }
      },
      error: err => {
        this.saving = false;
        this.error = err.error?.message || 'Tạo tài khoản thất bại';
      }
    });
  }
}
