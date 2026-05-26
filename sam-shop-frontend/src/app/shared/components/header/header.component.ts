import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { TokenService } from '../../../core/services/token.service';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './header.component.html',
  styleUrl: './header.component.scss'
})
export class HeaderComponent {
  private auth = inject(AuthService);
  private token = inject(TokenService);

  user = this.auth.currentUser;

  logout(): void {
    this.auth.logout();
  }

  isStaff(): boolean {
    return this.token.hasRole('ADMIN', 'EMPLOYEE');
  }
}
