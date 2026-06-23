import { Component, Input, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';

export type StatusBadgeVariant =
  | 'satisfied'
  | 'missing'
  | 'unknown'
  | 'neutral'
  | 'success'
  | 'warning';

@Component({
  selector: 'gf-status-badge',
  standalone: true,
  imports: [CommonModule],
  template: `
    <span class="gf-badge" [ngClass]="badgeClass">{{ label }}</span>
  `,
  styles: [`
    .gf-badge {
      display: inline-block;
      padding: 4px 10px;
      border-radius: 12px;
      font-size: 0.75rem;
      font-weight: 600;
      text-transform: lowercase;
      white-space: nowrap;
    }
    .badge-satisfied { background: #e6f5e0; color: #2d6b24; }
    .badge-missing { background: #fce8e6; color: #c9190b; }
    .badge-unknown { background: #f5f5f5; color: #6a6e73; }
    .badge-neutral { background: #f5f5f5; color: #151515; border: 1px solid #d2d2d2; }
    .badge-success { background: #e6f5e0; color: #2d6b24; }
    .badge-warning { background: #fef3cd; color: #8a5500; }
  `],
  changeDetection: ChangeDetectionStrategy.Eager,
})
export class StatusBadgeComponent {
  @Input({ required: true }) label!: string;
  @Input() variant: StatusBadgeVariant = 'neutral';

  get badgeClass(): string {
    return `badge-${this.variant}`;
  }
}
