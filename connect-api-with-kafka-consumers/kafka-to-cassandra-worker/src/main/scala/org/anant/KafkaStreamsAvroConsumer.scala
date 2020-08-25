package org.anant

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;

import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.streams.scala.Serdes._
import io.confluent.kafka.serializers.{AbstractKafkaAvroSerDeConfig, KafkaAvroDeserializer, KafkaAvroSerializer}
import org.apache.kafka.clients.consumer.KafkaConsumer
import java.util.{Collections, Properties}
import scala.collection.JavaConversions._

import scala.io.Source

import org.apache.kafka.streams.scala.ImplicitConversions._
import org.apache.kafka.streams.scala._
import org.apache.kafka.streams.scala.kstream._
import org.apache.kafka.streams.{KafkaStreams, StreamsConfig}
// using generic avro, though for something like this where we know the schema already, specific is probably better
import io.confluent.kafka.streams.serdes.avro.GenericAvroSerde;


import java.time.Duration

object KafkaStreamsAvroConsumer extends App {
  /*
   * use Kafka streams to write to C* Db. 
   * - Note that there are pros and cons to this approach. See for example this response, which recommends using Kafka Connect: https://stackoverflow.com/a/46529152/6952495
   * - For a similar implementation using Kafka Connect, see separate Kafka Connect class within this directory
   * - We are mixing processor API for sending records to C*, with KStreams DSL where we can use its simpler API. See here for how to mix the two together: 
   *      https://docs.confluent.io/current/streams/developer-guide/dsl-api.html#applying-processors-and-transformers-processor-api-integration
   * - We are using Avro, so need Avro serde support
   *      https://docs.confluent.io/current/streams/developer-guide/datatypes.html#avro
   * - We are using Scala, so taking advantage of the kafka lib for scala
   *      - https://docs.confluent.io/current/streams/developer-guide/dsl-api.html#kstreams-dsl-for-scala
   *      - Their tests for scala provides helpful example as well: https://github.com/confluentinc/kafka-streams-examples/blob/5.5.1-post/src/test/scala/io/confluent/examples/streams/GenericAvroScalaIntegrationTest.scala#L82-L88
   *
   */

  if (args.length < 1) {
    println("A properties file is expected as 1st argument.")
    System.exit(1)
  }
  val projectPropertiesFilePath = args(0)

  val projectProps = new Properties()
  projectProps.load(Source.fromFile(projectPropertiesFilePath).bufferedReader())
  val topic = projectProps.getProperty("kafka.topic")
  val debugMode = projectProps.getProperty("debug-mode").toBoolean
  val kafkaProps = KafkaUtil.getProperties(projectProps, debugMode)

  // add implicit serde that will just magically (well...implicitly) get used
	implicit val genericAvroSerde: Serde[GenericRecord] = {
		val gas = new GenericAvroSerde
		val isKeySerde: Boolean = false
		gas.configure(Collections.singletonMap(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, kafkaProps.getProperty("schema.registry.url")), isKeySerde)
		gas
	}


  /////////////////////////////////
  // Setup streams builder

  val builder: StreamsBuilder = new StreamsBuilder

  // expecting keys as strings (though not using keys for now) and Avro Generic records for values
	// note that scala streams lib will ignore serdes declarations from the props, and use implicit serdes
  val recordStream: KStream[String, GenericRecord] = builder.stream(topic)

  println(s"begin streaming topic ${topic}...");
	// TODO send these to processor
	/* val counts : Map[String, Int] = Map(
    ("polls", 0),
    ("totalRecordsFound", 0),
    ("totalSuccesfulWrites", 0),
    ("totalFailedWrites", 0)
  )
  */

  // TODO send debugMode as option to processor
  // mixing processor api with streams api, to take advantage of streams DSL where we can and the processor API flexibility where we need to
  recordStream.process(() => new RecordProcessor(projectProps))

  // now that the builder has been setup, can start streaming
  val streams: KafkaStreams = new KafkaStreams(builder.build(), kafkaProps)
  streams.start()

  sys.ShutdownHookThread {
    streams.close(Duration.ofSeconds(60))
  }

}
