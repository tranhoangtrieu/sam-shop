import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { HeaderComponent } from '../../shared/components/header/header.component';
import { SidebarComponent } from '../../shared/components/sidebar/sidebar.component';

@Component({
  selector: 'app-admin-layout',
  standalone: true,
  imports: [RouterOutlet, HeaderComponent, SidebarComponent],
  template: `
    <app-header />
    <div class="admin-wrap">
      <app-sidebar />
      <main class="admin-main"><router-outlet /></main>
    </div>
  `,
  styles: [`
    .admin-wrap { display: flex; min-height: calc(100vh - 64px); background: #f0f4f0; }
    .admin-main { flex: 1; padding: 2rem; overflow: auto; }
  `]
})
export class AdminLayoutComponent {}
