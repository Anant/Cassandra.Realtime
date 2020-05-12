package org.davidcampos.kafka.producer;

import kafka.admin.AdminUtils;
import kafka.admin.RackAwareMode;
import kafka.utils.ZKStringSerializer$;
import kafka.utils.ZkUtils;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.ZkConnection;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.davidcampos.kafka.commons.Commons;

import java.util.Properties;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class KafkaProducerExample {
    private static final Logger logger = LogManager.getLogger(KafkaProducerExample.class);

    public static void main(final String... args) {
        // Create topic
        createTopic();

        String[] words = new String[]{"one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten"};
        Random ran = new Random(System.currentTimeMillis());

        final Producer<String, String> producer = createProducer();
        int EXAMPLE_PRODUCER_INTERVAL = System.getenv("EXAMPLE_PRODUCER_INTERVAL") != null ?
                Integer.parseInt(System.getenv("EXAMPLE_PRODUCER_INTERVAL")) : 100;

        try {
            while (true) {
                String word = words[ran.nextInt(words.length)];
                String uuid = UUID.randomUUID().toString();

                ProducerRecord<String, String> record = new ProducerRecord<>(Commons.EXAMPLE_KAFKA_TOPIC, uuid, word);
                RecordMetadata metadata = producer.send(record).get();

                logger.info("Sent ({}, {}) to topic {} @ {}.", uuid, word, Commons.EXAMPLE_KAFKA_TOPIC, metadata.timestamp());

                Thread.sleep(EXAMPLE_PRODUCER_INTERVAL);
            }
        } catch (InterruptedException | ExecutionException e) {
            logger.error("An error occurred.", e);
        } finally {
            producer.flush();
            producer.close();
        }
    }

    private static Producer<String, String> createProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, Commons.EXAMPLE_KAFKA_SERVER);
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "KafkaProducerExample");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        return new KafkaProducer<>(props);
    }

    private static void createTopic() {
        int sessionTimeoutMs = 10 * 1000;
        int connectionTimeoutMs = 8 * 1000;

        ZkClient zkClient = new ZkClient(
                Commons.EXAMPLE_ZOOKEEPER_SERVER,
                sessionTimeoutMs,
                connectionTimeoutMs,
                ZKStringSerializer$.MODULE$);

        boolean isSecureKafkaCluster = false;
        ZkUtils zkUtils = new ZkUtils(zkClient, new ZkConnection(Commons.EXAMPLE_ZOOKEEPER_SERVER), isSecureKafkaCluster);

        int partitions = 1;
        int replication = 1;

        // Add topic configuration here
        Properties topicConfig = new Properties();
        if (!AdminUtils.topicExists(zkUtils, Commons.EXAMPLE_KAFKA_TOPIC)) {
            AdminUtils.createTopic(zkUtils, Commons.EXAMPLE_KAFKA_TOPIC, partitions, replication, topicConfig, RackAwareMode.Safe$.MODULE$);
            logger.info("Topic {} created.", Commons.EXAMPLE_KAFKA_TOPIC);
        } else {
            logger.info("Topic {} already exists.", Commons.EXAMPLE_KAFKA_TOPIC);
        }

        zkClient.close();
    }
}
