import { Component, Input, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'gf-loading-skeleton',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="loading-state" role="status" [attr.aria-label]="message || 'Loading'">
      <div class="skeleton-bar" [class.wide]="variant === 'cards'"></div>
      <div *ngIf="variant === 'cards'" class="skeleton-grid-cards">
        <div *ngFor="let i of cardSlots" class="skeleton-card">
          <div class="skeleton-line long"></div>
          <div class="skeleton-line short"></div>
        </div>
      </div>
      <p *ngIf="message" class="loading-text">{{ message }}</p>
    </div>
  `,
  styles: [`
    .loading-state { padding: 16px 0 32px; }
    .skeleton-bar {
      height: 14px;
      border-radius: 4px;
      background: linear-gradient(90deg, #f0f0f0 25%, #e8e8e8 50%, #f0f0f0 75%);
      background-size: 200% 100%;
      animation: shimmer 1.4s ease infinite;
      margin-bottom: 20px;
      max-width: 320px;
    }
    .skeleton-bar.wide { max-width: 100%; }
    .skeleton-grid-cards {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
      gap: 12px;
    }
    .skeleton-card {
      padding: 20px;
      border-radius: 8px;
      background: white;
      border: 1px solid #e8e8e8;
    }
    .skeleton-line {
      height: 12px;
      border-radius: 4px;
      background: linear-gradient(90deg, #f0f0f0 25%, #e8e8e8 50%, #f0f0f0 75%);
      background-size: 200% 100%;
      animation: shimmer 1.4s ease infinite;
      margin-bottom: 10px;
    }
    .skeleton-line.long { width: 75%; }
    .skeleton-line.short { width: 40%; }
    .loading-text { color: #6a6e73; font-size: 0.9rem; margin-top: 16px; }
    @keyframes shimmer { 0% { background-position: 200% 0; } 100% { background-position: -200% 0; } }
  `],
  changeDetection: ChangeDetectionStrategy.Eager,
})
export class LoadingSkeletonComponent {
  @Input() message = '';
  @Input() variant: 'bar' | 'cards' = 'cards';
  @Input() cardCount = 6;

  get cardSlots(): number[] {
    return Array.from({ length: this.cardCount }, (_, i) => i);
  }
}
