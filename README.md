# Kafka Learning Repository

A collection of **Spring Boot** projects used to learn **Apache Kafka**, including producer/consumer examples and a full **Saga orchestration** implementation with **compensating transactions**.

- **Java:** 17  
- **Spring Boot:** 4.0.2  
- **Build:** Maven  

---

## Repository Structure

| Path | Description |
|------|-------------|
| **`core/`** | Shared library: DTOs, events, commands, exceptions used across services |
| **`saga/`** | **Saga orchestration** — order flow with compensating transactions (see [Saga](#saga-saga-orchestration-with-compensating-transactions) below) |
| **`transfer-producer/`** | Producer example: sends debit/credit events to Kafka |
| **`credit-amount-consumer-microservice/`** | Consumer example: listens to `credit-amount-topic` |
| **`debit-amount-consumer-microservice/`** | Consumer example: listens to `debit-amount-topic` |
| **`productsmicroservice/`** | Producer example: creates products and publishes `ProductCreatedEvent` |
| **`EmailNotificationMicroservice/`** | Consumer example: idempotent consumer with retry and dead-letter handling |

---

## Prerequisites

- **Kafka** (e.g. `localhost:9092`, or cluster `localhost:9092,9094,9096` for saga)
- **MySQL** (optional; saga can use H2 — see `application.properties` in each saga service)
- **Maven** to build the `core` module first (other modules depend on it)

---

## Building

Build the shared **core** module first (required by all other services):

```bash
cd core
mvn clean install
```

Then build and run any service from its directory, e.g.:

```bash
cd saga/orders-service
mvn spring-boot:run
```

---

## Kafka Learning Projects (Producer / Consumer)

These projects demonstrate basic Kafka usage: producers, consumers, and error handling.

### 1. Transfer Producer (`transfer-producer`)

- **Purpose:** Producer that publishes **debit** and **credit** events for a “transfer” flow.
- **API:** `POST /transfer` with a transfer payload (e.g. `senderId`, `recepientId`).
- **Behavior:** Persists a transfer record, sends `DebitRequestedEvent` to `debit-amount-topic`, calls an external HTTP endpoint, then sends `CreditRequestedEvent` to `credit-amount-topic`.
- **Kafka:** `debit-amount-topic`, `credit-amount-topic` (bootstrap: `localhost:9092,9094`).

### 2. Credit Amount Consumer (`credit-amount-consumer-microservice`)

- **Purpose:** Consumer for credit events.
- **Behavior:** Listens to `credit-amount-topic` and logs `CreditRequestedEvent` (e.g. `recepientId`).
- **Kafka:** `credit-amount-topic`.

### 3. Debit Amount Consumer (`debit-amount-consumer-microservice`)

- **Purpose:** Consumer for debit events.
- **Behavior:** Listens to `debit-amount-topic` and logs `DebitRequestedEvent` (e.g. `senderId`).
- **Kafka:** `debit-amount-topic`.

### 4. Products Microservice (`productsmicroservice`)

- **Purpose:** Producer that creates a “product” and publishes a **product-created** event.
- **API:** `POST /products` with product details (title, price, quantity).
- **Behavior:** Generates a product ID, builds `ProductCreatedEvent`, sends to `product-created-events-topic` with a `messageId` header (sync send with `.get()`).
- **Kafka:** `product-created-events-topic`.

### 5. Email Notification Microservice (`EmailNotificationMicroservice`)

- **Purpose:** Consumer with **idempotency**, **retries**, and **dead-letter** behavior.
- **Behavior:**
  - Listens to `product-created-events-topic`.
  - Uses `messageId` header to skip duplicate processing (stores processed IDs in DB).
  - Calls an external HTTP endpoint; uses `RetryableException` / `NonRetryableException` for error handling.
  - Configured with `DefaultErrorHandler`, `DeadLetterPublishingRecoverer`, and fixed backoff (e.g. 3 retries).
- **Kafka:** `product-created-events-topic`; DLQ and retry via custom `KafkaConsumerConfig`.

---

## Saga: Saga Orchestration with Compensating Transactions

The **`saga/`** folder contains a **Spring Boot** application that implements the **Saga orchestration pattern** over Kafka, with **compensating transactions** when a step fails.

### Overview

- **Orchestrator:** The **orders-service** hosts the saga orchestrator (`OrderSaga`), which listens to **events** from orders, products, and payments and sends **commands** to the respective services.
- **Services:**
  - **orders-service** — Creates orders, approves/rejects them, maintains order history.
  - **products-service** — Reserves or cancels product inventory.
  - **payments-service** — Processes payment (and calls the credit-card-processor).
  - **credit-card-processor-service** — Minimal HTTP service used by payments (e.g. mock endpoint).

### Happy Path (Success)

1. **Client** → `POST /orders` (orders-service).
2. **Orders-service** saves order (e.g. status `CREATED`), publishes **OrderCreatedEvent** to `orders-events`.
3. **OrderSaga** receives `OrderCreatedEvent` → sends **ReserveProductCommand** to `products-commands`.
4. **Products-service** reserves stock, publishes **ProductReservedEvent** to `products-events`.
5. **OrderSaga** receives `ProductReservedEvent` → sends **ProcessPaymentCommand** to `payments-commands`.
6. **Payments-service** processes payment (calls credit-card-processor), publishes **PaymentProcessedEvent** to `payments-events`.
7. **OrderSaga** receives `PaymentProcessedEvent` → sends **ApproveOrderCommand** to `orders-commands`.
8. **Orders-service** sets order to `APPROVED`, publishes **OrderApprovedEvent**; saga records success in order history.

### Compensating Path (Failure & Rollback)

- If **payment fails**, payments-service publishes **PaymentFailedEvent**.
  - **OrderSaga** receives it → sends **CancelProductReservationCommand** to products-service.
  - **Products-service** restores inventory and publishes **ProductReservationCancelEvent**.
  - **OrderSaga** receives it → sends **RejectOrderCommand** to orders-service.
  - **Orders-service** sets order to `REJECTED`; saga records rejection in order history.

- If **product reservation fails** (e.g. insufficient stock), products-service publishes **ProductReservationFailedEvent**; the saga can be extended to reject the order similarly.

### Topics (Saga)

| Topic | Direction | Content |
|-------|-----------|--------|
| `orders-events` | Orders → Saga | OrderCreatedEvent, OrderApprovedEvent |
| `orders-commands` | Saga → Orders | ApproveOrderCommand, RejectOrderCommand |
| `products-commands` | Saga → Products | ReserveProductCommand, CancelProductReservationCommand |
| `products-events` | Products → Saga | ProductReservedEvent, ProductReservationCancelEvent, ProductReservationFailedEvent |
| `payments-commands` | Saga → Payments | ProcessPaymentCommand |
| `payments-events` | Payments → Saga | PaymentProcessedEvent, PaymentFailedEvent |

### Saga Services (Ports & Config)

| Service | Port | Notes |
|---------|------|--------|
| orders-service | 8080 | Saga orchestrator + order API and history |
| products-service | 8081 | Product API, reserve/cancel |
| payments-service | 8082 | Payment processing, calls credit-card-processor |
| credit-card-processor-service | (dynamic) | Mock HTTP endpoint for payments |

Kafka bootstrap for saga: `localhost:9092,localhost:9094,localhost:9096` (override in each service’s `application.properties`).

### Running the Saga

1. Start Kafka (and optionally MySQL; or switch saga apps to H2).
2. Build and install **core**: `cd core && mvn clean install`.
3. Start **credit-card-processor-service** (payments-service calls it via `remote.ccp.url`).
4. Start **products-service**, **payments-service**, **orders-service** (order can vary, but orders-service must be up for the API).
5. Create products (if needed) via products-service, then place an order via `POST /orders` (e.g. `customerId`, `productId`, `productQuantity`).
6. Query order status/history: e.g. `GET /orders/{orderId}`, `GET /orders/history/{orderId}`.

---

## Core Module (`core/`)

Shared artifacts used across the repo:

- **Events:** e.g. `OrderCreatedEvent`, `OrderApprovedEvent`, `PaymentProcessedEvent`, `PaymentFailedEvent`, `ProductCreatedEvent`, `CreditRequestedEvent`, `DebitRequestedEvent`.
- **Commands:** e.g. `ReserveProductCommand`, `CancelProductReservationCommand`, `ProcessPaymentCommand`, `ApproveOrderCommand`, `RejectOrderCommand`.
- **DTOs:** e.g. `Order`, `Product`, `Payment`, `ProductReservedEvent`, `ProductReservationFailedEvent`, `ProductReservationCancelEvent`.
- **Types:** e.g. `OrderStatus` (CREATED, APPROVED, REJECTED).
- **Exceptions:** e.g. `ProductInsufficientQuantityException`, `CreditCardProcessorUnavailableException`, `RetryableException`, `NonRetryableException`.

Publish **core** to your local Maven repo (or your configured repository) so that the other modules resolve `com.eventcart.repo:core`.

---

## Summary

| Project | Role | Kafka usage |
|---------|------|-------------|
| **core** | Shared lib | — |
| **saga/* (4 apps)** | Saga orchestration + compensating transactions | Events & commands over 6 topics |
| **transfer-producer** | Producer | Debit/credit topics |
| **credit-amount-consumer-microservice** | Consumer | credit-amount-topic |
| **debit-amount-consumer-microservice** | Consumer | debit-amount-topic |
| **productsmicroservice** | Producer | product-created-events-topic |
| **EmailNotificationMicroservice** | Consumer (idempotent, retry, DLQ) | product-created-events-topic |

Together, these projects illustrate **Kafka producers/consumers**, **event-driven flows**, and the **Saga orchestration pattern with compensating transactions** in Spring Boot.
