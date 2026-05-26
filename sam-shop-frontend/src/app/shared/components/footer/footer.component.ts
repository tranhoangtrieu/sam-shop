import { Component } from '@angular/core';

@Component({
  selector: 'app-footer',
  standalone: true,
  template: `<footer class="footer"><p>© 2026 Sam Shop — Nước sâm thảo mộc</p></footer>`,
  styles: [`.footer { text-align: center; padding: 1.5rem; color: #888; font-size: 0.875rem; margin-top: auto; }`]
})
export class FooterComponent {}
