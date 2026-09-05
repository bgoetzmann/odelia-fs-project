import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import {
  CdkDrag,
  CdkDragDrop,
  CdkDropList,
  CdkDropListGroup,
  moveItemInArray,
  transferArrayItem
} from '@angular/cdk/drag-drop';
import { forkJoin } from 'rxjs';

import { Board, BoardList, Card } from '../models/board';
import { BoardService } from '../services/board.service';

/**
 * Day 2 view: the real Kanban board for one board.
 *
 * Columns come from the backend; each column's cards are dropped into an
 * Angular CDK {@link CdkDropList}. When a card is dropped, the local arrays are
 * updated for an instant response and {@code PATCH /api/cards/{id}/move} is
 * called to persist the new column and order.
 */
@Component({
  selector: 'app-board-detail',
  imports: [FormsModule, RouterLink, CdkDropListGroup, CdkDropList, CdkDrag],
  templateUrl: './board-detail.component.html',
  styleUrl: './board-detail.component.css'
})
export class BoardDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly boardService = inject(BoardService);

  readonly board = signal<Board | null>(null);
  readonly columns = signal<BoardList[]>([]);
  /** Cards keyed by column id. The arrays are mutated in place by the CDK helpers. */
  readonly cardsByList = signal<Record<number, Card[]>>({});
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  /** One "add a card" draft per column, keyed by column id. */
  readonly drafts: Record<number, string> = {};

  private boardId = 0;

  ngOnInit(): void {
    this.boardId = Number(this.route.snapshot.paramMap.get('id'));
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.error.set(null);
    this.boardService.getBoard(this.boardId).subscribe({
      next: board => this.board.set(board),
      error: err => this.fail('Could not load the board', err)
    });
    this.boardService.getLists(this.boardId).subscribe({
      next: columns => {
        this.columns.set(columns);
        this.loadCards(columns);
      },
      error: err => this.fail('Could not load the columns', err)
    });
  }

  cardsFor(listId: number): Card[] {
    return this.cardsByList()[listId] ?? [];
  }

  addCard(column: BoardList): void {
    const title = (this.drafts[column.id!] ?? '').trim();
    if (!title || column.id === undefined) {
      return;
    }
    this.error.set(null);
    this.boardService.createCard(column.id, title).subscribe({
      next: card => {
        this.mutate(column.id!, cards => cards.push(card));
        this.drafts[column.id!] = '';
      },
      error: err => this.fail(`Could not add "${title}"`, err)
    });
  }

  deleteCard(card: Card): void {
    if (card.id === undefined) {
      return;
    }
    this.error.set(null);
    this.boardService.deleteCard(card.id).subscribe({
      next: () => this.mutate(card.listId, cards => {
        const i = cards.findIndex(c => c.id === card.id);
        if (i > -1) {
          cards.splice(i, 1);
        }
      }),
      error: err => this.fail(`Could not delete "${card.title}"`, err)
    });
  }

  drop(event: CdkDragDrop<BoardList>): void {
    const card = event.item.data as Card;
    const sourceId = event.previousContainer.data.id!;
    const targetId = event.container.data.id!;

    if (event.previousContainer === event.container) {
      moveItemInArray(this.cardsFor(targetId), event.previousIndex, event.currentIndex);
    } else {
      transferArrayItem(
        this.cardsFor(sourceId),
        this.cardsFor(targetId),
        event.previousIndex,
        event.currentIndex
      );
      card.listId = targetId;
    }
    this.refresh();

    if (card.id === undefined) {
      return;
    }
    this.boardService.moveCard(card.id, targetId, event.currentIndex).subscribe({
      error: err => {
        this.fail('Could not move the card', err);
        this.reload();
      }
    });
  }

  private loadCards(columns: BoardList[]): void {
    const withId = columns.filter((c): c is BoardList & { id: number } => c.id !== undefined);
    if (withId.length === 0) {
      this.cardsByList.set({});
      this.loading.set(false);
      return;
    }
    forkJoin(withId.map(c => this.boardService.getCards(c.id))).subscribe({
      next: results => {
        const byList: Record<number, Card[]> = {};
        withId.forEach((c, i) => (byList[c.id] = results[i]));
        this.cardsByList.set(byList);
        this.loading.set(false);
      },
      error: err => this.fail('Could not load the cards', err)
    });
  }

  private mutate(listId: number, change: (cards: Card[]) => void): void {
    const cards = this.cardsByList()[listId] ?? [];
    change(cards);
    this.cardsByList.set({ ...this.cardsByList(), [listId]: cards });
  }

  /** Re-emit the record so the template re-renders after an in-place mutation. */
  private refresh(): void {
    this.cardsByList.set({ ...this.cardsByList() });
  }

  private fail(message: string, err: unknown): void {
    console.error(message, err);
    this.error.set(message);
    this.loading.set(false);
  }
}
