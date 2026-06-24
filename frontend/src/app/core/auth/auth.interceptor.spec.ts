import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AUTH_CONFIG } from './auth.config';
import type { AuthConfig } from './auth.types';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;

  function configureAuth(auth: AuthConfig): void {
    TestBed.configureTestingModule({
      providers: [
        { provide: AUTH_CONFIG, useValue: auth },
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  }

  afterEach(() => httpMock.verify());

  it('doesNotAddHeaderWhenAuthDisabled', () => {
    configureAuth({
      enabled: false,
      bearerToken: 'token',
      basicUsername: 'admin',
      basicPassword: 'admin',
    });

    http.get('/api/migration/plans').subscribe();
    const req = httpMock.expectOne('/api/migration/plans');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush([]);
  });

  it('addsBearerTokenWhenEnabled', () => {
    configureAuth({
      enabled: true,
      bearerToken: 'jwt-token',
      basicUsername: '',
      basicPassword: '',
    });

    http.post('/api/migration/analyze', {}).subscribe();
    const req = httpMock.expectOne('/api/migration/analyze');
    expect(req.request.headers.get('Authorization')).toBe('Bearer jwt-token');
    req.flush({});
  });

  it('addsBasicCredentialsWhenBearerMissing', () => {
    configureAuth({
      enabled: true,
      bearerToken: '',
      basicUsername: 'operator',
      basicPassword: 'operator',
    });

    http.post('/api/threescale/refresh', null).subscribe();
    const req = httpMock.expectOne('/api/threescale/refresh');
    expect(req.request.headers.get('Authorization')).toBe(`Basic ${btoa('operator:operator')}`);
    req.flush({});
  });

  it('prefersBearerOverBasic', () => {
    configureAuth({
      enabled: true,
      bearerToken: 'jwt-token',
      basicUsername: 'admin',
      basicPassword: 'admin',
    });

    http.get('/api/threescale/products').subscribe();
    const req = httpMock.expectOne('/api/threescale/products');
    expect(req.request.headers.get('Authorization')).toBe('Bearer jwt-token');
    req.flush([]);
  });

  it('skipsNonApiRequests', () => {
    configureAuth({
      enabled: true,
      bearerToken: 'jwt-token',
      basicUsername: '',
      basicPassword: '',
    });

    http.get('/assets/config.json').subscribe();
    const req = httpMock.expectOne('/assets/config.json');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
  });

  it('doesNotOverrideExistingAuthorizationHeader', () => {
    configureAuth({
      enabled: true,
      bearerToken: 'jwt-token',
      basicUsername: '',
      basicPassword: '',
    });

    http.get('/api/migration/plans', { headers: { Authorization: 'Bearer existing' } }).subscribe();
    const req = httpMock.expectOne('/api/migration/plans');
    expect(req.request.headers.get('Authorization')).toBe('Bearer existing');
    req.flush([]);
  });
});
