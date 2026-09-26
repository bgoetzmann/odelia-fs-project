import { Component, OnInit, computed, inject, signal } from '@angular/core';

import { AuthService } from '../auth/auth.service';
import { CurrentUser } from '../models/current-user';
import { CurrentUserService } from '../services/current-user.service';

/**
 * Account panel: what the backend says about the caller (`GET /api/me`, read
 * straight from the validated JWT) next to what keycloak-js already parsed out
 * of the same token client-side. The two are derived independently, so this is
 * also a live check that they agree.
 */
@Component({
  selector: 'app-account',
  templateUrl: './account.component.html',
  styleUrl: './account.component.css'
})
export class AccountComponent implements OnInit {
  private readonly currentUserService = inject(CurrentUserService);

  readonly auth = inject(AuthService);
  readonly backendUser = signal<CurrentUser | null>(null);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  /** Roles Keycloak put in the token but the backend's `groups` claim mapper did not carry over. */
  readonly frontendOnlyRoles = computed(() => this.rolesOnlyIn(this.auth.roles(), this.backendUser()?.roles ?? []));

  /** Roles the backend reports that keycloak-js's `realm_access.roles` does not include. */
  readonly backendOnlyRoles = computed(() => this.rolesOnlyIn(this.backendUser()?.roles ?? [], this.auth.roles()));

  readonly usernameMatches = computed(() => this.backendUser()?.username === this.auth.username());
  readonly adminMatches = computed(() => this.backendUser()?.admin === this.auth.isAdmin());

  ngOnInit(): void {
    this.loading.set(true);
    this.error.set(null);
    this.currentUserService.getCurrentUser().subscribe({
      next: user => {
        this.backendUser.set(user);
        this.loading.set(false);
      },
      error: err => {
        console.error('Could not load /api/me', err);
        this.error.set('Could not load the backend identity');
        this.loading.set(false);
      }
    });
  }

  private rolesOnlyIn(a: string[], b: string[]): string[] {
    return a.filter(role => !b.includes(role));
  }
}
