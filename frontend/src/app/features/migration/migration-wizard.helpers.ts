import {
  MigrationPlan,
  MigrationPrerequisite,
  ThreeScaleProduct,
} from '../../core/api/models';
import { PrerequisiteSection } from './steps/wizard-review.types';

export type ProductRow = { product: ThreeScaleProduct; selected: boolean };

export const PREREQUISITE_CATEGORY_ORDER = [
  'connectivity',
  'core-policy',
  'extension',
  'portal',
  'platform',
  'tool-config',
] as const;

export const PREREQUISITE_CATEGORY_LABELS: Record<string, string> = {
  connectivity: 'Connectivity',
  'core-policy': 'Core policies',
  extension: 'Extensions',
  portal: 'Portal',
  platform: 'Platform',
  'tool-config': 'ApiShift configuration',
};

export const GATEWAY_STRATEGIES = [
  {
    value: 'shared',
    icon: '⎈',
    label: 'Shared gateway',
    description:
      'One Gateway for all migrated workloads — fastest to operate and simplest Day-2 footprint.',
  },
  {
    value: 'dual',
    icon: '⇄',
    label: 'Dual gateway',
    description:
      'Split internal and external traffic across two Gateways for stronger blast-radius control.',
  },
  {
    value: 'dedicated',
    icon: '◎',
    label: 'Dedicated per app',
    description:
      'Isolate each application with its own Gateway when you need hard tenancy boundaries.',
  },
] as const;

export function filterProductsByQuery(
  products: ProductRow[],
  query: string,
): ProductRow[] {
  if (!query) return products;
  const q = query.toLowerCase();
  return products.filter(
    (p) =>
      p.product.name.toLowerCase().includes(q) ||
      (p.product.namespace || '').toLowerCase().includes(q) ||
      (p.product.backendNamespace || '').toLowerCase().includes(q) ||
      (p.product.systemName || '').toLowerCase().includes(q),
  );
}

export function pageProducts(
  products: ProductRow[],
  page: number,
  pageSize: number,
): ProductRow[] {
  const start = (page - 1) * pageSize;
  return products.slice(start, start + pageSize);
}

export function totalPages(count: number, pageSize: number): number {
  return Math.max(1, Math.ceil(count / pageSize));
}

export function allFilteredSelected(products: ProductRow[]): boolean {
  return products.length > 0 && products.every((p) => p.selected);
}

export function toggleAllFiltered(products: ProductRow[]): void {
  const target = !allFilteredSelected(products);
  products.forEach((p) => {
    p.selected = target;
  });
}

export function groupPrerequisites(
  prerequisites: MigrationPrerequisite[] | undefined,
): PrerequisiteSection[] {
  if (!prerequisites?.length) return [];
  const grouped = new Map<string, MigrationPrerequisite[]>();
  for (const item of prerequisites) {
    const list = grouped.get(item.category) ?? [];
    list.push(item);
    grouped.set(item.category, list);
  }
  return PREREQUISITE_CATEGORY_ORDER.filter((cat) => grouped.has(cat)).map(
    (cat) => ({
      category: cat,
      label: PREREQUISITE_CATEGORY_LABELS[cat] ?? cat,
      items: grouped.get(cat)!,
    }),
  );
}

export function consumerApiKeySecretCount(plan: MigrationPlan | null): number {
  return plan?.resources?.filter((r) => r.kind === 'Secret').length ?? 0;
}

export function hasOidcJwtAuth(plan: MigrationPlan | null): boolean {
  return (
    plan?.resources?.some(
      (r) => r.kind === 'AuthPolicy' && (r.yaml?.includes('issuerUrl:') ?? false),
    ) ?? false
  );
}

export function isExportProduct(product: ThreeScaleProduct): boolean {
  return (product.source || '').includes('export-v1');
}

export function productsLoadingMessage(
  importing: boolean,
  productSource: 'live' | 'export',
): string {
  if (importing) return 'Importing export archive…';
  if (productSource === 'export') return 'Loading imported products…';
  return 'Loading products from cluster…';
}

export function validateExportFile(file: File | null): string | null {
  if (!file) return 'Select a .zip export archive';
  if (!file.name.toLowerCase().endsWith('.zip')) {
    return 'Only .zip export archives are supported';
  }
  return null;
}

export function extractErrorMessage(err: {
  error?: unknown;
  message?: string;
}): string {
  const body = err?.error;
  if (typeof body === 'string' && body.trim()) return body;
  if (body && typeof body === 'object' && 'message' in body) {
    const message = (body as { message?: string }).message;
    if (message) return message;
  }
  return err?.message || 'Import failed';
}

export function selectedPlanIds(
  planSelection: Record<string, boolean>,
): string[] {
  return Object.entries(planSelection)
    .filter(([, v]) => v)
    .map(([k]) => k);
}

export function allPlansSelected(
  allPlans: MigrationPlan[],
  planSelection: Record<string, boolean>,
): boolean {
  const active = allPlans.filter((p) => p.status !== 'REVERTED');
  return active.length > 0 && active.every((p) => planSelection[p.id]);
}
