import { InjectionToken } from '@angular/core';
import type { AuthConfig } from './auth.types';
import { authBuildConfig } from '../../../environments/auth';

const SESSION_KEYS = {
  bearer: 'apishift.oidc.accessToken',
  enabled: 'apishift.auth.enabled',
  basicUser: 'apishift.auth.basicUser',
  basicPass: 'apishift.auth.basicPassword',
} as const;

function readSession(key: string): string {
  if (typeof sessionStorage === 'undefined') {
    return '';
  }
  return sessionStorage.getItem(key) ?? '';
}

function readSessionFlag(key: string): boolean | undefined {
  const value = readSession(key);
  if (value === 'true') {
    return true;
  }
  if (value === 'false') {
    return false;
  }
  return undefined;
}

function coalesceString(...values: (string | undefined)[]): string {
  for (const value of values) {
    if (value !== undefined && value !== '') {
      return value;
    }
  }
  return '';
}

function coalesceBoolean(...values: (boolean | undefined)[]): boolean {
  for (const value of values) {
    if (value !== undefined) {
      return value;
    }
  }
  return false;
}

/** Merge build-time, runtime (`window.__APISHIFT_AUTH__`), and sessionStorage auth settings. */
export function resolveAuthConfig(): AuthConfig {
  const runtime = typeof window !== 'undefined' ? window.__APISHIFT_AUTH__ : undefined;

  return {
    enabled: coalesceBoolean(runtime?.enabled, readSessionFlag(SESSION_KEYS.enabled), authBuildConfig.enabled),
    bearerToken: coalesceString(runtime?.bearerToken, readSession(SESSION_KEYS.bearer), authBuildConfig.bearerToken),
    basicUsername: coalesceString(
      runtime?.basicUsername,
      readSession(SESSION_KEYS.basicUser),
      authBuildConfig.basicUsername,
    ),
    basicPassword: coalesceString(
      runtime?.basicPassword,
      readSession(SESSION_KEYS.basicPass),
      authBuildConfig.basicPassword,
    ),
  };
}

export function buildAuthorizationHeader(config: AuthConfig): string | undefined {
  const bearer = config.bearerToken.trim();
  if (bearer) {
    return `Bearer ${bearer}`;
  }
  if (config.basicUsername && config.basicPassword) {
    return `Basic ${btoa(`${config.basicUsername}:${config.basicPassword}`)}`;
  }
  return undefined;
}

export const AUTH_CONFIG = new InjectionToken<AuthConfig>('AUTH_CONFIG', {
  providedIn: 'root',
  factory: () => resolveAuthConfig(),
});
