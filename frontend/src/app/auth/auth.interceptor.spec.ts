import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed, fakeAsync, tick } from '@angular/core/testing';

import { authInterceptor } from './auth.interceptor';
import { AuthService } from './auth.service';

describe('authInterceptor', () => {
  let http: HttpClient;
  let backend: HttpTestingController;

  /** Stands in for AuthService so the tests never touch keycloak-js. */
  function configure(token: string | null): void {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { token: () => Promise.resolve(token) } }
      ]
    });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
  }

  afterEach(() => backend.verify());

  it('attaches the bearer token to API calls', fakeAsync(() => {
    configure('a-token');

    http.get('/api/boards').subscribe();
    tick(); // the interceptor awaits AuthService.token() before forwarding

    const req = backend.expectOne('/api/boards');
    expect(req.request.headers.get('Authorization')).toBe('Bearer a-token');
    req.flush([]);
  }));

  it('sends no Authorization header when nobody is signed in', fakeAsync(() => {
    configure(null);

    http.get('/api/boards').subscribe();
    tick();

    const req = backend.expectOne('/api/boards');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush([]);
  }));

  it('leaves calls to other hosts alone, so the token cannot leak', fakeAsync(() => {
    configure('a-token');

    http.get('http://localhost:8081/realms/kanban').subscribe();
    tick();

    const req = backend.expectOne('http://localhost:8081/realms/kanban');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
  }));
});
