import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { Board, BoardList } from '../models/board';
import { BoardService } from '../services/board.service';

/**
 * Day 1 view: list the boards, create one, delete one, and peek at the columns
 * a board contains. Day 2 replaces the "peek" with the real drag-and-drop board.
 */
@Component({
  selector: 'app-board-list',
  imports: [FormsModule, DatePipe],
  templateUrl: './board-list.component.html',
  styleUrl: './board-list.component.css'
})
export class BoardListComponent implements OnInit {
  private readonly boardService = inject(BoardService);

  readonly boards = signal<Board[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  readonly selectedBoardId = signal<number | null>(null);
  readonly columns = signal<BoardList[]>([]);

  newBoardName = '';

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.error.set(null);
    this.boardService.getBoards().subscribe({
      next: boards => {
        this.boards.set(boards);
        this.loading.set(false);
      },
      error: err => this.fail('Could not load the boards', err)
    });
  }

  createBoard(): void {
    const name = this.newBoardName.trim();
    if (!name) {
      return;
    }
    this.error.set(null);
    this.boardService.createBoard(name).subscribe({
      next: board => {
        this.boards.update(boards => [...boards, board].sort((a, b) => a.name.localeCompare(b.name)));
        this.newBoardName = '';
      },
      error: err => this.fail(`Could not create the board "${name}"`, err)
    });
  }

  deleteBoard(board: Board): void {
    if (board.id === undefined) {
      return;
    }
    this.error.set(null);
    this.boardService.deleteBoard(board.id).subscribe({
      next: () => {
        this.boards.update(boards => boards.filter(b => b.id !== board.id));
        if (this.selectedBoardId() === board.id) {
          this.selectedBoardId.set(null);
          this.columns.set([]);
        }
      },
      error: err => this.fail(`Could not delete the board "${board.name}"`, err)
    });
  }

  toggleColumns(board: Board): void {
    if (board.id === undefined) {
      return;
    }
    if (this.selectedBoardId() === board.id) {
      this.selectedBoardId.set(null);
      this.columns.set([]);
      return;
    }
    this.selectedBoardId.set(board.id);
    this.columns.set([]);
    this.boardService.getLists(board.id).subscribe({
      next: columns => this.columns.set(columns),
      error: err => this.fail(`Could not load the columns of "${board.name}"`, err)
    });
  }

  private fail(message: string, err: unknown): void {
    console.error(message, err);
    this.error.set(message);
    this.loading.set(false);
  }
}
