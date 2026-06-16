import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { WizardReviewActions, WizardReviewState } from './wizard-review.types';

@Component({
  selector: 'app-wizard-step-review',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './wizard-step-review.component.html',
  styleUrls: ['../migration-wizard.shared.scss'],
})
export class WizardStepReviewComponent {
  @Input({ required: true }) s!: WizardReviewState;
  @Input({ required: true }) a!: WizardReviewActions;
}
