import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute } from '@angular/router';

import { BoardDetailComponent } from './board-detail.component';

describe('BoardDetailComponent', () => {
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BoardDetailComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: new Map([['id', '7']]) } } }
      ]
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('loads the board and its columns on init', () => {
    const fixture = TestBed.createComponent(BoardDetailComponent);
    fixture.detectChanges();

    http.expectOne('/api/boards/7').flush({ id: 7, name: 'Sprint 1' });

    const listsReq = http.expectOne('/api/boards/7/lists');
    listsReq.flush([{ id: 1, boardId: 7, name: 'To do', position: 0 }]);

    http.expectOne('/api/lists/1/cards').flush([
      { id: 10, listId: 1, title: 'First card', position: 0 }
    ]);

    expect(fixture.componentInstance.board()?.name).toBe('Sprint 1');
    expect(fixture.componentInstance.cardsFor(1).length).toBe(1);
  });

  it('PATCHes the card when it is dropped into another column', () => {
    const fixture = TestBed.createComponent(BoardDetailComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    http.expectOne('/api/boards/7').flush({ id: 7, name: 'Sprint 1' });
    http.expectOne('/api/boards/7/lists').flush([
      { id: 1, boardId: 7, name: 'To do', position: 0 },
      { id: 2, boardId: 7, name: 'Doing', position: 1 }
    ]);
    http.expectOne('/api/lists/1/cards').flush([{ id: 10, listId: 1, title: 'Card', position: 0 }]);
    http.expectOne('/api/lists/2/cards').flush([]);

    const card = component.cardsFor(1)[0];
    component.drop({
      item: { data: card },
      container: { data: { id: 2 } },
      previousContainer: { data: { id: 1 } },
      previousIndex: 0,
      currentIndex: 0
    } as never);

    const move = http.expectOne('/api/cards/10/move');
    expect(move.request.method).toBe('PATCH');
    expect(move.request.body).toEqual({ targetListId: 2, position: 0 });
    move.flush({ id: 10, listId: 2, title: 'Card', position: 0 });

    expect(component.cardsFor(1).length).toBe(0);
    expect(component.cardsFor(2).length).toBe(1);
  });
});
