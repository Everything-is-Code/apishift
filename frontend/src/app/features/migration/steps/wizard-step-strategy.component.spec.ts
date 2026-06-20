import { ComponentFixture, TestBed } from '@angular/core/testing';
import { WizardStepStrategyComponent } from './wizard-step-strategy.component';

describe('WizardStepStrategyComponent', () => {
  let fixture: ComponentFixture<WizardStepStrategyComponent>;

  const strategies = [
    { value: 'shared', icon: 'S', label: 'Shared', description: 'One gateway' },
    { value: 'dual', icon: 'D', label: 'Dual', description: 'Two gateways' },
  ];

  const clusters = [
    {
      id: 'local',
      label: 'Local',
      apiServerUrl: '',
      authType: 'in-cluster',
      verifySsl: true,
      enabled: true,
      credentialConfigured: false,
    },
    {
      id: 'remote',
      label: 'Remote',
      apiServerUrl: 'https://api.example.com',
      authType: 'token',
      verifySsl: true,
      enabled: true,
      credentialConfigured: true,
    },
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [WizardStepStrategyComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(WizardStepStrategyComponent);
    fixture.componentRef.setInput('strategies', strategies);
    fixture.componentRef.setInput('gatewayStrategy', 'shared');
    fixture.componentRef.setInput('targetClusters', clusters);
    fixture.componentRef.setInput('selectedClusterId', 'local');
  });

  it('rendersStrategiesAndClusterSelector', () => {
    fixture.detectChanges();

    const el: HTMLElement = fixture.nativeElement;
    expect(el.textContent).toContain('Shared');
    expect(el.textContent).toContain('Target cluster');
    expect(el.textContent).toContain('Remote');
  });

  it('emitsStrategyClusterBackAndAnalyze', () => {
    fixture.detectChanges();
    const component = fixture.componentInstance;
    const strategySpy = spyOn(component.strategyChange, 'emit');
    const clusterSpy = spyOn(component.clusterChange, 'emit');
    const backSpy = spyOn(component.back, 'emit');
    const analyzeSpy = spyOn(component.analyze, 'emit');

    const strategyInput = fixture.nativeElement.querySelector(
      'input[name="gatewayStrategy"][value="dual"]',
    ) as HTMLInputElement;
    strategyInput.dispatchEvent(new Event('change'));
    expect(strategySpy).toHaveBeenCalledWith('dual');

    const clusterInput = fixture.nativeElement.querySelector(
      'input[name="targetCluster"][value="remote"]',
    ) as HTMLInputElement;
    clusterInput.dispatchEvent(new Event('change'));
    expect(clusterSpy).toHaveBeenCalledWith('remote');

    fixture.nativeElement.querySelector('.btn-secondary')?.dispatchEvent(new Event('click'));
    expect(backSpy).toHaveBeenCalled();

    fixture.nativeElement.querySelector('.btn-primary')?.dispatchEvent(new Event('click'));
    expect(analyzeSpy).toHaveBeenCalled();
  });
});
