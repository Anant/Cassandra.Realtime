package org.anant

import org.apache.kafka.clients.consumer.KafkaConsumer
import java.util.{Collections, Properties}
import scalaj.http._
import scala.collection.JavaConversions._

import scala.io.Source

object DemoKafkaConsumer extends App {

  println("starting...")
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

  ////////////////////////////////
  // extract out non-kafka configs
	// api to send kafka events
	val apiHost = projectProps.getProperty("api.host")

  // cassandra stuff
  val cassandraKeyspace = projectProps.getProperty("cassandra.keyspace")
  val cassandraTable = projectProps.getProperty("cassandra.table")
  val topic = projectProps.getProperty("kafka.topic")
  val debugMode = projectProps.getProperty("debug-mode").toBoolean

  /////////////////////////////////
  // set kafka properties based on project properties
  val kafkaProps = KafkaUtil.getProperties(projectProps, debugMode)

  val consumer = new KafkaConsumer[String, String](kafkaProps);

  /////////////////////////////////
  // begin consuming from kafka
	
	// make a single item list before passing to kafka
	val topics = Collections.singletonList(topic)
  consumer.subscribe(topics)

  println(s"begin polling topics ${topics}...");
	var count = 0
	var totalRecordsFound = 0

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

    if (debugMode) {
      println("--------------------------")
      println("hitting endpoint:" + apiHost)
    }

		for (record <- records) {
			if (debugMode) {
        println("");
        printf("offset = %d, key = %s, value = %s%n", record.offset(), record.key(), record.value());
      }

			try {
        // then write to C* using our cassandra api
        // this is for our schemaless topic. So value is just a json string
        val data = record.value()

        // if data is null, just skip
        if (data != null) {
          // NOTE currently fails since JSON we're receiving/sending has properties within single quotes, not double quotes. 
          val response: HttpResponse[String] = Http(s"${apiHost}/api/leaves").postData(data).header("content-type", "application/json").asString;

          if (debugMode) {
            println(s"body: ${response.body}")
            println(s"code: ${response.code}")
          }
        }

      } catch {
        case unknown: Throwable => println("Got some other kind of Throwable exception: " + unknown)
        println
        if (debugMode) {
        //   throw unknown
          println("continuing...")
        }
      }
    }
  }
}
