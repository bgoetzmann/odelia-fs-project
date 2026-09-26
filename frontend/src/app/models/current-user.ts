/** Mirrors com.odelia.kanban.resource.CurrentUserResponse: GET /api/me. */
export interface CurrentUser {
  username: string;
  roles: string[];
  admin: boolean;
}
