# Apache Kafka — Complete Structured Notes

> **Beginner-friendly, concept-focused, interview-ready guide**  
> Covers: Kafka fundamentals, Spring Boot integration, producer/consumer patterns, transactions, idempotency, error handling, consumer groups, Saga design pattern, and compensating transactions.

---

## Table of Contents

1. [Introduction to Apache Kafka](#1-introduction-to-apache-kafka)
2. [Kafka Architecture & Core Components](#2-kafka-architecture--core-components)
3. [Kafka Broker(s) & KRaft Mode](#3-kafka-brokers--kraft-mode)
4. [Kafka CLI Operations](#4-kafka-cli-operations)
5. [Kafka Producer — Spring Boot Microservice](#5-kafka-producer--spring-boot-microservice)
6. [Producer Acknowledgements & Retries](#6-producer-acknowledgements--retries)
7. [Idempotent Producer](#7-idempotent-producer)
8. [Kafka Consumer — Spring Boot Microservice](#8-kafka-consumer--spring-boot-microservice)
9. [Consumer Error Handling — Deserialization Errors](#9-consumer-error-handling--deserialization-errors)
10. [Dead Letter Topic (DLT)](#10-dead-letter-topic-dlt)
11. [Consumer Exceptions & Retries](#11-consumer-exceptions--retries)
12. [Consumer Groups & Rebalancing](#12-consumer-groups--rebalancing)
13. [Idempotent Consumer](#13-idempotent-consumer)
14. [Apache Kafka Transactions](#14-apache-kafka-transactions)
15. [Kafka & Database Transactions](#15-kafka--database-transactions)
16. [Saga Design Pattern with Apache Kafka](#16-saga-design-pattern-with-apache-kafka)
17. [Saga — Compensating Transactions](#17-saga--compensating-transactions)
18. [Interview Quick-Reference Cheat Sheet](#18-interview-quick-reference-cheat-sheet)

---

## 1. Introduction to Apache Kafka

### What is Apache Kafka?

**Apache Kafka** is a distributed **event streaming platform** used to build real-time, event-driven applications. It acts as a high-throughput, fault-tolerant **message broker** that allows microservices to communicate **asynchronously** by publishing and consuming **events** (messages).

### Why Kafka? — Monolith vs. Microservices

| Aspect         | Monolith                | Microservices                    |
| -------------- | ----------------------- | -------------------------------- |
| Deployment     | Single deployable unit  | Independent services             |
| Scaling        | Scale entire app        | Scale individual services        |
| Communication  | In-process method calls | Network calls (HTTP / messaging) |
| Failure impact | Entire app affected     | Isolated failure                 |

### Synchronous vs. Asynchronous Communication

```
Synchronous (Request-Response):
┌──────────┐   HTTP Request    ┌──────────┐
│ Service A │ ───────────────► │ Service B │
│           │ ◄─────────────── │           │
└──────────┘   HTTP Response   └──────────┘
  ⚠️ Service A WAITS for Service B

Asynchronous (Event-Driven):
┌──────────┐   Publish Event   ┌──────────────┐   Consume Event   ┌──────────┐
│ Service A │ ────────────────► │ Message Broker│ ────────────────► │ Service B │
│           │                   │   (Kafka)     │                   │           │
└──────────┘                    └──────────────┘                    └──────────┘
  ✅ Service A does NOT wait — fire and forget
```

**Key Interview Terms:**

- **Synchronous communication**: The sender waits for a response before continuing. Tight coupling.
- **Asynchronous communication**: The sender publishes a message and continues immediately. Loose coupling.
- **Event-driven architecture**: Microservices communicate by producing and consuming events through a message broker.

### Kafka's Role

Kafka sits **between** producers and consumers as a **message broker**:

```
┌──────────┐        ┌─────────────────┐        ┌──────────┐
│ Producer  │──────► │   Apache Kafka  │──────► │ Consumer │
│ (Service) │  send  │   (Broker)      │  read  │ (Service)│
└──────────┘        └─────────────────┘        └──────────┘
```

- **Producer**: Application that **sends** (publishes) messages to Kafka.
- **Consumer**: Application that **reads** (subscribes to) messages from Kafka.
- **Broker**: A Kafka server that **stores** and **delivers** messages.

---

## 2. Kafka Architecture & Core Components

### Message (Event / Record)

A Kafka **message** (also called an **event** or **record**) is the fundamental unit of data. It consists of:

| Field         | Description                                                                                     |
| ------------- | ----------------------------------------------------------------------------------------------- |
| **Key**       | Optional. Used for **partition routing** (messages with the same key go to the same partition). |
| **Value**     | The actual data payload (e.g., JSON object).                                                    |
| **Timestamp** | When the message was created.                                                                   |
| **Headers**   | Optional key-value metadata (e.g., message ID, correlation ID).                                 |

### Topic

A **Topic** is a named, logical channel (category) to which messages are published. Think of it as a **folder** or a **database table** for events.

- Producers write to topics.
- Consumers read from topics.
- Topics can have multiple producers and multiple consumers simultaneously.

### Partition

Each topic is split into one or more **Partitions**. A partition is an **ordered, immutable sequence** of messages.

```
Topic: "orders"
┌──────────────────────────────────┐
│ Partition 0: [msg0][msg1][msg2]  │
│ Partition 1: [msg0][msg1]        │
│ Partition 2: [msg0][msg1][msg2]  │
└──────────────────────────────────┘
```

**Why partitions?**

- Enable **parallelism** — multiple consumers can read from different partitions simultaneously.
- Enable **horizontal scaling** — more partitions = more throughput.
- **Ordering guarantee**: Messages within a **single partition** are strictly ordered. Messages across different partitions are **NOT** ordered.

### Offset

An **Offset** is a unique, sequential ID assigned to each message **within a partition**. It represents the position of a message.

```
Partition 0:
  Offset 0 → msg_A
  Offset 1 → msg_B
  Offset 2 → msg_C   ← Consumer's current position
```

- Consumers track their position using offsets.
- **Committed offset** = the last successfully processed message.
- If a consumer restarts, it resumes from its last committed offset.

### Message Key & Ordering

When a producer sends a message **with a key**, Kafka uses a **hashing algorithm** on the key to determine which partition the message goes to. Messages with the **same key always go to the same partition**, ensuring order for related messages.

```
Key = "user-123"  →  hash("user-123") % partitions  →  Partition 1
Key = "user-456"  →  hash("user-456") % partitions  →  Partition 0
Key = "user-123"  →  hash("user-123") % partitions  →  Partition 1  (same!)
```

> **Interview Tip**: "Kafka guarantees message ordering **per partition**, not across partitions. To guarantee ordering for a specific entity (e.g., a user), use that entity's ID as the message key."

### Replication

Each partition can be **replicated** across multiple brokers for fault tolerance.

```
Broker 1: Partition 0 (Leader)    Partition 1 (Follower)
Broker 2: Partition 0 (Follower)  Partition 1 (Leader)
Broker 3: Partition 0 (Follower)  Partition 1 (Follower)
```

- **Leader Replica**: Handles all read/write requests for the partition.
- **Follower Replica**: Passively replicates data from the leader. Takes over if the leader fails.
- **Replication Factor**: Number of copies of each partition (e.g., `replication-factor=3` means 3 copies).
- **ISR (In-Sync Replicas)**: The set of replicas that are fully caught up with the leader.

---

## 3. Kafka Broker(s) & KRaft Mode

### What is a Broker?

A **Broker** is a single Kafka server instance. A Kafka **cluster** consists of multiple brokers working together.

```
Kafka Cluster
┌──────────┐  ┌──────────┐  ┌──────────┐
│ Broker 1 │  │ Broker 2 │  │ Broker 3 │
│ (Leader P0)│ │(Leader P1)│ │(Leader P2)│
│ (Follower │  │(Follower │  │(Follower │
│  P1, P2)  │  │  P0, P2) │  │  P0, P1) │
└──────────┘  └──────────┘  └──────────┘
```

**Responsibilities of a Broker:**

- Stores messages on disk.
- Serves producer write requests and consumer read requests.
- Manages partition replicas (leader election, replication).

### KRaft Mode (Kafka Raft)

**KRaft** is the new **consensus protocol** that replaces **Apache ZooKeeper** for Kafka cluster metadata management.

| Feature             | ZooKeeper Mode (Legacy)             | KRaft Mode (New)                 |
| ------------------- | ----------------------------------- | -------------------------------- |
| External dependency | Requires separate ZooKeeper cluster | No external dependency           |
| Architecture        | Kafka + ZooKeeper                   | Kafka only                       |
| Metadata management | ZooKeeper stores metadata           | Kafka stores metadata internally |
| Scalability         | Limited by ZooKeeper                | Better scalability               |

**KRaft Key Details:**

- Uses built-in **Raft consensus protocol** for leader election.
- Metadata stored in an internal Kafka topic (`__cluster_metadata`).
- Recommended for all new Kafka deployments.

### Starting Kafka with KRaft Mode

**Step 1: Generate a Cluster UUID**

```bash
KAFKA_CLUSTER_ID="$(bin/kafka-storage.sh random-uuid)"
```

**Step 2: Format storage directories**

```bash
bin/kafka-storage.sh format -t $KAFKA_CLUSTER_ID \
  -c config/kraft/server.properties
```

**Step 3: Start the broker**

```bash
bin/kafka-server-start.sh config/kraft/server.properties
```

### Running Multiple Brokers

For a multi-broker cluster, each broker needs its own configuration file with unique values:

| Property    | Broker 1     | Broker 2     | Broker 3     |
| ----------- | ------------ | ------------ | ------------ |
| `node.id`   | 1            | 2            | 3            |
| `listeners` | :9092        | :9094        | :9096        |
| `log.dirs`  | /tmp/server1 | /tmp/server2 | /tmp/server3 |

### Graceful Shutdown

```bash
bin/kafka-server-stop.sh
```

- Always stop brokers gracefully to ensure partition leadership is properly transferred.

---

## 4. Kafka CLI Operations

### Topic Operations

**Create a topic:**

```bash
bin/kafka-topics.sh --create \
  --topic my-topic \
  --partitions 3 \
  --replication-factor 3 \
  --bootstrap-server localhost:9092
```

**List all topics:**

```bash
bin/kafka-topics.sh --list \
  --bootstrap-server localhost:9092
```

**Describe a topic (show details):**

```bash
bin/kafka-topics.sh --describe \
  --topic my-topic \
  --bootstrap-server localhost:9092
```

**Delete a topic:**

```bash
bin/kafka-topics.sh --delete \
  --topic my-topic \
  --bootstrap-server localhost:9092
```

### Producing Messages (CLI)

**Produce simple messages (value only):**

```bash
bin/kafka-console-producer.sh \
  --topic my-topic \
  --bootstrap-server localhost:9092
> Hello World
> Another message
```

**Produce messages with keys:**

```bash
bin/kafka-console-producer.sh \
  --topic my-topic \
  --bootstrap-server localhost:9092 \
  --property parse.key=true \
  --property key.separator=:
> user-1:Order placed
> user-2:Payment received
> user-1:Order shipped
```

### Consuming Messages (CLI)

**Consume new messages only (from now):**

```bash
bin/kafka-console-consumer.sh \
  --topic my-topic \
  --bootstrap-server localhost:9092
```

**Consume all messages from the beginning:**

```bash
bin/kafka-console-consumer.sh \
  --topic my-topic \
  --from-beginning \
  --bootstrap-server localhost:9092
```

**Consume messages showing keys and values:**

```bash
bin/kafka-console-consumer.sh \
  --topic my-topic \
  --bootstrap-server localhost:9092 \
  --from-beginning \
  --property print.key=true \
  --property key.separator=:
```

> **Note**: When consuming from the beginning, messages from different partitions may appear interleaved (not in original send order). Messages within the same partition are always in order.

---

## 5. Kafka Producer — Spring Boot Microservice

### Project Setup

Create a new Spring Boot project with dependencies:

- **Spring Web** — REST API support
- **Spring for Apache Kafka** — Kafka integration

### Application Properties Configuration

```properties
# Kafka bootstrap server(s)
spring.kafka.producer.bootstrap-servers=localhost:9092,localhost:9094,localhost:9096

# Serializers — convert Java objects to byte arrays
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer

# Topic name
spring.kafka.producer.properties.topic.name=product-created-events-topic
```

**Key Configuration Properties:**

| Property            | Description                                           |
| ------------------- | ----------------------------------------------------- |
| `bootstrap-servers` | List of Kafka broker addresses for initial connection |
| `key-serializer`    | Converts message key (usually String) to bytes        |
| `value-serializer`  | Converts message value (Java object) to bytes         |

### Creating a Topic Programmatically

```java
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.producer.properties.topic.name}")
    private String topicName;

    @Bean
    NewTopic createTopic() {
        return TopicBuilder.name(topicName)
                .partitions(3)
                .replicas(3)
                .build();
    }
}
```

### Event Class (Message Payload)

```java
public class ProductCreatedEvent {
    private String productId;
    private String title;
    private BigDecimal price;
    private int quantity;

    // Constructors, Getters, Setters
}
```

### REST Controller

```java
@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public String createProduct(@RequestBody CreateProductRestModel product) {
        String productId = productService.createProduct(product);
        return productId;
    }
}
```

### Kafka Producer Service

```java
@Service
public class ProductServiceImpl implements ProductService {

    private final KafkaTemplate<String, ProductCreatedEvent> kafkaTemplate;

    @Value("${spring.kafka.producer.properties.topic.name}")
    private String topicName;

    public ProductServiceImpl(KafkaTemplate<String, ProductCreatedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public String createProduct(CreateProductRestModel product) {
        String productId = UUID.randomUUID().toString();

        ProductCreatedEvent event = new ProductCreatedEvent(
            productId, product.getTitle(), product.getPrice(), product.getQuantity()
        );

        // Asynchronous send (default)
        CompletableFuture<SendResult<String, ProductCreatedEvent>> future =
            kafkaTemplate.send(topicName, productId, event);

        future.whenComplete((result, exception) -> {
            if (exception != null) {
                logger.error("Failed to send message: {}", exception.getMessage());
            } else {
                logger.info("Message sent successfully. Topic: {}, Partition: {}, Offset: {}",
                    result.getRecordMetadata().topic(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
            }
        });

        // For synchronous send: future.join();

        return productId;
    }
}
```

### Asynchronous vs. Synchronous Send

| Mode                | Code                             | Behavior                                  |
| ------------------- | -------------------------------- | ----------------------------------------- |
| **Async** (default) | `kafkaTemplate.send(...)`        | Returns immediately; callback fires later |
| **Sync**            | `kafkaTemplate.send(...).join()` | Blocks until broker confirms receipt      |

**`SendResult` Metadata Available:**

- `getRecordMetadata().topic()` — topic name
- `getRecordMetadata().partition()` — partition number
- `getRecordMetadata().offset()` — message offset within partition

---

## 6. Producer Acknowledgements & Retries

### Producer Acknowledgements (`acks`)

**`acks`** controls how many broker replicas must confirm receipt before the producer considers a send successful.

| Value             | Name          | Behavior                     | Durability | Performance |
| ----------------- | ------------- | ---------------------------- | ---------- | ----------- |
| `acks=0`          | Fire & Forget | No acknowledgement waited    | ❌ Lowest  | ✅ Fastest  |
| `acks=1`          | Leader Only   | Only leader confirms         | ⚠️ Medium  | ⚠️ Medium   |
| `acks=all` / `-1` | All ISR       | All in-sync replicas confirm | ✅ Highest | ❌ Slowest  |

```
acks=0:  Producer ──send──► Broker (no response waited)
acks=1:  Producer ──send──► Leader confirms ──ack──► Producer
acks=all: Producer ──send──► Leader + All Followers confirm ──ack──► Producer
```

### `min.insync.replicas`

Works **with** `acks=all`. Specifies the **minimum number of replicas** that must be in-sync for a write to succeed.

```properties
# If replication-factor=3 and min.insync.replicas=2:
# → At least 2 brokers (including leader) must confirm the write.
# → If only 1 broker is available, producer gets NotEnoughReplicasException.
min.insync.replicas=2
```

> **Interview Tip**: Setting `acks=all` + `min.insync.replicas=2` with `replication-factor=3` is the **recommended production configuration** for durability without sacrificing too much performance.

### Producer Retries

When a send fails (e.g., network error, leader not available), the producer can automatically retry.

```properties
# Number of retry attempts (default: 2147483647 i.e., MAX_INT)
spring.kafka.producer.retries=10

# Time to wait between retries (default: 100ms)
spring.kafka.producer.properties.retry.backoff.ms=1000

# Max time for all delivery attempts (default: 120000ms = 2 min)
spring.kafka.producer.properties.delivery.timeout.ms=120000

# Time to wait for broker response per request (default: 30000ms = 30s)
spring.kafka.producer.properties.request.timeout.ms=30000

# Batching delay — time to wait before sending a batch (default: 0ms)
spring.kafka.producer.properties.linger.ms=0
```

```
Timeline of a send attempt:
├─ request.timeout.ms ─┤  (single request timeout)
├─── retry.backoff.ms ──┤  (wait between retries)
├──────────── delivery.timeout.ms ──────────────┤  (total time limit)
```

> **Key Rule**: `delivery.timeout.ms >= request.timeout.ms + linger.ms`

### Configuring via Java `@Bean` (Alternative to properties file)

```java
@Bean
Map<String, Object> producerConfigs() {
    Map<String, Object> config = new HashMap<>();
    config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
    config.put(ProducerConfig.ACKS_CONFIG, "all");
    config.put(ProducerConfig.RETRIES_CONFIG, 10);
    config.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 1000);
    config.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120000);
    config.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000);
    config.put(ProducerConfig.LINGER_MS_CONFIG, 0);
    return config;
}

@Bean
ProducerFactory<String, ProductCreatedEvent> producerFactory() {
    return new DefaultKafkaProducerFactory<>(producerConfigs());
}

@Bean
KafkaTemplate<String, ProductCreatedEvent> kafkaTemplate() {
    return new KafkaTemplate<>(producerFactory());
}
```

---

## 7. Idempotent Producer

### What is an Idempotent Producer?

An **Idempotent Producer** ensures that even if a message is sent **multiple times** (due to retries after network errors), Kafka will store it **only once**. This prevents **duplicate messages** in a topic.

### How Does It Work?

```
Without idempotency:
Producer ──send──► Broker (ack lost) ──retry──► Broker stores DUPLICATE

With idempotency:
Producer ──send──► Broker (ack lost) ──retry──► Broker detects duplicate → IGNORES
```

Kafka achieves this using:

- **Producer ID (PID)**: A unique ID assigned to each producer instance.
- **Sequence Number**: Each message gets a sequence number. Kafka tracks the last sequence number per PID/partition and rejects duplicates.

### Configuration

```properties
# Enable idempotent producer
spring.kafka.producer.properties.enable.idempotence=true
```

### Required Constraints for Idempotent Producer

When `enable.idempotence=true`, the following constraints **must** be satisfied:

| Property                                | Required Value | Reason                                          |
| --------------------------------------- | -------------- | ----------------------------------------------- |
| `acks`                                  | `all`          | All replicas must confirm to prevent duplicates |
| `retries`                               | > 0            | Must retry to benefit from deduplication        |
| `max.in.flight.requests.per.connection` | ≤ 5            | Limits concurrent unconfirmed requests          |

```java
config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
config.put(ProducerConfig.ACKS_CONFIG, "all");           // Required
config.put(ProducerConfig.RETRIES_CONFIG, 10);            // Must be > 0
config.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5); // Must be ≤ 5
```

> **Interview Tip**: "An idempotent producer prevents duplicate messages at the **broker level** by assigning each producer a unique ID and tracking sequence numbers per partition. It's the **producer-side** component of exactly-once semantics."

---

## 8. Kafka Consumer — Spring Boot Microservice

### Project Setup

Create a new Spring Boot project with dependencies:

- **Spring Web**
- **Spring for Apache Kafka**

### Application Properties Configuration

```properties
# Kafka bootstrap servers
spring.kafka.consumer.bootstrap-servers=localhost:9092,localhost:9094,localhost:9096

# Deserializers — convert byte arrays back to Java objects
spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer

# Consumer Group ID
spring.kafka.consumer.group-id=product-created-events

# Trusted packages for JSON deserialization
spring.kafka.consumer.properties.spring.json.trusted.packages=com.example.core.*
```

### Consuming Messages with `@KafkaListener` and `@KafkaHandler`

```java
@Component
@KafkaListener(topics = "product-created-events-topic")
public class ProductCreatedEventHandler {

    private static final Logger logger = LoggerFactory.getLogger(ProductCreatedEventHandler.class);

    @KafkaHandler
    public void handle(ProductCreatedEvent event) {
        logger.info("Received event: {}", event.getProductId());
        // Process the event...
    }
}
```

**Annotations Explained:**

| Annotation       | Purpose                                                                                                       |
| ---------------- | ------------------------------------------------------------------------------------------------------------- |
| `@KafkaListener` | Marks a class/method as a Kafka message listener. Specifies which topic(s) to listen to.                      |
| `@KafkaHandler`  | Marks a method inside a `@KafkaListener` class. Routes messages to the correct handler based on payload type. |

### Shared Core Module for Event Classes

When producers and consumers are in **different microservices**, share event classes via a common Maven module:

```
core-module/
  └── src/main/java/
       └── com.example.core.events/
            └── ProductCreatedEvent.java
```

Add as dependency to both producer and consumer `pom.xml`:

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>core</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Custom Consumer Factory Configuration (Java `@Bean`)

```java
@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.consumer.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @Bean
    ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "com.example.core.*");
        return new DefaultKafkaConsumerFactory<>(config);
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        return factory;
    }
}
```

---

## 9. Consumer Error Handling — Deserialization Errors

### The Problem

If a consumer receives a message it **cannot deserialize** (e.g., invalid JSON, wrong class), the default behavior is to throw an exception and the consumer gets stuck in a loop.

### Solution: `ErrorHandlingDeserializer`

The **`ErrorHandlingDeserializer`** is a **wrapper** around the actual deserializer. It catches deserialization exceptions and allows the consumer to continue processing other messages.

```properties
# Wrap key deserializer
spring.kafka.consumer.key-deserializer=org.springframework.kafka.support.serializer.ErrorHandlingDeserializer

# Wrap value deserializer
spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.ErrorHandlingDeserializer

# Specify the actual delegate deserializers
spring.kafka.consumer.properties.spring.deserializer.key.delegate.class=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.consumer.properties.spring.deserializer.value.delegate.class=org.springframework.kafka.support.serializer.JsonDeserializer
```

```
Message arrives → ErrorHandlingDeserializer
                    ├── Success → Delegate to JsonDeserializer → Handler method
                    └── Failure → Log error, skip message → Continue to next
```

> **Interview Tip**: "The `ErrorHandlingDeserializer` acts as a safety net. It catches deserialization failures gracefully so that one bad message doesn't crash the consumer."

---

## 10. Dead Letter Topic (DLT)

### What is a Dead Letter Topic?

A **Dead Letter Topic (DLT)** is a special Kafka topic where messages that **fail processing** are sent. Instead of losing failed messages or blocking the consumer, they are moved to the DLT for later analysis and reprocessing.

```
Normal flow:
Topic ──► Consumer ──► Process successfully ✅

With DLT:
Topic ──► Consumer ──► Process fails ❌ ──► Dead Letter Topic (DLT)
                                              └── Inspect later / reprocess
```

### Implementation with Spring Kafka

```java
@Configuration
public class KafkaConsumerConfig {

    // Consumer Factory (configured as before)...

    // Producer Factory for DLT (to publish failed messages)
    @Bean
    ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(config);
    }

    // KafkaTemplate for publishing to DLT
    @Bean
    KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    // Dead Letter Publishing Recoverer
    @Bean
    DeadLetterPublishingRecoverer deadLetterPublishingRecoverer(
            KafkaTemplate<String, Object> kafkaTemplate) {
        return new DeadLetterPublishingRecoverer(kafkaTemplate);
    }

    // Default Error Handler with DLT
    @Bean
    DefaultErrorHandler defaultErrorHandler(
            DeadLetterPublishingRecoverer recoverer) {
        return new DefaultErrorHandler(recoverer);
    }

    // Listener Container Factory with Error Handler
    @Bean
    ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory,
            DefaultErrorHandler errorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }
}
```

**DLT Naming Convention:**  
By default, the DLT is named: `<original-topic-name>.DLT`  
Example: `product-created-events-topic.DLT`

### Key Components

| Component                       | Purpose                                             |
| ------------------------------- | --------------------------------------------------- |
| `DefaultErrorHandler`           | Handles exceptions thrown during message processing |
| `DeadLetterPublishingRecoverer` | Publishes failed messages to the DLT                |
| `KafkaTemplate`                 | Used by the recoverer to send messages to DLT       |

---

## 11. Consumer Exceptions & Retries

### Retryable vs. Non-Retryable Exceptions

| Type              | Description                                  | Example                               | Action                  |
| ----------------- | -------------------------------------------- | ------------------------------------- | ----------------------- |
| **Retryable**     | Temporary failures that may succeed on retry | Network timeout, service unavailable  | Retry with backoff      |
| **Non-Retryable** | Permanent failures that will never succeed   | Invalid data, business rule violation | Send to DLT immediately |

### Custom Exception Classes

```java
// Non-retryable — skip retries, send to DLT immediately
public class NonRetryableException extends RuntimeException {
    public NonRetryableException(String message) {
        super(message);
    }
    public NonRetryableException(Throwable cause) {
        super(cause);
    }
}

// Retryable — retry a few times before giving up
public class RetryableException extends RuntimeException {
    public RetryableException(String message) {
        super(message);
    }
    public RetryableException(Throwable cause) {
        super(cause);
    }
}
```

### Configuring Retries with `DefaultErrorHandler`

```java
@Bean
DefaultErrorHandler defaultErrorHandler(DeadLetterPublishingRecoverer recoverer) {
    DefaultErrorHandler errorHandler = new DefaultErrorHandler(
        recoverer,
        new FixedBackOff(3000, 3)  // 3 retries, 3-second interval
    );

    // Tell the error handler which exceptions are NOT retryable
    errorHandler.addNotRetryableExceptions(NonRetryableException.class);

    return errorHandler;
}
```

```
Message processing throws exception:
├── NonRetryableException → Send to DLT immediately (no retries)
└── RetryableException → Retry 3 times (3s apart) → If still fails → Send to DLT
```

### Example: Retryable Exception with HTTP Call

```java
@KafkaHandler
public void handle(ProductCreatedEvent event) {
    try {
        // Call another microservice
        ResponseEntity<String> response = restTemplate.exchange(
            "http://localhost:8080/api/data",
            HttpMethod.GET, null, String.class
        );

        if (response.getStatusCode().is2xxSuccessful()) {
            logger.info("Successfully processed: {}", event.getProductId());
        }
    } catch (ResourceAccessException e) {
        // Network error — might succeed on retry
        logger.error("Retryable error: {}", e.getMessage());
        throw new RetryableException(e);
    } catch (HttpServerErrorException e) {
        // Server error — might succeed on retry
        logger.error("Retryable error: {}", e.getMessage());
        throw new RetryableException(e);
    } catch (Exception e) {
        // Unknown error — don't retry
        logger.error("Non-retryable error: {}", e.getMessage());
        throw new NonRetryableException(e);
    }
}
```

---

## 12. Consumer Groups & Rebalancing

### What is a Consumer Group?

A **Consumer Group** is a set of consumer instances that cooperate to consume messages from one or more topics. Each message in a partition is delivered to **only one consumer** within the group.

```
Topic: "orders" (3 partitions)

Consumer Group: "order-processors"
┌────────────┐  reads from  ┌──────────────┐
│ Consumer 1 │ ◄──────────── │ Partition 0  │
├────────────┤               ├──────────────┤
│ Consumer 2 │ ◄──────────── │ Partition 1  │
├────────────┤               ├──────────────┤
│ Consumer 3 │ ◄──────────── │ Partition 2  │
└────────────┘               └──────────────┘
```

### Partition Assignment Rules

1. **Each partition** is assigned to **exactly one consumer** within a group.
2. **One consumer** can read from **multiple partitions**.
3. If **consumers > partitions**, extra consumers sit **idle**.
4. If **consumers < partitions**, some consumers handle **multiple partitions**.

```
Scenario A: 3 partitions, 2 consumers
Consumer 1 → Partition 0, Partition 1
Consumer 2 → Partition 2

Scenario B: 3 partitions, 3 consumers (ideal)
Consumer 1 → Partition 0
Consumer 2 → Partition 1
Consumer 3 → Partition 2

Scenario C: 3 partitions, 4 consumers
Consumer 1 → Partition 0
Consumer 2 → Partition 1
Consumer 3 → Partition 2
Consumer 4 → IDLE ⚠️ (wasted resource)
```

> **Key Rule**: The maximum useful number of consumers in a group equals the number of partitions.

### Rebalancing

**Rebalancing** is the process by which Kafka **redistributes partitions** among consumers in a group. It happens when:

- A new consumer **joins** the group.
- An existing consumer **leaves** (crashes or gracefully shuts down).
- New partitions are **added** to the topic.
- A consumer fails to send a **heartbeat** within `session.timeout.ms`.
- A consumer exceeds **`max.poll.interval.ms`** between poll calls.

### Heartbeat Mechanism

Consumers send periodic **heartbeat** signals to the Kafka broker (Group Coordinator) to indicate they are alive.

```properties
# How often the consumer sends heartbeats (default: 3000ms)
heartbeat.interval.ms=3000

# Max time without heartbeat before consumer is considered dead (default: 45000ms)
session.timeout.ms=45000

# Max time between poll() calls before consumer is removed (default: 300000ms = 5min)
max.poll.interval.ms=300000
```

```
Consumer ──heartbeat──► Group Coordinator (Broker)
  Every 3s                   │
                             ├── Heartbeat received → Consumer is alive ✅
                             └── No heartbeat for 45s → Consumer dead → Rebalance ⚠️
```

### Configuring `group-id`

```properties
# In application.properties
spring.kafka.consumer.group-id=my-consumer-group
```

Or in Java code:

```java
config.put(ConsumerConfig.GROUP_ID_CONFIG, "my-consumer-group");
```

Or directly on the listener:

```java
@KafkaListener(topics = "my-topic", groupId = "my-consumer-group")
```

> **Interview Tip**: "Consumer groups enable horizontal scaling of message consumption. Kafka ensures that each message in a partition is processed by exactly one consumer in the group, providing both load balancing and fault tolerance."

---

## 13. Idempotent Consumer

### What is an Idempotent Consumer?

An **Idempotent Consumer** can process the **same message multiple times** without causing **side effects** or **data inconsistencies**. Even if a duplicate message arrives, the consumer processes it **only once**.

### When Do Duplicates Occur?

```
Scenario: Consumer timeout
1. Consumer reads message → starts processing (long operation)
2. Processing exceeds max.poll.interval.ms
3. Broker removes consumer from group → Rebalance
4. Same message delivered to another consumer
5. BOTH consumers complete processing → DUPLICATE!

Scenario: Non-idempotent producer
1. Producer sends message → ack lost
2. Producer retries → sends same message again
3. Consumer receives TWO copies of the same message
```

### Implementation Strategy

Use a **database table** to track processed message IDs:

```
┌──────────┐    ┌──────────────────────┐    ┌────────────┐
│  Kafka   │    │     Consumer         │    │  Database   │
│  Topic   │──► │                      │    │             │
│          │    │ 1. Read message ID   │    │ processed_  │
│          │    │ 2. Check DB: exists? │──► │ events      │
│          │    │    YES → skip        │    │ table       │
│          │    │    NO  → process     │    │             │
│          │    │ 3. Save message ID   │──► │             │
│          │    │ 4. Commit offset     │    │             │
└──────────┘    └──────────────────────┘    └────────────┘
```

### Step 1: Include a Unique ID in Message Header (Producer Side)

```java
// In Producer Service
public String createProduct(CreateProductRestModel product) {
    String productId = UUID.randomUUID().toString();
    ProductCreatedEvent event = new ProductCreatedEvent(/*...*/);

    // Create ProducerRecord with headers
    ProducerRecord<String, ProductCreatedEvent> record =
        new ProducerRecord<>(topicName, productId, event);

    // Add unique message ID to header
    record.headers().add("messageId",
        UUID.randomUUID().toString().getBytes());

    kafkaTemplate.send(record);
    return productId;
}
```

### Step 2: Read Message ID from Header (Consumer Side)

```java
@KafkaHandler
public void handle(
        @Payload ProductCreatedEvent event,
        @Header("messageId") String messageId,
        @Header(KafkaHeaders.RECEIVED_KEY) String messageKey) {

    logger.info("Received message with ID: {}", messageId);
    // Process event...
}
```

**Annotations for Reading Message Components:**

| Annotation                           | Purpose                        |
| ------------------------------------ | ------------------------------ |
| `@Payload`                           | Binds to message value/payload |
| `@Header("messageId")`               | Reads custom header by name    |
| `@Header(KafkaHeaders.RECEIVED_KEY)` | Reads the message key          |

### Step 3: Database Setup (H2 In-Memory)

**Dependencies (`pom.xml`):**

```xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
```

**Application Properties:**

```properties
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect
spring.h2.console.enabled=true
```

### Step 4: JPA Entity for Processed Events

```java
@Entity
@Table(name = "processed_events")
public class ProcessedEventEntity implements Serializable {

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false, unique = true)
    private String messageId;

    @Column(nullable = false)
    private String productId;

    // Constructors, Getters, Setters
}
```

### Step 5: JPA Repository

```java
@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEventEntity, Long> {
    ProcessedEventEntity findByMessageId(String messageId);
}
```

> Spring Data JPA automatically generates the SQL from the method name: `findByMessageId` → `SELECT * FROM processed_events WHERE message_id = ?`

### Step 6: Idempotent Consumer Handler

```java
@KafkaHandler
@Transactional
public void handle(
        @Payload ProductCreatedEvent event,
        @Header("messageId") String messageId,
        @Header(KafkaHeaders.RECEIVED_KEY) String messageKey) {

    // 1. Check if message was already processed
    ProcessedEventEntity existingRecord =
        processedEventRepository.findByMessageId(messageId);

    if (existingRecord != null) {
        logger.info("Duplicate message ID: {}. Skipping.", messageId);
        return;
    }

    // 2. Execute business logic
    logger.info("Processing event: {}", event.getProductId());
    // ... your business logic here ...

    // 3. Store message ID in database to prevent reprocessing
    try {
        processedEventRepository.save(
            new ProcessedEventEntity(messageId, event.getProductId())
        );
    } catch (DataIntegrityViolationException e) {
        throw new NonRetryableException(e);
    }
}
```

### Combined Approach: Idempotent Producer + Idempotent Consumer

For **exactly-once processing**, combine multiple techniques:

| Technique           | Prevents                                   |
| ------------------- | ------------------------------------------ |
| Idempotent Producer | Duplicate messages being **sent** to Kafka |
| Idempotent Consumer | Duplicate messages being **processed**     |
| Transactions        | Partial operations; ensures all-or-nothing |

> **Interview Tip**: "True exactly-once semantics requires a combination of idempotent provider, idempotent consumer, and transactional processing. No single technique alone guarantees exactly-once in all edge cases."

---

## 14. Apache Kafka Transactions

### Why Transactions?

Kafka transactions provide two guarantees:

1. **All-or-Nothing (Atomicity)**: Either all Kafka operations within a transaction succeed, or none take effect.
2. **Exactly-Once Delivery**: Combined with idempotent producers, ensures messages are neither lost nor duplicated.

### Transaction Flow

```
Without transactions:
Consumer ──read──► Process ──write Topic A──► ❌ CRASH ──write Topic B──► ✗
   Result: Topic A has message, Topic B doesn't → INCONSISTENCY

With transactions:
┌── Transaction Start ──────────────────────────────┐
│  Consumer ──read──► Process ──write Topic A──►     │
│                              ──write Topic B──►    │
│                              ──► Commit ✅         │
└────────────────────────────────────────────────────┘
   Result: Both writes visible atomically, or neither

With transactions (on error):
┌── Transaction Start ──────────────────────────────┐
│  Consumer ──read──► Process ──write Topic A──►     │
│                              ──► ❌ CRASH          │
│                              ──► Abort ⛔          │
└────────────────────────────────────────────────────┘
   Result: Topic A write is NOT committed → consumers don't see it
```

### Enable Kafka Transactions in Spring Boot

**1. Application Properties:**

```properties
# Enable transactions by setting transaction ID prefix
spring.kafka.producer.transaction-id-prefix=transfer-service-${random.value}-

# Logging for debugging
logging.level.org.springframework.kafka.transaction=DEBUG
logging.level.org.springframework.transaction=DEBUG
```

**2. Java Bean Configuration (if using custom ProducerFactory):**

```java
@Value("${spring.kafka.producer.transaction-id-prefix}")
private String transactionalIdPrefix;

@Bean
ProducerFactory<String, Object> producerFactory() {
    Map<String, Object> config = new HashMap<>();
    config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
    config.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, transactionalIdPrefix);
    // ... other configs
    DefaultKafkaProducerFactory<String, Object> factory =
        new DefaultKafkaProducerFactory<>(config);
    factory.setTransactionalIdPrefix(transactionalIdPrefix);
    return factory;
}

@Bean
KafkaTransactionManager<String, Object> kafkaTransactionManager(
        ProducerFactory<String, Object> producerFactory) {
    return new KafkaTransactionManager<>(producerFactory);
}
```

### Using `@Transactional` Annotation

```java
@Service
public class TransferServiceImpl implements TransferService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    @Transactional("kafkaTransactionManager")
    public void transfer(TransferDetails details) {
        // Send to withdrawal topic
        kafkaTemplate.send("withdraw-money-topic", details.getWithdrawal());

        // Call remote service (throws exception if unavailable)
        callRemoteService(details);

        // Send to deposit topic
        kafkaTemplate.send("deposit-money-topic", details.getDeposit());
    }
    // If exception occurs → both sends are rolled back
}
```

**`@Transactional` Behavior:**

| Scenario                      | Result                                           |
| ----------------------------- | ------------------------------------------------ |
| Method completes successfully | Transaction commits → both messages visible      |
| Exception thrown anywhere     | Transaction rolls back → neither message visible |

### Rollback Configuration

```java
// Default: rolls back on RuntimeException and Error
@Transactional

// Roll back for ALL exceptions:
@Transactional(rollbackFor = Throwable.class)

// Roll back for specific exceptions:
@Transactional(rollbackFor = {TransferException.class, ConnectException.class})

// Do NOT roll back for specific exceptions:
@Transactional(noRollbackFor = {SomeSpecificException.class})
```

### Reading Committed Messages (Consumer Side)

```properties
# Consumer reads ONLY committed messages
spring.kafka.consumer.isolation.level=read_committed
```

| Value                        | Behavior                                                     |
| ---------------------------- | ------------------------------------------------------------ |
| `read_uncommitted` (default) | Reads all messages, including uncommitted/aborted            |
| `read_committed`             | Reads only messages from successfully committed transactions |

### Local Transactions with `KafkaTemplate.executeInTransaction()`

An alternative to `@Transactional` for simple Kafka-only transactions:

```java
kafkaTemplate.executeInTransaction(t -> {
    t.send("withdraw-topic", withdrawalEvent);
    callRemoteService();  // Exception here → transaction aborts
    t.send("deposit-topic", depositEvent);
    return true;
});
```

**Difference: `@Transactional` vs. `executeInTransaction()`:**

| Feature              | `@Transactional`                 | `executeInTransaction()`            |
| -------------------- | -------------------------------- | ----------------------------------- |
| Scope                | Entire method                    | Lambda block only                   |
| Manager              | Spring's KafkaTransactionManager | Kafka's internal TransactionManager |
| Database integration | ✅ Can coordinate with JPA       | ❌ Kafka operations only            |
| Nesting              | Supports                         | Creates separate transaction        |

---

## 15. Kafka & Database Transactions

### The Challenge

When a method performs **both** Kafka operations and **database operations**, you need **two transaction managers**:

1. **KafkaTransactionManager** — manages Kafka transactions.
2. **JpaTransactionManager** — manages database (JPA) transactions.

### Transaction Synchronization

When you annotate a method with `@Transactional` and specify the **JPA Transaction Manager**, Spring's `KafkaTemplate` will **synchronize** its Kafka transaction with the JPA transaction:

```
@Transactional("transactionManager")  ← JPA Transaction Manager
┌──────────────────────────────────────────────────┐
│  save to DB      → managed by JpaTransactionMgr  │
│  send to Kafka   → synced with JPA transaction   │
│                                                    │
│  ✅ Success → DB commits first, then Kafka commits │
│  ❌ Exception → DB rolls back AND Kafka aborts    │
└──────────────────────────────────────────────────┘
```

### Configuration

```java
@Configuration
public class KafkaConfig {

    // Kafka Transaction Manager
    @Bean(name = "kafkaTransactionManager")
    KafkaTransactionManager<String, Object> kafkaTransactionManager(
            ProducerFactory<String, Object> producerFactory) {
        return new KafkaTransactionManager<>(producerFactory);
    }

    // JPA Transaction Manager (used as primary for synchronized transactions)
    @Bean(name = "transactionManager")
    JpaTransactionManager jpaTransactionManager(EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }
}
```

### Service Method with Both DB and Kafka Operations

```java
@Service
public class TransferServiceImpl implements TransferService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final TransferRepository transferRepository;

    @Override
    @Transactional("transactionManager")  // Use JPA Transaction Manager
    public void transfer(TransferRestModel details) {
        // Save to database
        TransferEntity entity = new TransferEntity();
        BeanUtils.copyProperties(details, entity);
        entity.setTransferId(UUID.randomUUID().toString());
        transferRepository.save(entity);

        // Send Kafka messages
        kafkaTemplate.send("withdraw-money-topic", details.getWithdrawal());
        kafkaTemplate.send("deposit-money-topic", details.getDeposit());

        // Call remote service
        callRemoteService(details);
    }
}
```

### Commit Order

When using synchronized transactions:

1. **Database transaction commits first**
2. **Kafka transaction commits second**

If the DB commit succeeds but the Kafka commit fails, the DB change persists but Kafka messages are not visible — this is a known edge case.

### Logging for Debugging

```properties
# JPA Transaction Manager logs
logging.level.org.springframework.orm.jpa.JpaTransactionManager=DEBUG

# Kafka Transaction Manager logs
logging.level.org.springframework.kafka.transaction.KafkaTransactionManager=DEBUG

# Kafka internal transaction manager
logging.level.org.apache.kafka.clients.producer.internals.TransactionManager=DEBUG

# Spring transaction infrastructure
logging.level.org.springframework.transaction=DEBUG
```

---

## 16. Saga Design Pattern with Apache Kafka

### What is the Saga Pattern?

The **Saga pattern** is a design pattern for managing **distributed transactions** across multiple microservices. Instead of using a single ACID transaction (impossible across services), a saga breaks the transaction into a sequence of **local transactions**, each performed by a different service.

### Two Types of Saga

#### 1. Choreography-Based Saga

Each microservice publishes events that trigger actions in other microservices. **No central coordinator**.

```
┌──────────┐   OrderCreated    ┌──────────┐   ProductReserved   ┌──────────┐
│  Orders  │ ────────────────► │ Products │ ──────────────────► │ Payments │
│          │                   │          │                     │          │
│          │ ◄──ShipmentCreated │          │ ◄─CardAuthFailed──  │          │
└──────────┘                   └──────────┘                     └──────────┘
                                     │
                                     ▼
                              ┌──────────┐
                              │ Shipment │
                              └──────────┘
```

**Characteristics:**

- Decentralized — each service knows only its own step.
- Simple for small flows.
- Hard to track and debug for complex workflows.

#### 2. Orchestration-Based Saga (Recommended)

A **central Saga orchestrator** coordinates the entire flow by sending commands and handling events.

```
                        ┌─────────────────┐
                        │   Order Saga    │
                        │  (Orchestrator) │
                        └────────┬────────┘
                  ┌──────────────┼──────────────┐
                  ▼              ▼              ▼
           ┌──────────┐  ┌──────────┐  ┌──────────┐
           │ Products │  │ Payments │  │ Shipment │
           └──────────┘  └──────────┘  └──────────┘
```

**Characteristics:**

- Centralized coordination — easier to understand the flow.
- Saga class manages the sequence of operations.
- Better for complex, multi-step workflows.
- Easier to implement compensating transactions.

### Orchestration-Based Saga — Happy Path

```
Step 1: Client ──HTTP POST──► Orders Service
          └── Create order → Publish OrderCreatedEvent

Step 2: OrderSaga handles OrderCreatedEvent
          └── Publish ReserveProductCommand → products-commands topic

Step 3: Products Service handles ReserveProductCommand
          └── Reserve product in stock
          └── Publish ProductReservedEvent → products-events topic

Step 4: OrderSaga handles ProductReservedEvent
          └── Publish ProcessPaymentCommand → payments-commands topic

Step 5: Payments Service handles ProcessPaymentCommand
          └── Process payment (charge credit card)
          └── Publish PaymentProcessedEvent → payments-events topic

Step 6: OrderSaga handles PaymentProcessedEvent
          └── Publish ApproveOrderCommand → orders-commands topic

Step 7: Orders Service handles ApproveOrderCommand
          └── Update order status to APPROVED
          └── Publish OrderApprovedEvent → orders-events topic

Step 8: OrderSaga handles OrderApprovedEvent
          └── Store order status in history table
          └── Saga complete ✅
```

### Flow Diagram (Happy Path)

```
Client
  │
  │ HTTP POST /orders
  ▼
┌───────────────┐
│ Orders Service│──OrderCreatedEvent──►┌──────────┐
│               │                     │ OrderSaga │
│               │◄─ApproveOrderCmd────│          │
└───────────────┘                     └────┬─────┘
                                           │
                    ReserveProductCmd       │     ProcessPaymentCmd
                           │               │            │
                           ▼               │            ▼
                    ┌──────────────┐       │     ┌──────────────┐
                    │Products Svc  │       │     │Payments Svc  │
                    │              │───────┘     │              │
                    │ ProductReservedEvent       │ PaymentProcessedEvent
                    └──────────────┘             └──────────────┘
```

### Kafka Topics Used in Saga

| Topic               | Purpose                             | Messages                                                                              |
| ------------------- | ----------------------------------- | ------------------------------------------------------------------------------------- |
| `orders-events`     | Order lifecycle events              | OrderCreatedEvent, OrderApprovedEvent                                                 |
| `orders-commands`   | Commands targeting orders service   | ApproveOrderCommand, RejectOrderCommand                                               |
| `products-commands` | Commands targeting products service | ReserveProductCommand, CancelProductReservationCommand                                |
| `products-events`   | Product lifecycle events            | ProductReservedEvent, ProductReservationFailedEvent, ProductReservationCancelledEvent |
| `payments-commands` | Commands targeting payments service | ProcessPaymentCommand                                                                 |
| `payments-events`   | Payment lifecycle events            | PaymentProcessedEvent, PaymentFailedEvent                                             |

### Saga Class Implementation

```java
@Component
@KafkaListener(topics = {
    "${orders-events-topic-name}",
    "${products-events-topic-name}",
    "${payments-events-topic-name}"
})
public class OrderSaga {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final OrderHistoryService orderHistoryService;

    @Value("${products-commands-topic-name}")
    private String productsCommandsTopicName;

    @Value("${payments-commands-topic-name}")
    private String paymentsCommandsTopicName;

    @Value("${orders-commands-topic-name}")
    private String ordersCommandsTopicName;

    // Constructor injection...

    @KafkaHandler
    public void handleOrderCreatedEvent(OrderCreatedEvent event) {
        ReserveProductCommand command = new ReserveProductCommand(
            event.getOrderId(), event.getProductId(), event.getProductQuantity()
        );
        kafkaTemplate.send(productsCommandsTopicName, command);
        orderHistoryService.add(event.getOrderId(), OrderStatus.CREATED);
    }

    @KafkaHandler
    public void handleProductReservedEvent(ProductReservedEvent event) {
        ProcessPaymentCommand command = new ProcessPaymentCommand(
            event.getOrderId(), event.getProductId(),
            event.getProductPrice(), event.getProductQuantity()
        );
        kafkaTemplate.send(paymentsCommandsTopicName, command);
    }

    @KafkaHandler
    public void handlePaymentProcessedEvent(PaymentProcessedEvent event) {
        ApproveOrderCommand command = new ApproveOrderCommand(event.getOrderId());
        kafkaTemplate.send(ordersCommandsTopicName, command);
    }

    @KafkaHandler
    public void handleOrderApprovedEvent(OrderApprovedEvent event) {
        orderHistoryService.add(event.getOrderId(), OrderStatus.APPROVED);
        // Saga complete!
    }
}
```

### Command Handler Example (Products Service)

```java
@Component
@KafkaListener(topics = "${products-commands-topic-name}")
public class ProductCommandsHandler {

    private final ProductService productService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${products-events-topic-name}")
    private String productsEventsTopicName;

    @KafkaHandler
    public void handleReserveProductCommand(ReserveProductCommand command) {
        Product product = new Product(command.getProductId(), command.getProductQuantity());

        try {
            productService.reserve(product);

            // Success → publish ProductReservedEvent
            ProductReservedEvent event = new ProductReservedEvent(
                command.getOrderId(), command.getProductId(),
                product.getPrice(), command.getProductQuantity()
            );
            kafkaTemplate.send(productsEventsTopicName, event);

        } catch (InsufficientQuantityException e) {
            // Failure → publish ProductReservationFailedEvent
            ProductReservationFailedEvent event = new ProductReservationFailedEvent(
                command.getProductId(), command.getOrderId(),
                command.getProductQuantity()
            );
            kafkaTemplate.send(productsEventsTopicName, event);
        }
    }
}
```

---

## 17. Saga — Compensating Transactions

### What are Compensating Transactions?

**Compensating transactions** are operations that **undo** changes made by previously successful steps when a later step **fails**. They restore the system to a consistent state.

### Key Rules

1. **Reverse order**: Compensating transactions execute in the **reverse order** of the original operations.
2. **Only for modifying operations**: Read-only steps don't need compensation.
3. **History preservation**: Never delete or modify history records — add new records instead.

```
Forward flow (happy path):
  Step 1: Create Order     →  Step 2: Reserve Product  →  Step 3: Process Payment
                                                              ❌ FAILS!

Compensating flow (reverse order):
  Comp 2: Cancel Reservation  ←  Comp 1: Reject Order
```

### Compensation Flow Diagram

```
PaymentFailed
      │
      ▼
┌──────────┐  CancelProductResCmd   ┌──────────┐
│ OrderSaga│ ─────────────────────► │ Products │
│          │                        │ Service  │
│          │ ◄─ProductResCancelled── │          │
│          │                        └──────────┘
│          │
│          │  RejectOrderCmd        ┌──────────┐
│          │ ─────────────────────► │ Orders   │
│          │                        │ Service  │
└──────────┘                        └──────────┘
```

### Implementation: Handle PaymentFailedEvent

```java
// In OrderSaga class
@KafkaHandler
public void handlePaymentFailedEvent(PaymentFailedEvent event) {
    // Publish command to cancel product reservation
    CancelProductReservationCommand command = new CancelProductReservationCommand(
        event.getProductId(), event.getOrderId(), event.getProductQuantity()
    );
    kafkaTemplate.send(productsCommandsTopicName, command);
}
```

### Implementation: Handle CancelProductReservationCommand

```java
// In ProductCommandsHandler class
@KafkaHandler
public void handleCancelProductReservationCommand(
        CancelProductReservationCommand command) {

    Product product = new Product(command.getProductId(), command.getProductQuantity());
    productService.cancelReservation(product, command.getOrderId());

    // Publish event that reservation was cancelled
    ProductReservationCancelledEvent event = new ProductReservationCancelledEvent(
        command.getProductId(), command.getOrderId()
    );
    kafkaTemplate.send(productsEventsTopicName, event);
}
```

### Implementation: Handle ProductReservationCancelledEvent → Reject Order

```java
// In OrderSaga class
@KafkaHandler
public void handleProductReservationCancelledEvent(
        ProductReservationCancelledEvent event) {

    // Send command to reject the order
    RejectOrderCommand command = new RejectOrderCommand(event.getOrderId());
    kafkaTemplate.send(ordersCommandsTopicName, command);

    // Update order history
    orderHistoryService.add(event.getOrderId(), OrderStatus.REJECTED);
}
```

### Implementation: Handle RejectOrderCommand

```java
// In OrderCommandsHandler class
@KafkaHandler
public void handleRejectOrderCommand(RejectOrderCommand command) {
    orderService.rejectOrder(command.getOrderId());
}

// In OrderServiceImpl
public void rejectOrder(String orderId) {
    OrderEntity order = orderRepository.findById(orderId)
        .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
    order.setStatus(OrderStatus.REJECTED);
    orderRepository.save(order);
}
```

### Order Status Flow — Complete Picture

```
Happy Path:
  CREATED → APPROVED

Compensation Path (payment failure):
  CREATED → REJECTED

Full Order History Example (happy):
  ┌─────────┬──────────┬─────────────────────┐
  │ OrderID │  Status  │     Timestamp       │
  ├─────────┼──────────┼─────────────────────┤
  │  abc123 │ CREATED  │ 2024-01-15 10:00:00 │
  │  abc123 │ APPROVED │ 2024-01-15 10:00:05 │
  └─────────┴──────────┴─────────────────────┘

Full Order History Example (compensated):
  ┌─────────┬──────────┬─────────────────────┐
  │ OrderID │  Status  │     Timestamp       │
  ├─────────┼──────────┼─────────────────────┤
  │  def456 │ CREATED  │ 2024-01-15 11:00:00 │
  │  def456 │ REJECTED │ 2024-01-15 11:00:08 │
  └─────────┴──────────┴─────────────────────┘
```

### Handle ProductReservationFailedEvent (Insufficient Stock)

```java
// In OrderSaga class
@KafkaHandler
public void handleProductReservationFailedEvent(
        ProductReservationFailedEvent event) {

    // No need to cancel reservation — it never happened
    // Just reject the order directly
    RejectOrderCommand command = new RejectOrderCommand(event.getOrderId());
    kafkaTemplate.send(ordersCommandsTopicName, command);

    orderHistoryService.add(event.getOrderId(), OrderStatus.REJECTED);
}
```

> **Note**: When `ProductReservationFailedEvent` occurs, we don't need to compensate because no product was actually reserved. We skip straight to rejecting the order.

### Event Sourcing Connection

The order history table follows **event sourcing** principles:

- Every state change is recorded as a new event.
- History is never modified or deleted.
- You can reconstruct the state of any entity at any point in time.
- Extremely valuable for **auditing**, **debugging**, and **compliance**.

---

## 18. Interview Quick-Reference Cheat Sheet

### Core Concepts

| Term               | Definition                                                                                       |
| ------------------ | ------------------------------------------------------------------------------------------------ |
| **Apache Kafka**   | Distributed event streaming platform for building real-time, event-driven applications           |
| **Broker**         | A single Kafka server instance that stores and serves messages                                   |
| **Topic**          | Named channel/category to which messages are published                                           |
| **Partition**      | Ordered, immutable sequence of messages within a topic; unit of parallelism                      |
| **Offset**         | Sequential ID of a message within a partition                                                    |
| **Consumer Group** | Set of consumers that cooperate to consume from a topic; each partition assigned to one consumer |
| **Rebalancing**    | Redistribution of partitions among consumers when group membership changes                       |
| **KRaft**          | Kafka's built-in consensus protocol replacing ZooKeeper                                          |
| **ISR**            | In-Sync Replicas — replicas fully caught up with the leader                                      |

### Producer Concepts

| Term                      | Definition                                                           |
| ------------------------- | -------------------------------------------------------------------- |
| **`acks=all`**            | Wait for all ISR to confirm write; highest durability                |
| **`min.insync.replicas`** | Minimum number of replicas that must confirm for `acks=all`          |
| **Idempotent Producer**   | Prevents duplicate messages using PID + sequence numbers             |
| **`delivery.timeout.ms`** | Total time allowed for a message to be delivered (including retries) |
| **`KafkaTemplate`**       | Spring wrapper around Kafka Producer for sending messages            |

### Consumer Concepts

| Term                            | Definition                                                        |
| ------------------------------- | ----------------------------------------------------------------- |
| **`@KafkaListener`**            | Annotation to subscribe a class/method to Kafka topics            |
| **`@KafkaHandler`**             | Routes messages to correct handler based on payload type          |
| **`ErrorHandlingDeserializer`** | Wrapper that catches deserialization failures gracefully          |
| **Dead Letter Topic (DLT)**     | Topic where failed messages are sent for later inspection         |
| **`DefaultErrorHandler`**       | Configurable error handler with retry + DLT support               |
| **`max.poll.interval.ms`**      | Max time between poll calls before consumer is removed from group |

### Transaction Concepts

| Term                        | Definition                                                  |
| --------------------------- | ----------------------------------------------------------- |
| **`@Transactional`**        | Spring annotation to execute a method within a transaction  |
| **KafkaTransactionManager** | Manages Kafka-specific transactions                         |
| **JpaTransactionManager**   | Manages database (JPA) transactions                         |
| **`read_committed`**        | Consumer isolation level that reads only committed messages |
| **`transaction-id-prefix`** | Unique prefix for transactional producer identification     |

### Saga Pattern Concepts

| Term                         | Definition                                                                |
| ---------------------------- | ------------------------------------------------------------------------- |
| **Saga Pattern**             | Design pattern for distributed transactions across microservices          |
| **Choreography**             | Decentralized saga — services communicate via events, no coordinator      |
| **Orchestration**            | Centralized saga — a saga class coordinates the entire flow               |
| **Compensating Transaction** | Operation that undoes a previously completed step when a later step fails |
| **Command**                  | Message that tells a service to perform an action (imperative)            |
| **Event**                    | Message that notifies that something has happened (past tense)            |

### Quick Property Reference

```properties
# ── PRODUCER ──
spring.kafka.producer.bootstrap-servers=localhost:9092
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer
spring.kafka.producer.acks=all
spring.kafka.producer.retries=10
spring.kafka.producer.properties.enable.idempotence=true
spring.kafka.producer.properties.max.in.flight.requests.per.connection=5
spring.kafka.producer.properties.delivery.timeout.ms=120000
spring.kafka.producer.properties.request.timeout.ms=30000
spring.kafka.producer.properties.retry.backoff.ms=1000
spring.kafka.producer.properties.linger.ms=0
spring.kafka.producer.transaction-id-prefix=my-service-

# ── CONSUMER ──
spring.kafka.consumer.bootstrap-servers=localhost:9092
spring.kafka.consumer.key-deserializer=org.springframework.kafka.support.serializer.ErrorHandlingDeserializer
spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.ErrorHandlingDeserializer
spring.kafka.consumer.properties.spring.deserializer.key.delegate.class=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.consumer.properties.spring.deserializer.value.delegate.class=org.springframework.kafka.support.serializer.JsonDeserializer
spring.kafka.consumer.group-id=my-consumer-group
spring.kafka.consumer.properties.spring.json.trusted.packages=com.example.*
spring.kafka.consumer.isolation.level=READ_COMMITTED
spring.kafka.consumer.properties.max.poll.interval.ms=300000
spring.kafka.consumer.properties.heartbeat.interval.ms=3000
spring.kafka.consumer.properties.session.timeout.ms=45000

# ── LOGGING ──
logging.level.org.springframework.kafka.transaction=DEBUG
logging.level.org.springframework.transaction=DEBUG
logging.level.org.springframework.orm.jpa.JpaTransactionManager=DEBUG
```

---

> **End of Notes**  
> These notes cover Sections 0–15, 18–19 of the Apache Kafka course, including fundamentals, Spring Boot integration, producer/consumer patterns, error handling, transactions, idempotency, consumer groups, and the Saga design pattern with compensating transactions.
