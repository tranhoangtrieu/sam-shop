export const environment = {
  production: false,
  apiUrl: 'http://localhost:8088',
  /** REST via gateway; WebSocket direct to chat-service (gateway does not proxy WS) */
  chatWsUrl: 'ws://localhost:8085',
  keycloak: {
    url: 'http://localhost:8180',
    realm: 'sam-shop',
    clientId: 'sam-shop-ui'
  }
};
