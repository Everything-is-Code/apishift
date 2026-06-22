import { Component, Input, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { StatusBadgeComponent } from '../../../shared/ui/status-badge.component';
import { WizardReviewActions, WizardReviewState } from './wizard-review.types';

@Component({
  selector: 'app-wizard-step-review',
  standalone: true,
  imports: [CommonModule, FormsModule, StatusBadgeComponent],
  templateUrl: './wizard-step-review.component.html',
  styleUrls: ['../migration-wizard.shared.scss'],
  changeDetection: ChangeDetectionStrategy.Eager,
})
export class WizardStepReviewComponent {
  @Input({ required: true }) s!: WizardReviewState;
  @Input({ required: true }) a!: WizardReviewActions;
}
