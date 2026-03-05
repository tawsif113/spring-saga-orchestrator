# Saga POC (DDD-friendly) — Scaffold

This repo is a **multi-module Gradle (Groovy DSL)** scaffold for a small **Saga** POC with 3 microservices:

- `order-service` (REST + orchestrator later)
- `inventory-service` (commands/events later)
- `payment-service` (commands/events later)
- `common` (shared Value Objects / IDs)

## Prereqs
- Java 25
- Gradle 8.14+ or 9.x (Spring Boot 4 compatible)
- Docker (for RabbitMQ)

## Start infrastructure (RabbitMQ)
```bash
docker compose up -d
```

RabbitMQ UI: http://localhost:15672  (guest/guest)

## Run services (3 terminals)
If you don’t have the Gradle wrapper yet, generate it once:
```bash
gradle wrapper
```

Then run:
```bash
./gradlew :inventory-service:bootRun
./gradlew :payment-service:bootRun
./gradlew :order-service:bootRun
```

## Test order-service
```bash
curl http://localhost:8081/actuator/health
curl http://localhost:8081/api/ping
```

## Next steps (we’ll implement in phases)
1) Define message contracts in `common`
2) Create exchanges/queues and listeners (AMQP)
3) Add Saga orchestrator state in order-service
4) Add compensation (release inventory) + idempotency
