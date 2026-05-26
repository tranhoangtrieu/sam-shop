import { Component } from '@angular/core';

@Component({
  selector: 'app-loading',
  standalone: true,
  template: `<div class="loading"><span class="spinner"></span><p>Đang tải...</p></div>`,
  styles: [`
    .loading { display: flex; flex-direction: column; align-items: center; padding: 2rem; color: #666; }
    .spinner { width: 36px; height: 36px; border: 3px solid #e0e0e0; border-top-color: #2e7d32; border-radius: 50%; animation: spin 0.8s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }
  `]
})
export class LoadingComponent {}
