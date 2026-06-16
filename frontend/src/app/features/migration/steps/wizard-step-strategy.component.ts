import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TargetCluster } from '../../../core/api/models';

export interface GatewayStrategyOption {
  value: string;
  icon: string;
  label: string;
  description: string;
}

@Component({
  selector: 'app-wizard-step-strategy',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './wizard-step-strategy.component.html',
  styleUrls: ['../migration-wizard.shared.scss'],
})
export class WizardStepStrategyComponent {
  @Input({ required: true }) strategies!: GatewayStrategyOption[];
  @Input({ required: true }) gatewayStrategy!: string;
  @Input({ required: true }) targetClusters!: TargetCluster[];
  @Input({ required: true }) selectedClusterId!: string;
  @Input() analyzing = false;

  @Output() strategyChange = new EventEmitter<string>();
  @Output() clusterChange = new EventEmitter<string>();
  @Output() back = new EventEmitter<void>();
  @Output() analyze = new EventEmitter<void>();
}
