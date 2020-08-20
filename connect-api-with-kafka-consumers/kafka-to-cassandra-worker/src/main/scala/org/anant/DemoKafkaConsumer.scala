package org.anant

import org.apache.kafka.clients.consumer.KafkaConsumer
import java.util.{Collections, Properties}
import scalaj.http._
import scala.collection.JavaConversions._

import scala.io.Source

object DemoKafkaConsumer extends App {

  println("starting...")
	val flask_host = "http://localhost:5000"
  case class KafkaMessage(message_date_time: String,
                          message_type: String,
                          message_value: String,
                          message_id: String)

  if (args.length < 1) {
    println("A properties file is expected as 1st argument.")
    System.exit(1)
  }
  val filePath = args(0)

  val projectProps = new Properties()
  projectProps.load(Source.fromFile(filePath).bufferedReader())

  // extract out non-kafka configs
  val cassandraKeyspace = projectProps.getProperty("cassandra.keyspace")
  val cassandraTable = projectProps.getProperty("cassandra.table")
  val cassandraHost = projectProps.getProperty("cassandra.host")
  val topic = projectProps.getProperty("kafka.topic")

  // sets kafka properties based on project properties
  val kafkaProps = KafkaUtil.getProperties(projectProps)

  val consumer = new KafkaConsumer[String, String](kafkaProps);

	// make a single item list before passing to kafka
	val topics = Collections.singletonList(topic)
  consumer.subscribe(topics)

  println("begin polling...");
	var count = 0
	var totalRecordsFound = 0
  println("");

	while (true) {

		val records = consumer.poll(3000);
    count += 1
    totalRecordsFound += records.size
    val message = s"polling... (poll count: ${count}). Found ${records.size} new records. Total records processed: ${totalRecordsFound}"
    // backspaces to clear last message. 
    // add a couple more to message length in case previous message was longer
	  val clear = "\b"* (message.size + 2)

    print(clear);
    print(message);

		for (record <- records) {
			printf("offset = %d, key = %s, value = %s%n", record.offset(), record.key(), record.value());

			// then write to C* using our cassandra flask api
			val form = Seq(
        "key" -> "value"
      )
			val response: HttpResponse[String] = Http(s"${flask_host}/api/leaves").postForm(form).asString;

			print("body", response.body)
			print("code", response.code)
    }
  }
}
