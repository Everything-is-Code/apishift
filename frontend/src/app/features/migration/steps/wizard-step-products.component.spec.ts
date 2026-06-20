import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { By } from '@angular/platform-browser';
import { WizardStepProductsComponent } from './wizard-step-products.component';
import { ProductRow } from '../migration-wizard.helpers';
import { ThreeScaleProduct } from '../../../core/api/models';

describe('WizardStepProductsComponent', () => {
  let fixture: ComponentFixture<WizardStepProductsComponent>;

  const product: ThreeScaleProduct = {
    name: 'demo-api',
    namespace: 'default',
    systemName: 'demo-api',
    description: 'Demo',
    deploymentOption: 'hosted',
    mappingRules: [],
    backendUsages: [],
    authentication: {},
    source: 'local',
  };

  const rows: ProductRow[] = [{ product, selected: false }];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [WizardStepProductsComponent],
      providers: [provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(WizardStepProductsComponent);
    fixture.componentRef.setInput('products', rows);
    fixture.componentRef.setInput('productSource', 'live');
    fixture.componentRef.setInput('selectedCount', 0);
    fixture.componentRef.setInput('productSearchQuery', '');
    fixture.componentRef.setInput('pagedProducts', rows);
    fixture.componentRef.setInput('visibleCount', 1);
    fixture.componentRef.setInput('allFilteredSelected', false);
    fixture.componentRef.setInput('productPage', 1);
    fixture.componentRef.setInput('totalPages', 1);
  });

  it('rendersProductsAndEmitsInteractions', () => {
    fixture.detectChanges();
    const component = fixture.componentInstance;
    const sourceSpy = spyOn(component.sourceChange, 'emit');
    const searchSpy = spyOn(component.searchQueryChange, 'emit');
    const selectSpy = spyOn(component.selectAllFiltered, 'emit');
    const nextSpy = spyOn(component.next, 'emit');

    const el: HTMLElement = fixture.nativeElement;
    expect(el.textContent).toContain('demo-api');

    (el.querySelector('input[value="export"]') as HTMLInputElement).dispatchEvent(new Event('change'));
    expect(sourceSpy).toHaveBeenCalledWith('export');

    fixture.debugElement.query(By.css('.search-input')).triggerEventHandler('ngModelChange', 'demo');
    expect(searchSpy).toHaveBeenCalledWith('demo');

    el.querySelector('.btn-select-filtered')?.dispatchEvent(new Event('click'));
    expect(selectSpy).toHaveBeenCalled();

    el.querySelector('.btn-primary')?.dispatchEvent(new Event('click'));
    expect(nextSpy).toHaveBeenCalled();
  });
});
