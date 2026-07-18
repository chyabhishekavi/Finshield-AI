# FinShield UI

Angular 20 LTS frontend for FinShield AI.

## Development

```bash
npm install
npm start
```

The application runs on `http://localhost:4200` and expects the backend at `http://localhost:8080`.

## Structure

- `core` — authentication, guards, HTTP interceptors, singleton services and models
- `shared` — reusable presentational components
- `auth`, `dashboard`, `customers`, `transactions`, `alerts`, `cases`, `aml`, `rules`, `audit` — lazy feature boundaries
- `layout` — authenticated application shell
