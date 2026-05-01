# FinShield AI

**Real-time fraud detection, AML monitoring, and investigation casework in one operational platform.**

FinShield follows a payment from ingestion to a decision. It combines configurable fraud rules, short-window velocity checks, a Python fraud model, customer/device risk, and AML behavior signals; then it gives analysts the alert queue and evidence trail needed to act on that decision.

This is deliberately not a CRUD demo wearing a banking theme. The interesting work happens between receiving a transaction and explaining why it was approved, monitored, alerted, or held.

## What problem it solves

A bank rarely has a single “fraud system.” Transaction monitoring, sanctions screening, AML investigations, notifications, and audit evidence tend to live in separate tools. That fragmentation slows an analyst down and makes a decision hard to defend later.

FinShield keeps those concerns connected:

- transactions enter through an idempotent ingestion boundary and are processed asynchronously;
- risk combines rules, ML probability, customer context, device trust, and AML behavior;
- every matched rule and score component is retained as an explanation;
- high-risk decisions create an alert, an urgent notification, and optionally a case;
- analysts can work an alert through assignment and closure, or convert it into structured casework;
- AML reviewers can screen customers and beneficiaries using exact and fuzzy name matching;
- significant actions are written to an audit trail.

## Architecture

```text
Angular operations console
        │  REST / JWT                    WebSocket notifications
        ▼                                       ▲
Spring Boot API ───── PostgreSQL                 │
        │              transactions, scores, alerts, cases, audit
        ├──── Redis    rolling velocity counters with TTL
        │
        ├──── Kafka: transaction.incoming
        │              │
        │              ▼
        │        risk scoring pipeline ─── FastAPI ML service
        │              │                   Random Forest + reasons
        │              ▼
        └──── Kafka: transaction.risk.scored
                       │
                       └── alert/case/notification workflow
                              └── Kafka: fraud.alert.created
```

The synchronous ingestion request only validates and records the transaction before publishing its reference. Scoring happens in a Kafka consumer. This keeps API latency predictable and leaves a durable hand-off point for retries. Kafka producers are idempotent, failed consumer records are recoverable through dead-letter topics, and alert publication uses a database outbox so a committed alert is not silently lost between PostgreSQL and Kafka.

## Risk decision model

The final score is normalized to 0–100:

| Signal | Weight | Examples |
|---|---:|---|
| Configurable rule score | 45% | high amount, new device, velocity, beneficiary, location |
| ML fraud probability | 35% | behavioral transaction features from the FastAPI model |
| Customer risk | 10% | maintained customer risk tier |
| Device risk | 5% | trust and suspicious attempt history |
| AML risk | 5% | structuring, mule behavior, rapid movement, risky country, round amounts |

| Score | Band | Typical decision |
|---:|---|---|
| 0–30 | Low | `APPROVE` |
| 31–60 | Medium | `MONITOR` |
| 61–80 | High | `CREATE_ALERT` |
| 81–100 | Critical | `HOLD_AND_ESCALATE` |

The ML service is intentionally non-blocking to the control path. If it times out or is unavailable, scoring continues with an ML contribution of zero and records `ML service unavailable` in the explanation. In a real bank that fallback policy would be configurable by product, channel, and outage severity.

## Repository layout

```text
finshield-backend/     Java 17 / Spring Boot API and event consumers
finshield-ml-service/  FastAPI inference API, training script, saved model assets
finshield-ui/          Angular analyst and compliance console
docker-compose.yml     PostgreSQL, Redis, ZooKeeper, Kafka, backend, ML service, Angular/nginx
.env.example           local configuration contract
```

### Backend modules

| Module | Responsibility |
|---|---|
| `auth`, `user` | JWT login, BCrypt credentials, current user, role-based access |
| `customer`, `account` | KYC profile, accounts, customer risk history, Customer 360 |
| `beneficiary`, `device` | payee risk and known/trusted device context |
| `transaction` | ingestion, search, lifecycle, Kafka events |
| `risk` | orchestration, weighted score, rule matches, explanations, Redis velocity |
| `fraud` | database-backed rules, alerts, SLA workflow, alert event outbox |
| `aml` | watchlists, exact/fuzzy screening, behavior pattern detection |
| `casework` | investigation cases, notes, evidence metadata, status history |
| `notification` | durable inbox plus critical WebSocket push |
| `audit` | actor, action, entity, before/after values, IP, timestamp |
| `dashboard` | chart-ready operational summaries and trends |

### Frontend workspaces

The UI is organized around analyst tasks: live dashboard, transaction monitor with risk explanation, fraud alert queue, case investigation workspace, Customer 360, fraud rule administration, AML review, notifications, and audit review. Routes and navigation are role-aware; API authorization remains the source of truth.

## Technology

- **Backend:** Java 17, Spring Boot 3.4, Spring Security, Spring Data JPA, Bean Validation, WebFlux `WebClient`, Spring Kafka, Spring Data Redis, Actuator, springdoc OpenAPI
- **Data and messaging:** PostgreSQL 16, Redis 7, Kafka/ZooKeeper
- **ML:** Python, FastAPI, scikit-learn, pandas, Pydantic, joblib, Uvicorn
- **Frontend:** Angular 20, Angular Material, RxJS, reactive forms, Chart.js
- **Local operations:** Docker Compose and service-level health checks

## Start and check the complete project

You need Docker Desktop with Compose v2, Java 17, Maven, Node.js, and npm. On Windows PowerShell, run these commands from the repository root.

For the first run, create the local environment file:

```powershell
Copy-Item .env.example .env
```

Open `.env` and replace the placeholder PostgreSQL password, Redis password, and JWT secret. Keep the JWT secret at least 32 characters long.

Stop the old stack and start every service again:

```powershell
docker compose down
docker compose up --build -d
docker compose ps
```

Every row shown by `docker compose ps` should say `healthy`. The stack includes PostgreSQL, Redis, ZooKeeper, Kafka, the Spring Boot backend, the ML service, and the Angular UI.

Build and test the backend separately:

```powershell
Set-Location finshield-backend
mvn clean install
Set-Location ..
```

Install the frontend packages and create a production build:

```powershell
Set-Location finshield-ui
npm install
npm run build
Set-Location ..
```

Check the running services:

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
Invoke-RestMethod http://localhost:8000/health
Start-Process http://localhost:4200
```

The backend result should contain `UP`. The ML result should contain `"status": "ok"` and `"modelLoaded": true`.

Local test logins are created automatically:

| Person to test as | Email | Password |
|---|---|---|
| Administrator | `admin@finshield.ai` | `Admin@123` |
| Fraud analyst | `analyst@finshield.ai` | `Analyst@123` |
| AML investigator | `aml@finshield.ai` | `Aml@123` |
| Compliance officer | `compliance@finshield.ai` | `Compliance@123` |

Use the administrator account for the full smoke test. These are local test accounts, not production credentials.

The local seed data includes customer `CUST1001`, source account `ACC1001`, trusted beneficiary account `ACC2002`, risky beneficiary account `ACC9999`, trusted device `DEVICE-TRUSTED-001`, and untrusted device `DEVICE-NEW-001`.

Use [scripts/test-low-risk-transaction.http](scripts/test-low-risk-transaction.http) for an approved transfer. It should finish as `LOW / APPROVE` and should not create an alert. Use [scripts/test-high-risk-transaction.http](scripts/test-high-risk-transaction.http) for the alert path. It should finish as `HIGH / CREATE_ALERT` and create the score, rule matches, alert, notification, and audit entry.

Open these addresses after the checks:

- Application: `http://localhost:4200`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Backend health: `http://localhost:8080/actuator/health`
- ML health: `http://localhost:8000/health`

To stop the project without deleting its data, run `docker compose down`. Use `docker compose down -v` only when you deliberately want to erase the local databases and message data.

## Run services during development

### Backend with H2

The `dev` profile uses an in-memory H2 database in PostgreSQL compatibility mode. Kafka and Redis still need to be reachable locally.

```bash
cd finshield-backend
mvn spring-boot:run
```

The development JWT secret is provided by the profile; never carry it into another environment.

### ML service

```bash
cd finshield-ml-service
python -m venv .venv
# Windows: .venv\Scripts\activate
# macOS/Linux: source .venv/bin/activate
pip install -r requirements.txt
python scripts/train_fraud_model.py --output models/fraud_model.joblib
uvicorn app.main:app --reload --port 8000
```

The training job creates reproducible, intentionally synthetic banking behavior, trains a `RandomForestClassifier`, and saves the model and feature order with joblib. Synthetic data is useful for exercising the pipeline; it is not evidence of production model quality.

### Angular UI

```bash
cd finshield-ui
npm install
npm start
```

Open `http://localhost:4200`. The development environment uses the live Spring API at `http://localhost:8080/api`; the production container uses nginx to proxy `/api` and `/ws` to the backend.

The public registration endpoint creates an active identity but does not grant an operational role. That is intentional: self-registration must never create an analyst or administrator. The profile-scoped demo seeder supplies operational users for local testing; deployed identity provisioning belongs behind an approved IAM/administration workflow.

## Reproducible API checks

The `scripts/` folder contains high-risk, low-risk, and ML `.http` requests. Login first, paste the returned JWT into the file variable, and submit the transaction with a current or past UTC `initiatedAt`. Ingestion accepts either `sourceAccountId` or the friendlier `sourceAccountNumber` plus optional `customerNumber`.

The seeded high-risk example is expected to match high amount, new/untrusted device, new beneficiary, geo mismatch, and beneficiary watchlist signals, producing `HIGH / CREATE_ALERT`. The low-risk example uses `ACC2002` and `DEVICE-TRUSTED-001`, producing `LOW / APPROVE` once the Kafka consumer completes.

## Principal APIs

All endpoints except authentication and OpenAPI/Swagger require a bearer token.

| Area | Endpoints |
|---|---|
| Authentication | `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/auth/me` |
| Customers | create/search/get, Customer 360, risk update under `/api/customers` |
| Beneficiaries and devices | `/api/customers/{customerId}/beneficiaries`, `/devices` |
| Transactions | `POST /api/transactions/ingest`, search/detail/risk explanation under `/api/transactions` |
| Fraud rules | CRUD and enable/disable under `/api/fraud/rules` |
| Alerts | queue, detail, assignees, assignment, status, close, escalation under `/api/fraud/alerts` |
| AML | `/api/aml/watchlist`, `/api/aml/screenings/customers/{id}`, `/beneficiaries/{id}` |
| Cases | lifecycle, notes, evidence, status history under `/api/cases` |
| Dashboard | summary, risk trends, top rules, alert states, high-risk transactions, SLA summary |
| Notifications | current-user list, unread count, mark read under `/api/notifications` |
| Audit | searchable records under `/api/audit-logs` |

## Kafka topics

| Topic | Key | Purpose |
|---|---|---|
| `transaction.incoming` | transaction reference | durable hand-off from ingestion to risk scoring |
| `transaction.risk.scored` | transaction reference | downstream decision and scored feature event |
| `fraud.alert.created` | alert number | alert integration event published from the outbox |
| `<source-topic>.DLT` | original key | exhausted consumer records for controlled recovery |

The Compose environment uses one broker, so replication defaults to one. Production should use multiple brokers, replication of at least three where appropriate, authenticated/encrypted listeners, schema governance, and explicit topic retention policy.

## Data model

The main PostgreSQL tables are grouped below rather than presented as one enormous ER diagram.

| Domain | Tables |
|---|---|
| Identity | `app_users`, `roles`, `user_roles` |
| Customer context | `customers`, `customer_risk_history`, `accounts`, `beneficiaries`, `customer_devices` |
| Detection | `financial_transactions`, `transaction_risk_scores`, `transaction_rule_matches`, `fraud_rules` |
| AML | `aml_watchlist_entries`, `aml_screening_results` |
| Operations | `fraud_alerts`, `fraud_alert_event_outbox`, `investigation_cases`, `case_notes`, `case_evidence`, `case_status_history` |
| Control plane | `notifications`, `audit_logs` |

Entities use UUID identifiers and UTC timestamps. Relationships retain the customer, account, transaction, alert, and actor context needed to reconstruct an investigation. Evidence stores metadata and an approved URL only; physical object upload is intentionally outside this iteration.

## Roles

| Role | Operational responsibility |
|---|---|
| `ADMIN` | identity, configuration, platform oversight |
| `FRAUD_ANALYST` | transaction review, alert triage, fraud case investigation |
| `AML_INVESTIGATOR` | AML behavior review, screening, AML casework |
| `COMPLIANCE_OFFICER` | watchlist governance, escalation review, regulatory decisions |
| `RISK_MANAGER` | fraud rule policy, thresholds, risk and control oversight |

Roles are seeded idempotently at startup. The sidebar hides workspaces the user cannot operate, and Spring method security independently enforces the same boundaries.

## Screenshots

Add release screenshots here once the UI is running with representative, non-sensitive seed data.

| Dashboard | Transaction explanation | Investigation workspace |
|---|---|---|
| `docs/screenshots/dashboard.png` | `docs/screenshots/transaction-monitor.png` | `docs/screenshots/case-workspace.png` |
| Alert queue | Customer 360 | AML review |
| `docs/screenshots/alert-queue.png` | `docs/screenshots/customer-360.png` | `docs/screenshots/aml-review.png` |

## How I explain this project in an interview

I start with the decision path, not the framework list. A transfer is validated and committed, then Kafka separates ingestion from scoring. The consumer builds context from the customer, account, device, beneficiary, and Redis velocity windows. It evaluates database-configured rules, calls a model with a strict timeout, evaluates AML behavior, and persists both the final score and its contributing evidence. High-risk decisions become work: alerts have ownership, SLA state, closure outcomes, notifications, and an audit trail; serious reviews continue as cases with notes, evidence, and status history.

The trade-offs are equally important. The ML fallback favors platform availability, but a bank may prefer fail-closed for selected payment types. Redis gives inexpensive rolling counters but is not the system of record. The alert outbox handles the database/message dual-write boundary. Fuzzy screening finds more candidates but requires thresholds, reviewer disposition, and list-quality controls. These are policy decisions surfaced in the design, not hidden inside controller code.

## Production hardening still to do

- replace Hibernate schema updates with versioned Flyway migrations and reviewed rollback plans;
- integrate workforce SSO/MFA and an approval-based user/role administration flow;
- use Kafka TLS/SASL, a schema registry, broker replication, and monitored DLT replay;
- move secrets to a managed secret store and rotate signing keys;
- calibrate thresholds and the model on labeled, time-split banking data with drift and bias monitoring;
- add watchlist ingestion/versioning, match disposition, SAR/STR reporting, and maker-checker approvals;
- store evidence in encrypted object storage with malware scanning and retention/legal-hold policy;
- add OpenTelemetry traces, SLOs, alerting, load tests, and failure-injection tests;
- add optimistic concurrency controls to every analyst mutation and idempotency keys at external ingestion edges;
- expand automated tests across API contracts, Kafka retries/outbox delivery, authorization matrices, and end-to-end analyst journeys.

---

FinShield is an engineering portfolio project, not a certified banking control or a substitute for a financial institution's approved fraud/AML policy.
