package org.anant

import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.clients.consumer.KafkaConsumer
import scalaj.http._

object DemoKafkaConsumer extends App {

	val flask_host = "http://localhost:5000"
  case class KafkaMessage(message_date_time: String,
                          message_type: String,
                          message_value: String,
                          message_id: String)

  if (args.length < 1) {
    println("A properties file is expected as 1st argument.")
    System.exit(1)
  }

	// TODO probably rename or split out SparkUtil
  val props = SparkUtil.getProperties(args(0))
  val cassandraKeyspace = props.getProperty("cassandra.keyspace")
  val cassandraTable = props.getProperty("cassandra.table")
  val cassandraHost = props.getProperty("cassandra.host")
  val kafkaHost = props.getProperty("kafka.host")
  val topic = props.getProperty("kafka.topic")
  val topics = Array(topic)
  val groupId = props.getProperty("kafka.consumer.group")

  val kafkaParams = Map[String, Object](
    "bootstrap.servers" -> kafkaHost,
    "key.deserializer" -> classOf[StringDeserializer],
    "value.deserializer" -> classOf[StringDeserializer],
    "group.id" -> groupId,
    "auto.offset.reset" -> "latest",
    "enable.auto.commit" -> (false: java.lang.Boolean)
  )

	KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
	consumer.subscribe(Arrays.asList("foo", "bar"));
	while (true) {
		ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
		for (ConsumerRecord<String, String> record : records)
			System.out.printf("offset = %d, key = %s, value = %s%n", record.offset(), record.key(), record.value());

			// then write to C* using our cassandra flask api
			val form = Seq(
        "key" -> "value",
      )
			val response: HttpResponse[String] = Http(s"${flask_host}/api/leaves").postForm(form).asString

			System.out.printf("body", response.body)
			System.out.printf("code", response.code)
}
