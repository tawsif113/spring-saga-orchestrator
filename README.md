# Saga POC (DDD-friendly) — Scaffold

This repo is a **multi-module Gradle (Groovy DSL)** scaffold for a small **Saga** POC with 3 microservices:

- `order-service` (REST + orchestrator later)
- `inventory-service` (commands/events later)
- `payment-service` (commands/events later)
- `common` (shared Value Objects / IDs)

## Prereqs
- Java 25
- Gradle 8.14+ or 9.x (Spring Boot 4 compatible)
- Docker (for Mongo sharding and RabbitMQ)

## Start infrastructure
```bash
docker compose up -d
```

This compose file defines the MongoDB shard/config/mongos topology.
If RabbitMQ is in a separate compose stack, start that too.

## Mongo shard runbook
Initialize replica sets + add shards to mongos:
```bash
./scripts/mongo-init-shards.sh
```

Verify shard health and distribution metadata:
```bash
./scripts/mongo-verify-shards.sh
```

Shard app collections (example):
```bash
docker exec -it mongos mongosh --port 27017 --eval 'sh.enableSharding("orderdb"); sh.enableSharding("paymentdb"); sh.shardCollection("orderdb.orders",{_id:"hashed"}); sh.shardCollection("orderdb.order_events",{aggregateId:"hashed"}); sh.shardCollection("paymentdb.payments",{orderId:"hashed"});'
```

Generate load through APIs:
```bash
./scripts/load-orders.sh --orders 500 --concurrency 20
```

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
