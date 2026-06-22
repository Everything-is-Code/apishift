import { Component, Input, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'gf-busy-overlay',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="busy-overlay" role="alertdialog" aria-modal="true" aria-busy="true"
         [attr.aria-labelledby]="titleId">
      <div class="busy-overlay-panel">
        <div class="busy-spinner" aria-hidden="true"></div>
        <p [id]="titleId" class="busy-overlay-title">{{ title }}</p>
        <p *ngIf="stage" class="busy-overlay-stage">{{ stage }}</p>
        <p *ngIf="hint" class="busy-overlay-hint">{{ hint }}</p>
      </div>
    </div>
  `,
  styles: [`
    .busy-overlay {
      position: fixed;
      inset: 0;
      z-index: 10000;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 24px;
      background: rgba(21, 21, 21, 0.62);
      backdrop-filter: blur(3px);
      pointer-events: all;
    }
    .busy-overlay-panel {
      width: min(420px, 100%);
      background: white;
      border-radius: 12px;
      padding: 36px 32px 28px;
      text-align: center;
      box-shadow: 0 18px 48px rgba(0, 0, 0, 0.28);
      border: 1px solid #e8e8e8;
    }
    .busy-spinner {
      width: 56px;
      height: 56px;
      margin: 0 auto 20px;
      border-radius: 50%;
      border: 4px solid #f0f0f0;
      border-top-color: #ee0000;
      border-right-color: #0066cc;
      animation: spin 0.9s linear infinite;
    }
    .busy-overlay-title {
      margin: 0 0 8px;
      font-family: 'Red Hat Display', sans-serif;
      font-size: 1.15rem;
      font-weight: 700;
      color: #151515;
    }
    .busy-overlay-stage {
      margin: 0 0 10px;
      font-size: 0.92rem;
      color: #0066cc;
      font-weight: 600;
      animation: fadeText 2s ease-in-out infinite alternate;
    }
    .busy-overlay-hint {
      margin: 0;
      font-size: 0.82rem;
      color: #6a6e73;
      line-height: 1.45;
    }
    @keyframes spin { to { transform: rotate(360deg); } }
    @keyframes fadeText { 0% { opacity: 0.5; } 100% { opacity: 1; } }
  `],
  changeDetection: ChangeDetectionStrategy.Eager,
})
export class BusyOverlayComponent {
  @Input({ required: true }) title!: string;
  @Input() stage = '';
  @Input() hint = '';
  @Input() titleId = 'busy-overlay-title';
}
