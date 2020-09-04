package org.davidcampos.kafka.consumer;

import java.util.*;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.spark.streaming.kafka010.ConsumerStrategies;
import org.apache.spark.streaming.kafka010.KafkaUtils;
import org.apache.spark.streaming.kafka010.LocationStrategies;
import org.davidcampos.kafka.commons.Commons;
import scala.Tuple2;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.function.*;
import org.apache.spark.streaming.api.java.*;
import org.apache.spark.streaming.Durations;

public class KafkaSparkConsumerExample {
    private static final Logger logger = LogManager.getLogger(KafkaSparkConsumerExample.class);

    public static void main(final String... args) {
        // Configure Spark to connect to Kafka running on local machine
        Map<String, Object> kafkaParams = new HashMap<>();
        kafkaParams.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, Commons.EXAMPLE_KAFKA_SERVER);
        kafkaParams.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        kafkaParams.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        kafkaParams.put(ConsumerConfig.GROUP_ID_CONFIG, "SparkConsumerGroup");

        //Configure Spark to listen messages in topic test
        Collection<String> topics = Arrays.asList(Commons.EXAMPLE_KAFKA_TOPIC);

        SparkConf conf = new SparkConf()
                .setMaster("local[2]")
                .setAppName("SparkConsumerApplication");

        //Read messages in batch of 30 seconds
        JavaStreamingContext jssc = new JavaStreamingContext(conf, Durations.seconds(5));

        // Start reading messages from Kafka and get DStream
        final JavaInputDStream<ConsumerRecord<String, String>> stream =
                KafkaUtils.createDirectStream(jssc, LocationStrategies.PreferConsistent(),
                        ConsumerStrategies.Subscribe(topics, kafkaParams));

        // Read value of each message from Kafka and return it
        JavaDStream<String> lines = stream.map((Function<ConsumerRecord<String, String>, String>) kafkaRecord -> kafkaRecord.value());

        // Break every message into words and return list of words
        JavaDStream<String> words = lines.flatMap((FlatMapFunction<String, String>) line -> Arrays.asList(line.split(" ")).iterator());

        // Take every word and return Tuple with (word,1)
        JavaPairDStream<String, Integer> wordMap = words.mapToPair((PairFunction<String, String, Integer>) word -> new Tuple2<>(word, 1));

        // Count occurrence of each word
        JavaPairDStream<String, Integer> wordCount = wordMap.reduceByKey((Function2<Integer, Integer, Integer>) (first, second) -> first + second);

        //Print the word count
        wordCount.print();

        jssc.start();
        try {
            jssc.awaitTermination();
        } catch (InterruptedException e) {
            logger.error("An error occurred.", e);
        }
    }

}
