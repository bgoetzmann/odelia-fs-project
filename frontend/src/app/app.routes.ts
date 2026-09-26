import { Routes } from '@angular/router';

import { AccountComponent } from './account/account.component';
import { authGuard } from './auth/auth.guard';
import { BoardListComponent } from './boards/board-list.component';
import { BoardDetailComponent } from './boards/board-detail.component';

export const routes: Routes = [
  { path: '', component: BoardListComponent, canActivate: [authGuard] },
  { path: 'boards/:id', component: BoardDetailComponent, canActivate: [authGuard] },
  { path: 'account', component: AccountComponent, canActivate: [authGuard] },
  { path: '**', redirectTo: '' }
];
