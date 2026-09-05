import { Routes } from '@angular/router';

import { BoardListComponent } from './boards/board-list.component';
import { BoardDetailComponent } from './boards/board-detail.component';

export const routes: Routes = [
  { path: '', component: BoardListComponent },
  { path: 'boards/:id', component: BoardDetailComponent },
  { path: '**', redirectTo: '' }
];
