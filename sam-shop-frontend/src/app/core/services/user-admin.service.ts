import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiResponse, User, UserRole } from '../models/user.model';
import { ApiService } from './api.service';

export interface CreateUserRequest {
  username: string;
  password: string;
  email: string;
  phone?: string;
  role: UserRole;
}

@Injectable({ providedIn: 'root' })
export class UserAdminService {
  private api = inject(ApiService);

  listUsers(): Observable<ApiResponse<User[]>> {
    return this.api.get<User[]>('/api/admin/users');
  }

  createUser(body: CreateUserRequest): Observable<ApiResponse<User>> {
    return this.api.post<User>('/api/admin/users', body);
  }
}
