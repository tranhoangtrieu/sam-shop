import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { TokenService } from '../../../core/services/token.service';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.scss'
})
export class SidebarComponent {
  private token = inject(TokenService);

  isAdmin(): boolean {
    return this.token.hasRole('ADMIN');
  }

  isStaff(): boolean {
    return this.token.hasRole('ADMIN', 'EMPLOYEE');
  }
}
