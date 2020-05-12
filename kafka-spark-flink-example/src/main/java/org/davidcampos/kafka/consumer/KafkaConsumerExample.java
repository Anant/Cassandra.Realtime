package org.davidcampos.kafka.consumer;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.davidcampos.kafka.commons.Commons;


import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class KafkaConsumerExample {
    private static final Logger logger = LogManager.getLogger(KafkaConsumerExample.class);

    public static void main(final String... args) {
        ConcurrentMap<String, Integer> counters = new ConcurrentHashMap<>();
        final Consumer<String, String> consumer = createConsumer();

        while (true) {
            final ConsumerRecords<String, String> consumerRecords = consumer.poll(1000);
            consumerRecords.forEach(record -> {
                String word = record.value();

                int count = counters.containsKey(word) ? counters.get(word) : 0;
                counters.put(word, ++count);

                logger.info("({}, {})", word, count);
            });
            consumer.commitAsync();
        }
    }

    private static Consumer<String, String> createConsumer() {
        // Create properties
        final Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, Commons.EXAMPLE_KAFKA_SERVER);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "KafkaConsumerGroup");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        // Create the consumer using properties
        final Consumer<String, String> consumer = new KafkaConsumer(props);

        // Subscribe to the topic.
        consumer.subscribe(Collections.singletonList(Commons.EXAMPLE_KAFKA_TOPIC));
        return consumer;
    }
}
