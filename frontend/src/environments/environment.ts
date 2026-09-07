export const environment = {
  production: false,
  /** Relative on purpose: proxy.conf.json forwards /api to OpenLiberty. */
  apiUrl: '/api',
  /**
   * Absolute on purpose: the browser is redirected to Keycloak, so this URL has
   * to be reachable from the browser (localhost:8081), not from the backend
   * container (which talks to keycloak:8080 instead).
   */
  keycloak: {
    url: 'http://localhost:8081',
    realm: 'kanban',
    clientId: 'kanban-app'
  }
};
