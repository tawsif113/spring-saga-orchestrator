# Spring Saga Orchestrator

A showcase-ready **event-driven Saga orchestration proof-of-concept** built with **Spring Boot**, **RabbitMQ**, and **MongoDB**.

This project demonstrates how to coordinate a distributed order workflow across microservices while keeping each service autonomous, resilient, and idempotent.

---

## What this project demonstrates

- **Orchestrated Saga flow** across three business services:
  - `order-service` (order lifecycle + orchestration)
  - `inventory-service` (stock reservation/compensation)
  - `payment-service` (payment authorization/rejection)
- **Asynchronous communication** over RabbitMQ topic exchange (`saga.events`).
- **Local persistence per service** in MongoDB (`orderdb`, `inventorydb`, `paymentdb`).
- **Outbox-style event publishing** from order service.
- **Idempotent event consumption** using processed/consumed event collections.
- **Compensation path** for failed orders (inventory release).

---

## Architecture at a glance

### Services

- **order-service** (`:8081`)
  - Owns order aggregate state (`CREATED`, `PLACED`, `INVENTORY_RESERVED`, `PAYMENT_AUTHORIZED`, `CONFIRMED`, `FAILED`).
  - Emits order domain events.
  - Listens to inventory/payment outcome events and advances/fails saga.

- **inventory-service** (`:8082`)
  - Reserves stock on `ORDER_PLACED`.
  - Emits `INVENTORY_RESERVED` or `INVENTORY_REJECTED`.
  - Handles `ORDER_FAILED` (release reserved stock) and `ORDER_CONFIRMED` (commit reservation).

- **payment-service** (`:8083`)
  - Reacts to `ORDER_INVENTORY_RESERVED`.
  - Creates/authorizes/rejects payment.
  - Uses configurable auto-approve limit (`saga.payment.auto-approve-limit`).
  - Emits `PAYMENT_AUTHORIZED` or `PAYMENT_REJECTED`.

- **common**
  - Shared value objects and event contracts.

### Event choreography (high-level)

1. Client places an order (`order-service`).
2. `ORDER_PLACED` is published.
3. `inventory-service` reserves stock.
   - Success → `INVENTORY_RESERVED`
   - Failure → `INVENTORY_REJECTED`
4. `order-service` handles inventory result.
   - On reserve success, it emits `ORDER_INVENTORY_RESERVED`.
5. `payment-service` processes payment.
   - Success → `PAYMENT_AUTHORIZED`
   - Failure → `PAYMENT_REJECTED`
6. `order-service` handles payment result.
   - Success → order `CONFIRMED` + `ORDER_CONFIRMED`
   - Failure → order `FAILED` + `ORDER_FAILED` (which triggers stock release in inventory-service).

---

## Tech stack

- Java 25
- Spring Boot 4 (multi-module Gradle project)
- Spring AMQP (RabbitMQ)
- Spring Data MongoDB
- Docker Compose for local infrastructure

---

## Prerequisites

- Java 25
- Docker + Docker Compose
- Gradle wrapper (included)

---

## Quick start

### 1) Start infrastructure (Mongo sharded cluster + RabbitMQ)

```bash
docker compose up -d
./scripts/mongo-init-shards.sh
```

Enable sharding for app collections:

```bash
docker exec -it mongos mongosh --port 27017 --eval 'sh.enableSharding("orderdb"); sh.enableSharding("paymentdb"); sh.shardCollection("orderdb.orders",{_id:"hashed"}); sh.shardCollection("orderdb.order_events",{aggregateId:"hashed"}); sh.shardCollection("paymentdb.payments",{orderId:"hashed"});'
```

RabbitMQ is included in the compose stack and exposed on:
- AMQP: `localhost:5672`
- Management UI: `http://localhost:15672`

The services default to `admin/admin`, configurable via env vars:

```bash
export RABBITMQ_USERNAME=admin
export RABBITMQ_PASSWORD=admin
```

### 2) Run services (3 terminals)

```bash
./gradlew :inventory-service:bootRun
./gradlew :payment-service:bootRun
./gradlew :order-service:bootRun
```

### 3) Health checks

```bash
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8083/actuator/health
```

---

## Local API guide

## Order service (`http://localhost:8081`)

### Create order

```bash
curl -X POST http://localhost:8081/api/orders \
  -H 'Content-Type: application/json' \
  -d '{
    "customerId": "11111111-1111-1111-1111-111111111111",
    "shipTo": {"city": "Bangalore", "street": "MG Road", "houseNo": "42"}
  }'
```

### Add line item

```bash
curl -X POST http://localhost:8081/api/orders/<ORDER_ID>/lines \
  -H 'Content-Type: application/json' \
  -d '{
    "productId": "22222222-2222-2222-2222-222222222222",
    "qty": 2,
    "unitAmount": 499.99,
    "currency": "USD"
  }'
```

### Place order (starts saga)

```bash
curl -X POST http://localhost:8081/api/orders/<ORDER_ID>/place
```

### Inspect order + event history

```bash
curl http://localhost:8081/api/orders/<ORDER_ID>
curl http://localhost:8081/api/orders/<ORDER_ID>/events
```

## Inventory service (`http://localhost:8082`)

### Seed stock

```bash
curl -X POST http://localhost:8082/api/stock \
  -H 'Content-Type: application/json' \
  -d '{"productId": "22222222-2222-2222-2222-222222222222"}'

curl -X POST http://localhost:8082/api/stock/22222222-2222-2222-2222-222222222222/add \
  -H 'Content-Type: application/json' \
  -d '{"qty": 10}'
```

### Read stock

```bash
curl http://localhost:8082/api/stock/22222222-2222-2222-2222-222222222222
```

## Payment service (`http://localhost:8083`)

### Lookup payment by order

```bash
curl http://localhost:8083/api/payments/by-order/<ORDER_ID>
```

---

## Configuration reference

### Default service ports

- Order: `8081`
- Inventory: `8082`
- Payment: `8083`

### Messaging

- Exchange: `saga.events`
- RabbitMQ default credentials: `admin` / `admin` (override with `RABBITMQ_USERNAME` and `RABBITMQ_PASSWORD`)
- Key queues (configured in `application.yml`):
  - inventory consumes `inventory.order.placed`, `inventory.order.failed`, `inventory.order.confirmed`
  - payment consumes `payment.order.inventory-reserved`
  - order consumes `order.inventory.reserved`, `order.inventory.rejected`, `order.payment.authorized`, `order.payment.rejected`

### Datastores

- Services connect via `mongos` at `mongodb://localhost:27017`
- `order-service` database: `orderdb`
- `inventory-service` database: `inventorydb`
- `payment-service` database: `paymentdb`

---

## Why this implementation is production-minded

- **Idempotency-first consumers**: duplicate events are ignored by storing processed event ids.
- **Service autonomy**: each service owns its data and business rules.
- **Clear failure semantics**: rejection events carry reason payloads.
- **Compensation workflow**: inventory release path is built in.
- **Traceable lifecycle**: order event history endpoint enables saga introspection.

---

## Development tips

- To reset local state quickly:
  - stop local Spring Boot services
  - `docker compose down -v --remove-orphans`
  - `docker compose up -d`
  - `./scripts/mongo-init-shards.sh`
  - `docker exec -it mongos mongosh --port 27017 --eval 'sh.enableSharding("orderdb"); sh.enableSharding("paymentdb"); sh.shardCollection("orderdb.orders",{_id:"hashed"}); sh.shardCollection("orderdb.order_events",{aggregateId:"hashed"}); sh.shardCollection("paymentdb.payments",{orderId:"hashed"});'`
  - `docker compose exec rabbitmq rabbitmqctl stop_app && docker compose exec rabbitmq rabbitmqctl reset && docker compose exec rabbitmq rabbitmqctl start_app`
- Use RabbitMQ UI (`http://localhost:15672`) to inspect queue depth and message flow.
- Tail logs of all three services side-by-side to observe saga transitions in real time.

---

## Roadmap ideas

- Add distributed tracing (OpenTelemetry).
- Add dead-letter queues and retry policies.
- Add contract tests for event payload compatibility.
- Add authentication and service-to-service authorization.
- Containerize services and run the whole stack via a single compose profile.

---

## Repository structure

```text
.
├── common/
├── order-service/
├── inventory-service/
├── payment-service/
├── docker-compose.yml
├── settings.gradle
└── README.md
```
