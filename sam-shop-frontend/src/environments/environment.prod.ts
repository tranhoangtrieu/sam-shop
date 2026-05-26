export const environment = {
  production: true,
  // Relative URL — nginx proxies /api/ to gateway; /api/chat/ws to chat-service
  apiUrl: '',
  chatWsUrl: '',
  keycloak: {
    // Replaced at Docker build from ARG KEYCLOAK_PUBLIC_URL (see Dockerfile)
    url: '__KEYCLOAK_PUBLIC_URL__',
    realm: 'sam-shop',
    clientId: 'sam-shop-ui'
  }
};
