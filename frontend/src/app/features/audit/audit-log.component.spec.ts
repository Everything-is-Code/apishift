import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { AuditLogComponent } from './audit-log.component';
import { AuditApiService } from '../../core/api/audit-api.service';
import { AuditEntry } from '../../core/api/models';

const mockEntry: AuditEntry = {
  id: 'entry-1',
  action: 'create',
  timestamp: '2024-01-01T12:00:00Z',
  resourceKind: 'Gateway',
  resourceName: 'shared-gw',
  namespace: 'apps',
  performedBy: 'gateforge',
  targetClusterId: 'local',
  yamlBefore: '',
  yamlAfter: 'kind: Gateway',
};

describe('AuditLogComponent', () => {
  let fixture: ComponentFixture<AuditLogComponent>;
  let auditApi: jasmine.SpyObj<AuditApiService>;

  beforeEach(async () => {
    auditApi = jasmine.createSpyObj<AuditApiService>('AuditApiService', ['getReports']);
    auditApi.getReports.and.returnValue(of([mockEntry]));

    await TestBed.configureTestingModule({
      imports: [AuditLogComponent],
      providers: [{ provide: AuditApiService, useValue: auditApi }],
    }).compileComponents();

    fixture = TestBed.createComponent(AuditLogComponent);
  });

  it('loadsEntriesOnInit', () => {
    fixture.detectChanges();

    expect(auditApi.getReports).toHaveBeenCalled();
    expect(fixture.componentInstance.entries.length).toBe(1);
    expect(fixture.componentInstance.loading).toBe(false);
    expect(fixture.nativeElement.textContent).toContain('shared-gw');
  });

  it('showsEmptyStateWhenNoEntries', () => {
    auditApi.getReports.and.returnValue(of([]));
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('No audit entries yet');
  });

  it('handlesLoadError', () => {
    auditApi.getReports.and.returnValue(throwError(() => new Error('failed')));
    fixture.detectChanges();

    expect(fixture.componentInstance.loading).toBe(false);
    expect(fixture.componentInstance.entries.length).toBe(0);
  });

  it('toggleYaml_expandsAndCollapses', () => {
    fixture.detectChanges();

    fixture.componentInstance.toggleYaml('entry-1');
    expect(fixture.componentInstance.expanded['entry-1']).toBe(true);

    fixture.componentInstance.toggleYaml('entry-1');
    expect(fixture.componentInstance.expanded['entry-1']).toBe(false);
  });

  it('toggleYaml_rendersYamlBlocks', () => {
    auditApi.getReports.and.returnValue(of([{ ...mockEntry, yamlBefore: 'before', yamlAfter: 'after' }]));
    fixture.detectChanges();

    fixture.componentInstance.toggleYaml('entry-1');
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('before');
  });

  it('toggleYaml_showsNoPayloadMessageWhenEmpty', () => {
    auditApi.getReports.and.returnValue(of([{ ...mockEntry, yamlBefore: '', yamlAfter: '' }]));
    fixture.detectChanges();
    fixture.componentInstance.toggleYaml('entry-1');
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('No YAML payload recorded');
  });

  it('actionTone_mapsCreateUpdateDelete', () => {
    const component = fixture.componentInstance;
    expect(component.actionTone('create')).toBe('tone-create');
    expect(component.actionTone('patch')).toBe('tone-update');
    expect(component.actionTone('delete')).toBe('tone-delete');
    expect(component.actionTone('unknown')).toBe('tone-neutral');
  });
});
