import { TestBed } from '@angular/core/testing';
import { buildAuthorizationHeader, resolveAuthConfig } from './auth.config';
import { authBuildConfig } from '../../../environments/auth';

describe('auth.config', () => {
  afterEach(() => {
    sessionStorage.clear();
    delete window.__APISHIFT_AUTH__;
  });

  it('resolveAuthConfig_usesBuildDefaults', () => {
    expect(resolveAuthConfig()).toEqual(authBuildConfig);
  });

  it('resolveAuthConfig_mergesWindowOverride', () => {
    window.__APISHIFT_AUTH__ = {
      enabled: true,
      basicUsername: 'admin',
      basicPassword: 'admin',
    };

    expect(resolveAuthConfig()).toEqual({
      enabled: true,
      bearerToken: '',
      basicUsername: 'admin',
      basicPassword: 'admin',
    });
  });

  it('resolveAuthConfig_mergesSessionStorage', () => {
    sessionStorage.setItem('apishift.auth.enabled', 'true');
    sessionStorage.setItem('apishift.oidc.accessToken', 'from-session');

    expect(resolveAuthConfig()).toEqual({
      enabled: true,
      bearerToken: 'from-session',
      basicUsername: '',
      basicPassword: '',
    });
  });

  it('buildAuthorizationHeader_prefersBearer', () => {
    expect(
      buildAuthorizationHeader({
        enabled: true,
        bearerToken: 'abc',
        basicUsername: 'admin',
        basicPassword: 'admin',
      }),
    ).toBe('Bearer abc');
  });

  it('buildAuthorizationHeader_buildsBasic', () => {
    expect(
      buildAuthorizationHeader({
        enabled: true,
        bearerToken: '',
        basicUsername: 'admin',
        basicPassword: 'admin',
      }),
    ).toBe(`Basic ${btoa('admin:admin')}`);
  });
});
