import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-wizard-step-nav',
  standalone: true,
  imports: [CommonModule],
  template: `
    <nav class="steps" aria-label="Wizard steps">
      <div class="step-track">
        <ng-container *ngFor="let s of stepLabels; let i = index">
          <div class="step-node-wrap">
            <div
              class="step-node"
              [class.active]="step === i + 1"
              [class.done]="step > i + 1">
              <span>{{ i + 1 }}</span>
            </div>
            <span class="step-caption" [class.active]="step === i + 1">{{ s }}</span>
          </div>
          <div *ngIf="i < stepLabels.length - 1" class="step-connector" [class.active]="step > i + 1"></div>
        </ng-container>
      </div>
    </nav>
  `,
  styleUrls: ['../migration-wizard.shared.scss'],
  changeDetection: ChangeDetectionStrategy.Eager,
})
export class WizardStepNavComponent {
  @Input({ required: true }) step!: number;
  @Input({ required: true }) stepLabels!: string[];
}
