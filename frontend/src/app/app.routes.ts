import { Routes } from '@angular/router';

import { BoardListComponent } from './boards/board-list.component';

export const routes: Routes = [
  { path: '', component: BoardListComponent },
  { path: '**', redirectTo: '' }
];
