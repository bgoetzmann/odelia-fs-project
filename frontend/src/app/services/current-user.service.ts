import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';
import { CurrentUser } from '../models/current-user';

@Injectable({ providedIn: 'root' })
export class CurrentUserService {
  private readonly http = inject(HttpClient);
  private readonly meUrl = `${environment.apiUrl}/me`;

  getCurrentUser(): Observable<CurrentUser> {
    return this.http.get<CurrentUser>(this.meUrl);
  }
}
