# FinShield Backend

Spring Boot backend for real-time fraud detection, AML monitoring, and case investigation.

## Requirements

- Java 17
- Maven 3.9+

## Run locally

```bash
mvn spring-boot:run
```

The `dev` profile is active by default and uses an in-memory H2 database in PostgreSQL compatibility mode.

- Swagger UI: http://localhost:8080/swagger-ui.html
- H2 console: http://localhost:8080/h2-console
- Health: http://localhost:8080/actuator/health

H2 JDBC URL: `jdbc:h2:mem:finshield`

## Run the complete Docker stack

From the repository root:

```bash
cp .env.example .env
docker compose up --build
```

The Compose stack runs PostgreSQL, Redis, Kafka, ZooKeeper, and this backend. Replace the example passwords in `.env` before starting it.

## Real-time notifications

The backend exposes a raw STOMP WebSocket endpoint at `ws://localhost:8080/ws`.
Send the JWT in the STOMP `CONNECT` headers and subscribe to the private user destination:

```typescript
const client = new Client({
  brokerURL: 'ws://localhost:8080/ws',
  connectHeaders: { Authorization: `Bearer ${accessToken}` },
  reconnectDelay: 5000,
});

client.onConnect = () => {
  client.subscribe('/user/queue/notifications', frame => {
    const notification = JSON.parse(frame.body);
  });
};

client.activate();
```

Configure allowed frontend origins with `WEBSOCKET_ALLOWED_ORIGINS`. PostgreSQL notification APIs remain the fallback for missed messages.
