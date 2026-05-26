import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { FooterComponent } from '../../shared/components/footer/footer.component';
import { HeaderComponent } from '../../shared/components/header/header.component';

@Component({
  selector: 'app-client-layout',
  standalone: true,
  imports: [RouterOutlet, HeaderComponent, FooterComponent],
  template: `
    <div class="layout">
      <app-header />
      <main class="main"><router-outlet /></main>
      <app-footer />
    </div>
  `,
  styles: [`
    .layout { min-height: 100vh; display: flex; flex-direction: column; background: #f5faf5; }
    .main { flex: 1; max-width: 1200px; width: 100%; margin: 0 auto; padding: 2rem 1.5rem; }
  `]
})
export class ClientLayoutComponent {}
