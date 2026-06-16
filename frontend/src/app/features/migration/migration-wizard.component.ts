import { Component, CUSTOM_ELEMENTS_SCHEMA, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { BusyOverlayComponent } from '../../shared/ui/busy-overlay.component';
import { LoadingSkeletonComponent } from '../../shared/ui/loading-skeleton.component';
import { WizardStepNavComponent } from './steps/wizard-step-nav.component';
import { WizardStepProductsComponent } from './steps/wizard-step-products.component';
import { WizardStepStrategyComponent } from './steps/wizard-step-strategy.component';
import { WizardStepReviewComponent } from './steps/wizard-step-review.component';
import { WizardHistoryComponent } from './steps/wizard-history.component';
import { MigrationWizardStateService } from './migration-wizard.state.service';

@Component({
  selector: 'app-migration-wizard',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    LoadingSkeletonComponent,
    BusyOverlayComponent,
    WizardStepNavComponent,
    WizardStepProductsComponent,
    WizardStepStrategyComponent,
    WizardStepReviewComponent,
    WizardHistoryComponent,
  ],
  providers: [MigrationWizardStateService],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  template: `
    <div class="wizard-root" [class.is-analyzing]="wizard.analyzing">
    <header class="page-header">
      <div class="container">
        <div class="header-row">
          <div>
            <h1>Migration Wizard</h1>
            <p class="subtitle">Select 3scale products, choose a gateway strategy, and review generated Connectivity Link resources.</p>
          </div>
          <a routerLink="/" class="header-link">Dashboard</a>
        </div>
      </div>
    </header>

    <section class="container main-content">
      <app-wizard-step-nav [step]="wizard.step" [stepLabels]="wizard.stepLabels" />

      <gf-loading-skeleton
        *ngIf="wizard.productsLoading || wizard.importing"
        [message]="wizard.productsLoadingMessage" />

      <app-wizard-step-products
        *ngIf="!wizard.productsLoading && !wizard.importing && wizard.step === 1"
        [products]="wizard.products"
        [productSource]="wizard.productSource"
        [selectedCount]="wizard.selectedCount"
        [productSearchQuery]="wizard.productSearchQuery"
        [pagedProducts]="wizard.pagedMigrateProducts"
        [visibleCount]="wizard.visibleProducts.length"
        [allFilteredSelected]="wizard.allFilteredSelected"
        [productPage]="wizard.migrateProductPage"
        [totalPages]="wizard.migrateTotalPages"
        [importing]="wizard.importing"
        [importError]="wizard.importError"
        [importResult]="wizard.importResult"
        [hasExportFile]="!!wizard.selectedExportFile"
        [isExportProduct]="wizard.isExportProductFn"
        (sourceChange)="wizard.setProductSource($event)"
        (exportFileSelected)="wizard.onExportFileSelected($event)"
        (importArchive)="wizard.importExportArchive()"
        (searchQueryChange)="wizard.productSearchQuery = $event; wizard.onProductSearchChange()"
        (selectAllFiltered)="wizard.selectAllFiltered()"
        (pagePrev)="wizard.migrateProductPage = wizard.migrateProductPage - 1"
        (pageNext)="wizard.migrateProductPage = wizard.migrateProductPage + 1"
        (next)="wizard.step = 2" />

      <app-wizard-step-strategy
        *ngIf="!wizard.productsLoading && wizard.step === 2"
        [strategies]="wizard.strategies"
        [gatewayStrategy]="wizard.gatewayStrategy"
        [targetClusters]="wizard.targetClusters"
        [selectedClusterId]="wizard.selectedClusterId"
        [analyzing]="wizard.analyzing"
        (strategyChange)="wizard.gatewayStrategy = $event"
        (clusterChange)="wizard.selectedClusterId = $event"
        (back)="wizard.step = 1"
        (analyze)="wizard.analyze()" />

      <app-wizard-step-review
        *ngIf="!wizard.productsLoading && wizard.step === 3 && wizard.plan"
        [s]="wizard.reviewState"
        [a]="wizard.reviewActions" />

      <app-wizard-history [s]="wizard.historyState" [a]="wizard.historyActions" />
    </section>

    <gf-busy-overlay
      *ngIf="wizard.analyzing"
      title="Analyzing migration"
      [stage]="wizard.analyzeStage"
      hint="Please wait — generating Connectivity Link resources for your selection."
      titleId="analyze-overlay-title" />
    </div>
  `,
  styleUrls: ['./migration-wizard.shared.scss'],
})
export class MigrationWizardComponent implements OnInit {
  constructor(readonly wizard: MigrationWizardStateService) {}

  ngOnInit(): void {
    this.wizard.init();
  }
}
