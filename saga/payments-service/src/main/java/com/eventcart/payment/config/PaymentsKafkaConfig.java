package com.eventcart.payment.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.transaction.KafkaTransactionManager;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.util.backoff.FixedBackOff;
import org.springframework.web.client.RestTemplate;

import com.eventcart.repo.exception.NonRetryableException;
import com.eventcart.repo.exception.RetryableException;

import jakarta.persistence.EntityManagerFactory;

@Configuration
public class PaymentsKafkaConfig {

	@Value("${spring.kafka.bootstrap-servers}")
	private String bootstrapServers;

	@Value("${spring.kafka.producer.key-serializer}")
	private String keySerializer;

	@Value("${spring.kafka.producer.value-serializer}")
	private String valueSerializer;

	@Value("${spring.kafka.producer.acks}")
	private String acks;

	@Value("${spring.kafka.producer.properties.delivery.timeout.ms}")
	private String deliveryTimeout;

	@Value("${spring.kafka.producer.properties.linger.ms}")
	private String linger;

	@Value("${spring.kafka.producer.properties.request.timeout.ms}")
	private String requestTimeout;

	@Value("${spring.kafka.producer.properties.enable.idempotence}")
	private boolean idempotence;

	@Value("${spring.kafka.producer.properties.max.in.flight.requests.per.connection}")
	private Integer inflightRequests;

	@Value("${spring.kafka.producer.transaction-id-prefix}")
	private String transactionalIdPrefix;

	@Value("${spring.kafka.consumer.key-deserializer}")
	private String keyDeserializer;

	@Value("${spring.kafka.consumer.value-deserializer}")
	private String valueDeserializer;

	@Value("${spring.kafka.consumer.group-id}")
	private String groupId;

	@Value("${spring.kafka.consumer.properties.spring.json.trusted.packages}")
	private String trustedpackages;

	@Value("${spring.kafka.consumer.isolation-level}")
	private String isolationLevel;

	@Value("${payments.events.topic.name}")
	private String paymentsEventsTopicName;

	Map<String, Object> producerConfigs() {
		Map<String, Object> config = new HashMap<>();

		config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, keySerializer);
		config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, valueSerializer);
		config.put(ProducerConfig.ACKS_CONFIG, acks);
		config.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, deliveryTimeout);
		config.put(ProducerConfig.LINGER_MS_CONFIG, linger);
		config.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, requestTimeout);
		config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, idempotence);
		config.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, inflightRequests);
		config.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, transactionalIdPrefix);

		return config;
	}

	@Bean
	ProducerFactory<String, Object> producerFactory() {
		return new DefaultKafkaProducerFactory<>(producerConfigs());
	}

	@Bean
	KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
		return new KafkaTemplate<String, Object>(producerFactory);
	}

//	@Bean(name = "kafkaTransactionManager")
//	KafkaTransactionManager<String, Object> kafkaTransactionManager(ProducerFactory<String, Object> producerFactory) {
//		return new KafkaTransactionManager<>(producerFactory);
//	}
//
//	@Bean(name = "transactionManager")
//	JpaTransactionManager jpaTransactionManager(EntityManagerFactory entityManagerFactory) {
//		return new JpaTransactionManager(entityManagerFactory);
//	}

	@Bean
	NewTopic paymentsEventsTopicName() {
		return TopicBuilder.name(paymentsEventsTopicName).partitions(3).replicas(3)
				.configs(Map.of("min.insync.replicas", "2")).build();
	}

	/** Consumer Configuration */

	@Bean
	ConsumerFactory<String, Object> consumerFactory() {

		Map<String, Object> config = new HashMap<>();
		config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, keyDeserializer);
		config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
		config.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, valueDeserializer);

		config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
		config.put(JacksonJsonDeserializer.TRUSTED_PACKAGES, trustedpackages);
		config.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, isolationLevel.toLowerCase());

		return new DefaultKafkaConsumerFactory<>(config);
	}

//	@Bean
//	ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
//			ConsumerFactory<String, Object> consumerFactory, KafkaTemplate<String, Object> kafkaTemplate) {
//		DefaultErrorHandler defaultErrorHandler = new DefaultErrorHandler(
//				new DeadLetterPublishingRecoverer(kafkaTemplate), new FixedBackOff(5000, 3));
//		defaultErrorHandler.addNotRetryableExceptions(NonRetryableException.class);
//		defaultErrorHandler.addRetryableExceptions(RetryableException.class);
//		ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
//		factory.setConsumerFactory(consumerFactory);
//		factory.setCommonErrorHandler(defaultErrorHandler);
//		return factory;
//	}

	@Bean
	RestTemplate restTemplate() {
		return new RestTemplate();
	}

}
