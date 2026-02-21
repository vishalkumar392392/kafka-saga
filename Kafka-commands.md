# Kafka Commands Reference

## 1) Delete a topic

**Command**

```bash
./kafka-topics.sh --delete \
--topic topic-one \
--bootstrap-server localhost:9092
```

**What it is:** Kafka topic administration command (`kafka-topics.sh`).  
**What it does:** Deletes the topic `topic-one` from the cluster reachable via broker `localhost:9092`.

## 2) Create a topic

**Command**

```bash
./kafka-topics.sh --create \
--topic topic-one \
--partitions 3 \
--replication-factor 3 \
--bootstrap-server localhost:9092,localhost:9094
```

**What it is:** Topic creation command.  
**What it does:** Creates `topic-one` with 3 partitions and replication factor 3 using the listed bootstrap brokers.

## 3) List topics

**Command**

```bash
./kafka-topics.sh --list \
--bootstrap-server localhost:9092
```

**What it is:** Topic metadata lookup command.  
**What it does:** Prints all topic names currently available in the cluster.

## 4) Produce messages with KEY : VALUE

✅ Start Producer (key:value)

**Command**

```bash
./kafka-console-producer.sh \
 --bootstrap-server $BOOTSTRAP_SERVER \
 --topic product-created-events-topic \
 --property parse.key=true \
 --property key.separator=:
```

**What it is:** Interactive console producer.  
**What it does:** Sends messages to `product-created-events-topic` and treats input as `key:value` (for example `order-1:{...}`).

## 5) Consume messages WITH keys (generic topic)

**Command**

```bash
./kafka-console-consumer.sh \
 --bootstrap-server $BOOTSTRAP_SERVER \
 --topic test-topic \
 --from-beginning \
 --property print.key=true \
 --property key.separator=" : "
```

**What it is:** Console consumer for reading records.  
**What it does:** Reads `test-topic` from the beginning and prints each record key and value separated by `:`.

## 6) Consume DLT topic messages with keys

**Command**

```bash
./kafka-console-consumer.sh \
 --bootstrap-server localhost:9092 \
 --topic payments-commands-dlt \
 --from-beginning \
 --property print.key=true \
 --property key.separator=" : "
```

**What it is:** Console consumer targeting dead-letter topic (`DLT`).  
**What it does:** Reads all messages from `payments-commands-dlt` from the start and displays key/value output.

## 7) Generate a KRaft cluster UUID

**Command**

```bash
./bin/kafka-storage.sh random-uuid
```

**What it is:** KRaft storage utility command.  
**What it does:** Generates a random cluster UUID used when formatting Kafka log directories in KRaft mode.

## 8) Format storage directory for a broker (KRaft)

**Command**

```bash
./bin/kafka-storage.sh format -t C8Bw2RupRv2miB4Z0drAVA -c config/kraft/server-1.properties
```

**What it is:** Storage initialization command for a specific broker config.  
**What it does:** Formats broker log directories using the provided cluster ID and server config file.

## 9) Start a Kafka broker/server

**Command**

```bash
./bin/kafka-server-start.sh config/kraft/server-3.properties
```

**What it is:** Kafka server startup command.  
**What it does:** Starts a Kafka broker using `server-3.properties` configuration.

## 10) Delete all topics

**Command**

```bash
./kafka-topics.sh --bootstrap-server localhost:9092 --list | xargs -n 1 ./kafka-topics.sh --bootstrap-server localhost:9092 --delete --topic
```

**What it is:** Shell pipeline combining topic listing + deletion.  
**What it does:** Lists all topics, then deletes them one by one against the broker at `localhost:9092`.
