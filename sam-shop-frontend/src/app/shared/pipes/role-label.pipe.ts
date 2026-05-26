import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'roleLabel', standalone: true })
export class RoleLabelPipe implements PipeTransform {
  transform(role: string): string {
    const map: Record<string, string> = {
      USER: 'Khách hàng',
      EMPLOYEE: 'Nhân viên',
      ADMIN: 'Quản trị'
    };
    return map[role] || role;
  }
}
