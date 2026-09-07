import { Injectable, computed, signal } from '@angular/core';
import Keycloak from 'keycloak-js';

import { environment } from '../../environments/environment';

/**
 * Everything the app knows about the signed-in user, on top of keycloak-js.
 *
 * <p>The OpenID Connect flow used here is <em>authorization code + PKCE</em>:
 * the browser is redirected to Keycloak, comes back with a one-time code, and
 * keycloak-js exchanges it for tokens. No secret is involved, which is why the
 * `kanban-app` client is declared "public" in the realm.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly keycloak = new Keycloak(environment.keycloak);

  readonly authenticated = signal(false);
  readonly username = signal<string | null>(null);
  readonly roles = signal<string[]>([]);
  readonly isAdmin = computed(() => this.roles().includes('admin'));

  /**
   * Runs once, before the app is bootstrapped (see `provideAppInitializer` in
   * `app.config.ts`).
   *
   * `check-sso` asks Keycloak whether a session already exists and comes
   * straight back either way — it never shows the login page by itself. That is
   * `authGuard`'s job, so an unauthenticated visitor is redirected only when
   * they actually ask for a protected route.
   */
  async init(): Promise<void> {
    try {
      await this.keycloak.init({
        onLoad: 'check-sso',
        pkceMethod: 'S256',
        // No hidden iframe: browsers block its cookies across ports anyway.
        checkLoginIframe: false
      });
    } catch (err) {
      // A Keycloak that is down must not take the whole app down with it: the
      // guard will simply keep every route closed.
      console.error('Keycloak initialisation failed', err);
    }
    this.sync();
  }

  /** Sends the browser to the Keycloak login page, then back to `redirectUri`. */
  login(redirectUri: string = window.location.href): Promise<void> {
    return this.keycloak.login({ redirectUri });
  }

  /** Ends the Keycloak session, not just the local one, and returns to the app. */
  logout(): Promise<void> {
    return this.keycloak.logout({ redirectUri: window.location.origin });
  }

  /**
   * The access token for the `Authorization` header, refreshed when it has less
   * than 30 seconds to live. Null when nobody is signed in.
   */
  async token(): Promise<string | null> {
    if (!this.keycloak.authenticated) {
      return null;
    }
    try {
      await this.keycloak.updateToken(30);
    } catch {
      // The refresh token has expired too — nothing left but a fresh login.
      this.sync();
      await this.login();
      return null;
    }
    this.sync();
    return this.keycloak.token ?? null;
  }

  /** Mirrors the keycloak-js state into signals so templates can react to it. */
  private sync(): void {
    const claims = this.keycloak.tokenParsed;
    this.authenticated.set(this.keycloak.authenticated ?? false);
    this.username.set(claims?.['preferred_username'] ?? null);
    this.roles.set(claims?.realm_access?.roles ?? []);
  }
}
