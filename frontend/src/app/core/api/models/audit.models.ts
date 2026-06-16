export interface AuditEntry {
  id: string;
  timestamp: string;
  action: string;
  resourceKind: string;
  resourceName: string;
  namespace: string;
  yamlBefore: string;
  yamlAfter: string;
  performedBy: string;
  targetClusterId?: string;
}
