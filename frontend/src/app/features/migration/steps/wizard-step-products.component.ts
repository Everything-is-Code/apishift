import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ImportExportResponse, ThreeScaleProduct } from '../../../core/api/models';
import { ProductRow } from '../migration-wizard.helpers';

export type { ProductRow };

@Component({
  selector: 'app-wizard-step-products',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './wizard-step-products.component.html',
  styleUrls: ['../migration-wizard.shared.scss'],
  changeDetection: ChangeDetectionStrategy.Eager,
})
export class WizardStepProductsComponent {
  @Input({ required: true }) products!: ProductRow[];
  @Input({ required: true }) productSource!: 'live' | 'export';
  @Input({ required: true }) selectedCount!: number;
  @Input({ required: true }) productSearchQuery!: string;
  @Input({ required: true }) pagedProducts!: ProductRow[];
  @Input({ required: true }) visibleCount!: number;
  @Input({ required: true }) allFilteredSelected!: boolean;
  @Input({ required: true }) productPage!: number;
  @Input({ required: true }) totalPages!: number;
  @Input() importing = false;
  @Input() importError = '';
  @Input() importResult: ImportExportResponse | null = null;
  @Input() hasExportFile = false;
  @Input() isExportProduct: (product: ThreeScaleProduct) => boolean = () => false;

  @Output() sourceChange = new EventEmitter<'live' | 'export'>();
  @Output() exportFileSelected = new EventEmitter<Event>();
  @Output() importArchive = new EventEmitter<void>();
  @Output() searchQueryChange = new EventEmitter<string>();
  @Output() selectAllFiltered = new EventEmitter<void>();
  @Output() pagePrev = new EventEmitter<void>();
  @Output() pageNext = new EventEmitter<void>();
  @Output() next = new EventEmitter<void>();
}
