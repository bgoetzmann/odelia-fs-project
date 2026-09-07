import { Routes } from '@angular/router';

import { authGuard } from './auth/auth.guard';
import { BoardListComponent } from './boards/board-list.component';
import { BoardDetailComponent } from './boards/board-detail.component';

export const routes: Routes = [
  { path: '', component: BoardListComponent, canActivate: [authGuard] },
  { path: 'boards/:id', component: BoardDetailComponent, canActivate: [authGuard] },
  { path: '**', redirectTo: '' }
];
