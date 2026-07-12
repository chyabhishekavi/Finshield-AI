export const environment = {
  production: true,
  apiBaseUrl: '/api',
  websocketUrl: `${location.protocol === 'https:' ? 'wss' : 'ws'}://${location.host}/ws`,
  useMockApi: false,
};
