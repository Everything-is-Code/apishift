import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { WizardHistoryActions, WizardHistoryState } from './wizard-review.types';

@Component({
  selector: 'app-wizard-history',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './wizard-history.component.html',
  styleUrls: ['../migration-wizard.shared.scss'],
})
export class WizardHistoryComponent {
  @Input({ required: true }) s!: WizardHistoryState;
  @Input({ required: true }) a!: WizardHistoryActions;
}
