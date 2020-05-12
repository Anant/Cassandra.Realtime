package org.davidcampos.kafka.commons;

public class Commons {
    public final static String EXAMPLE_KAFKA_TOPIC = System.getenv("EXAMPLE_KAFKA_TOPIC") != null ? System.getenv("EXAMPLE_KAFKA_TOPIC") : "example";
    public final static String EXAMPLE_KAFKA_SERVER = System.getenv("EXAMPLE_KAFKA_SERVER") != null ? System.getenv("EXAMPLE_KAFKA_SERVER") : "localhost:9092";
    public final static String EXAMPLE_ZOOKEEPER_SERVER = System.getenv("EXAMPLE_ZOOKEEPER_SERVER") != null ? System.getenv("EXAMPLE_ZOOKEEPER_SERVER") : "localhost:32181";
}
