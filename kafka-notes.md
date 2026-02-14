# Kafka Notes

This document explains **every Kafka-related line** found in `application.properties` (including commented lines) and in the **KafkaConfig** Java files across the project: what each property/config does and why it is used.

---

## Table of Contents

1. [Producer properties (application.properties)](#1-producer-properties-applicationproperties)
2. [Consumer properties (application.properties)](#2-consumer-properties-applicationproperties)
3. [Commented / optional properties](#3-commented--optional-properties)
4. [Topic names (application.properties)](#4-topic-names-applicationproperties)
5. [Kafka config Java files](#5-kafka-config-java-files)

---

## 1. Producer properties (application.properties)

| Property | Example value | Why it is used | Use of the property |
|----------|----------------|----------------|----------------------|
| **`spring.kafka.bootstrap-servers`** | `localhost:9092,localhost:9094,localhost:9096` | List of Kafka broker addresses the producer uses to discover the cluster and send records. | **Bootstrap servers** – Initial connection point. Producer uses these to get cluster metadata; multiple entries provide failover. Saga services use 3 brokers; transfer/product/email use 2. |
| **`spring.kafka.producer.bootstrap-servers`** | `localhost:9092,localhost:9094` | Same as above but under the producer-specific prefix. Used in transfer-producer, productsmicroservice (producer-only apps). | Alternative producer-only key when the app does not define consumer config. |
| **`spring.kafka.producer.key-serializer`** | `org.apache.kafka.common.serialization.StringSerializer` | Class that converts the message **key** (String) to bytes for the broker. | Keys are often partition keys or IDs; String is simple and readable. |
| **`spring.kafka.producer.value-serializer`** | `org.springframework.kafka.support.serializer.JacksonJsonSerializer` | Class that converts the message **value** (Java object) to JSON bytes. | Events/commands are POJOs; JSON allows interoperability and debugging. |
| **`spring.kafka.producer.acks`** | `all` | Number of replicas that must acknowledge a write before the producer considers it successful. | **`all`** (same as `-1`) = leader + all in-sync replicas must ack. Maximizes durability and avoids data loss on leader failover; used for saga and critical flows. |
| **`spring.kafka.producer.properties.delivery.timeout.ms`** | `120000` | Total time (ms) the producer will wait for a send to succeed (including retries). | 120 s cap so sends eventually fail instead of blocking forever; allows retries without indefinite wait. |
| **`spring.kafka.producer.properties.linger.ms`** | `0` | Time (ms) to wait before sending a batch to allow more records to be batched. | **0** = send as soon as possible. Keeps latency low; use &gt; 0 only when throughput is more important than latency. |
| **`spring.kafka.producer.properties.request.timeout.ms`** | `30000` | Time (ms) after which the producer considers a request to the broker failed if no response. | 30 s; avoids hanging on slow or stuck brokers. |
| **`spring.kafka.producer.properties.max.in.flight.requests.per.connection`** | `5` | Max number of unacknowledged requests per connection. | With **idempotence=true**, allows up to 5 in-flight requests while preserving order and exactly-once semantics. |
| **`spring.kafka.producer.properties.enable.idempotence`** | `true` | Enables idempotent producer: broker deduplicates by producer ID + sequence number. | Prevents duplicate records on retries; needed for at-most-once / exactly-once and saga consistency. |
| **`spring.kafka.producer.transaction-id-prefix`** | `transfer-service-${random.value}-` | Prefix for transactional producer IDs. Each producer instance gets a unique transactional id. | Used when producer is configured for **transactions** (e.g. `KafkaTransactionManager` in transfer-producer). Uniqueness (`random.value`) avoids two instances sharing the same transactional id. |

---

## 2. Consumer properties (application.properties)

| Property | Example value | Why it is used | Use of the property |
|----------|----------------|----------------|----------------------|
| **`spring.kafka.consumer.bootstrap-servers`** | `localhost:9092,localhost:9094` | List of Kafka broker addresses the consumer uses to connect and fetch records. | Same role as producer bootstrap-servers; consumer-only apps (credit, debit, email) use this key. |
| **`spring.kafka.consumer.key-deserializer`** | `org.apache.kafka.common.serialization.StringDeserializer` | Class that converts the message **key** bytes back to String. | Must match producer key type; String is common for keys. |
| **`spring.kafka.consumer.value-deserializer`** | `org.springframework.kafka.support.serializer.JacksonJsonDeserializer` | Class that converts the message **value** bytes (JSON) back to Java object. | Must match producer value serialization; JSON for events/commands. |
| **`spring.kafka.consumer.group-id`** | e.g. `orders-ms`, `amount-credit-events` | Consumer **group** identifier. All consumers with the same group id share the partition assignment for subscribed topics. | **orders-ms** / **products-ms** / **payments-ms**: one logical consumer per saga service. **amount-credit-events** / **amount-debit-events**: one per debit/credit topic. **product-created-events**: email notification consumer group. Ensures load balancing and no duplicate processing within the same group. |
| **`spring.kafka.consumer.properties.spring.json.trusted.packages`** | `com.eventcart.repo.*`, `com.eventcart.repo.event`, `com.eventcart.*` | Comma-separated package patterns that Jackson is allowed to deserialize. | **Security**: Jackson will only deserialize types from these packages, reducing risk of malicious payloads. Saga uses `com.eventcart.repo.*`; email uses `com.eventcart.repo.event`; credit/debit use `com.eventcart.*`. |
| **`spring.kafka.consumer.isolation-level`** | `READ_COMMITTED` | Whether the consumer reads only **committed** messages (no aborted transactional messages). | **READ_COMMITTED**: skip messages from aborted producer transactions. Required when producers use transactions (e.g. saga, transfer-producer) so consumers do not see rolled-back data. |

---

## 3. Commented / optional properties

These appear in `application.properties` as commented lines. They are listed here with their purpose so you know when to enable them.

| Property (commented) | Example | Why it could be used | Use of the property |
|----------------------|--------|----------------------|----------------------|
| **`#spring.kafka.producer.retries`** | `#spring.kafka.producer.retries=10` | (transfer-producer, productsmicroservice) Legacy retry count. | With modern Kafka client, **delivery.timeout.ms** and internal retries are preferred. If uncommented: producer would retry failed sends up to 10 times. |
| **`#spring.kafka.producer.properties.retry.backoff.ms`** | `#spring.kafka.producer.properties.retry.backoff.ms=1000` | Delay (ms) between retries. | 1000 ms = 1 second between retry attempts. Only relevant if you explicitly configure retries. |
| **`#logging.level.org.springframework.kafka.transaction.KafkaTransactionManager=trace`** | (saga orders/products/payments) | Log level for Spring’s Kafka transaction manager. | **trace** = very verbose; use when debugging transactional send/commit/abort behavior. |
| **`#logging.level.org.springframework.transaction=trace`** | (orders-service, transfer-producer) | Log level for Spring’s generic transaction abstraction. | See all transaction begin/commit/rollback (DB and/or Kafka). |
| **`#logging.level.org.springframework.orm.jpa.JpaTransactionManager=trace`** | (orders-service, transfer-producer) | Log level for JPA transaction manager. | Debug DB transaction boundaries when combining JPA with Kafka. |
| **`#logging.level.org.apache.kafka.clients.producer.internals.TransactionManager=trace`** | (saga, transfer-producer) | Log level for Kafka client’s internal transaction coordinator. | Debug producer-side init, begin, commit, abort of transactions. |
| **`logging.level...=debug`** (active in transfer-producer) | `KafkaTransactionManager=debug`, `TransactionManager=debug` | Same as above but **debug** and actually enabled in transfer-producer. | Less verbose than trace; useful to confirm transactional producer behavior. |

---

## 4. Topic names (application.properties)

Custom topic names are used so each environment or service can override them without code changes.

| Property | Example value | Where used | Use |
|----------|----------------|------------|-----|
| **`orders.events.topic.name`** | `orders-events` | orders-service, products-service | Topic for order events (OrderCreatedEvent, OrderApprovedEvent). |
| **`orders.commands.topic.name`** | `orders-commands` | orders-service | Topic for commands to orders (ApproveOrderCommand, RejectOrderCommand). |
| **`products.commands.topic.name`** | `products-commands` | orders-service, products-service | Topic for product commands (ReserveProductCommand, CancelProductReservationCommand). |
| **`products.events.topic.name`** | `products-events` | orders-service, products-service | Topic for product events (ProductReservedEvent, ProductReservationCancelEvent, etc.). |
| **`payments.commands.topic.name`** | `payments-commands` | orders-service, payments-service | Topic for payment commands (ProcessPaymentCommand). |
| **`payments.events.topic.name`** | `payments-events` | orders-service, payments-service | Topic for payment events (PaymentProcessedEvent, PaymentFailedEvent). |

---

## 5. Kafka config Java files

Each Kafka config class defines **beans** and often **overrides or supplements** the properties above. Below: which beans exist, what they do, and why certain Kafka constants are used.

---

### 5.1 Saga: OrdersKafkaConfig

**File:** `saga/orders-service/.../OrdersKafkaConfig.java`

| Element | What it does | Why it is used |
|--------|----------------|-----------------|
| **@Value properties** | Injects `spring.kafka.*` and custom topic names from `application.properties`. | Single place to tune producer/consumer and topic names per environment. |
| **producerConfigs()** | Builds a `Map` with `ProducerConfig.*` keys (bootstrap, serializers, acks, delivery timeout, linger, request timeout, idempotence, max in-flight, **transactional.id**). | Used by `ProducerFactory`; **TRANSACTIONAL_ID_CONFIG** is set so the producer can participate in transactions (bean is commented but config is ready). |
| **ProducerFactory&lt;String, Object&gt;** | Creates Kafka producer instances with the above config. | Default producer for sending events/commands. |
| **KafkaTemplate&lt;String, Object&gt;** | Spring helper to send records to topics. | Used by OrderSaga and OrderServiceImpl to publish events and commands. |
| **ConsumerFactory** | Builds consumer config with **ErrorHandlingDeserializer** wrapping **JacksonJsonDeserializer**. Sets **GROUP_ID_CONFIG**, **JacksonJsonDeserializer.TRUSTED_PACKAGES**, **ISOLATION_LEVEL_CONFIG** (from property, lowercased). | **ErrorHandlingDeserializer** passes bad payloads to an error handler instead of killing the listener. **READ_COMMITTED** so saga does not read aborted transactional messages. |
| **NewTopic beans** | `TopicBuilder.name(...).partitions(3).replicas(3).configs(Map.of("min.insync.replicas", "2"))`. Created for orders-events, products-commands, payments-commands, orders-commands. | Ensures topics exist with 3 partitions, 3 replicas, and at least 2 in-sync replicas for durability; matches `acks=all`. |
| **Commented: KafkaTransactionManager** | Would create a `KafkaTransactionManager` for transactional sends. | For use with `@Transactional` and Kafka; currently disabled. |
| **Commented: JpaTransactionManager** | Would set JPA as primary transaction manager. | For coordinating DB + Kafka in one transaction; currently disabled. |
| **Commented: kafkaListenerContainerFactory** | Would use **DefaultErrorHandler** with **DeadLetterPublishingRecoverer** and **FixedBackOff(5000, 3)**. | After 3 retries (5 s apart), failed records would go to a dead-letter topic; **NonRetryableException** / **RetryableException** control retry vs no-retry. |

---

### 5.2 Saga: ProductsKafkaConfig

**File:** `saga/products-service/.../ProductsKafkaConfig.java`

Same structure as OrdersKafkaConfig for producer/consumer and topic creation. Differences:

| Element | What it does | Why it is used |
|--------|----------------|-----------------|
| **NewTopic** | Only **products** command/events topic (name from `products.events.topic.name`). | Products service only creates the topic it needs. |
| **ConsumerFactory** | Same pattern: **ErrorHandlingDeserializer**, trusted packages, isolation level. | Consistent consumer behavior across saga services. |

Commented beans: **KafkaTransactionManager**, **JpaTransactionManager**, **kafkaListenerContainerFactory** (with DLQ and retry) — same intent as in OrdersKafkaConfig.

---

### 5.3 Saga: PaymentsKafkaConfig

**File:** `saga/payments-service/.../PaymentsKafkaConfig.java`

Same pattern as Orders/Products: producer and consumer config from properties, **ErrorHandlingDeserializer**, trusted packages, isolation level. **NewTopic** only for `payments.events.topic.name`. Same commented transaction and DLQ/retry beans.

---

### 5.4 Transfer-producer: KafkaProducerConfig

**File:** `transfer-producer/.../KafkaProducerConfig.java`

| Element | What it does | Why it is used |
|--------|----------------|-----------------|
| **producerConfigs()** | Same producer settings as saga (including **TRANSACTIONAL_ID_CONFIG**). | Transfer flow uses transactional sends (DB + Kafka). |
| **ProducerFactory / KafkaTemplate** | Same as saga; type `&lt;String, Object&gt;`. | Sends DebitRequestedEvent and CreditRequestedEvent. |
| **KafkaTransactionManager** | **Active** bean; wraps the producer for Spring-managed Kafka transactions. | Used with `@Transactional("transactionManager")` so transfer record and Kafka sends commit or roll back together. |
| **JpaTransactionManager** | **Active** bean; primary transaction manager for JPA. | Coordinates DB transaction with Kafka (via chained managers or application logic). |
| **NewTopic: debit-amount-topic, credit-amount-topic** | Partitions 3, replicas 3, `min.insync.replicas=2`. | Auto-create topics for debit/credit events with same durability as saga. |

---

### 5.5 Credit consumer: CreditKafkaConsumerConfig

**File:** `credit-amount-consumer-microservice/.../CreditKafkaConsumerConfig.java`

| Element | What it does | Why it is used |
|--------|----------------|-----------------|
| **ConsumerFactory** | Bootstrap, key/value deserializers (**ErrorHandlingDeserializer** + Jackson), **GROUP_ID_CONFIG**, **TRUSTED_PACKAGES**, **ISOLATION_LEVEL_CONFIG**. | Safe JSON consumption and READ_COMMITTED for transactional producers. |
| **kafkaListenerContainerFactory** | **Active** bean. Uses **DefaultErrorHandler(DeadLetterPublishingRecoverer(kafkaTemplate), FixedBackOff(5000, 3))**; registers **NonRetryableException** and **RetryableException**. | After 3 retries (5 s apart), failed messages go to DLQ; exception type decides retry or not. |
| **ProducerFactory / KafkaTemplate** | Simple producer (bootstrap, String + JacksonJsonSerializer). | Used only by **DeadLetterPublishingRecoverer** to publish failed records to the DLQ topic. |

---

### 5.6 Debit consumer: DebitKafkaConsumerConfig

**File:** `debit-amount-consumer-microservice/.../DebitKafkaConsumerConfig.java`

Same as CreditKafkaConsumerConfig: **ConsumerFactory** with ErrorHandlingDeserializer and isolation level, **kafkaListenerContainerFactory** with DLQ and **FixedBackOff(5000, 3)**, and a **KafkaTemplate** for the recoverer. Listener references `containerFactory = "kafkaListenerContainerFactory"`.

---

### 5.7 EmailNotificationMicroservice: KafkaConsumerConfig

**File:** `EmailNotificationMicroservice/.../KafkaConsumerConfig.java`

| Element | What it does | Why it is used |
|--------|----------------|-----------------|
| **ConsumerFactory** | Same pattern; **trusted.packages** from properties (`com.eventcart.repo.event`). No isolation level in properties; config does not set it. | Consumer only; trusted packages restrict deserialization to event types. |
| **kafkaListenerContainerFactory** | **DefaultErrorHandler** + **DeadLetterPublishingRecoverer** + **FixedBackOff(5000, 3)**; **NonRetryableException** / **RetryableException**. | Idempotent handler plus retry/DLQ for transient failures (e.g. HTTP timeout). |
| **ProducerFactory / KafkaTemplate** | For DLQ only. | Sends failed events to the dead-letter topic. |

---

### 5.8 Productsmicroservice: KafkaProducerConfig

**File:** `productsmicroservice/.../KafkaProducerConfig.java`

| Element | What it does | Why it is used |
|--------|----------------|-----------------|
| **producerConfigs()** | Same producer settings as others **except no TRANSACTIONAL_ID_CONFIG**. | Producer is **not** transactional; no KafkaTransactionManager. |
| **ProducerFactory&lt;String, ProductCreatedEvent&gt;** | Typed to **ProductCreatedEvent**. | Ensures only that event type is sent; type-safe. |
| **KafkaTemplate&lt;String, ProductCreatedEvent&gt;** | Typed template. | Used in ProductServiceImpl to send ProductCreatedEvent. |
| **NewTopic: product-created-events-topic** | Partitions 3, replicas 3, `min.insync.replicas=2`. | Same durability as other learning-project topics. |

---

## Summary table: Where each property is used

| Property / concept | Saga (orders/products/payments) | Transfer-producer | Credit/Debit consumer | EmailNotification | Productsmicroservice |
|--------------------|----------------------------------|-------------------|------------------------|------------------|---------------------|
| bootstrap-servers | ✅ (3 brokers) | ✅ (2) | ✅ (2) | ✅ (2) | ✅ (2) |
| acks=all | ✅ | ✅ | — | — | ✅ |
| idempotence | ✅ | ✅ | — | — | ✅ |
| transaction-id-prefix | ✅ (set, tx manager commented) | ✅ (tx manager active) | — | — | ❌ |
| consumer group-id | ✅ (per service) | — | ✅ | ✅ | — |
| trusted.packages | ✅ (com.eventcart.repo.*) | — | ✅ (com.eventcart.*) | ✅ (com.eventcart.repo.event) | — |
| isolation.level READ_COMMITTED | ✅ | — | ✅ | ❌ (not in props) | — |
| ErrorHandlingDeserializer | ✅ (in config) | — | ✅ | ✅ | — |
| DeadLetterPublishingRecoverer | Commented in saga | — | ✅ | ✅ | — |
| NewTopic (min.insync.replicas=2) | ✅ | ✅ | — | — | ✅ |

This document covers every Kafka-related line in `application.properties` (including commented) and the purpose of each KafkaConfig class and its beans in this repository.
