import { Routes } from '@angular/router';
import { DashboardComponent } from './features/dashboard/dashboard.component';
import { ThreeScaleExplorerComponent } from './features/threescale-explorer/threescale-explorer.component';
import { MigrationWizardComponent } from './features/migration/migration-wizard.component';
import { ChatComponent } from './features/chat/chat.component';
import { AuditLogComponent } from './features/audit/audit-log.component';
import { SettingsComponent } from './features/settings/settings.component';

export const routes: Routes = [
  { path: '', component: DashboardComponent },
  { path: 'threescale', component: ThreeScaleExplorerComponent },
  { path: 'migrate', component: MigrationWizardComponent },
  { path: 'chat', component: ChatComponent },
  { path: 'audit', component: AuditLogComponent },
  { path: 'settings', component: SettingsComponent },
  { path: '**', redirectTo: '' }
];
