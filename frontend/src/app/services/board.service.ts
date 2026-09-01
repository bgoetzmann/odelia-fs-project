import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';
import { Board, BoardList } from '../models/board';

/**
 * Single entry point to the Kanban REST API.
 *
 * During development `environment.apiUrl` is `/api` and requests are forwarded
 * to OpenLiberty on port 9080 by proxy.conf.json, so the browser sees a single
 * origin and no CORS pre-flight.
 */
@Injectable({ providedIn: 'root' })
export class BoardService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/boards`;

  getBoards(): Observable<Board[]> {
    return this.http.get<Board[]>(this.baseUrl);
  }

  getBoard(id: number): Observable<Board> {
    return this.http.get<Board>(`${this.baseUrl}/${id}`);
  }

  createBoard(name: string): Observable<Board> {
    return this.http.post<Board>(this.baseUrl, { name });
  }

  renameBoard(id: number, name: string): Observable<Board> {
    return this.http.put<Board>(`${this.baseUrl}/${id}`, { name });
  }

  deleteBoard(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  getLists(boardId: number): Observable<BoardList[]> {
    return this.http.get<BoardList[]>(`${this.baseUrl}/${boardId}/lists`);
  }
}
