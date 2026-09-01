/** Mirrors com.odelia.kanban.entity.Board on the backend. */
export interface Board {
  id?: number;
  name: string;
  owner?: string;
  createdAt?: string;
}

/** Mirrors com.odelia.kanban.entity.BoardList: a column of a board. */
export interface BoardList {
  id?: number;
  boardId: number;
  name: string;
  position: number;
}
