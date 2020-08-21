package org.anant

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer
import java.util.{Collections, Properties}
import scalaj.http._
import scala.collection.JavaConversions._

import scala.io.Source

object KafkaAvroConsumer extends App {

  println("starting...")
  case class KafkaMessage(message_date_time: String,
                          message_type: String,
                          message_value: String,
                          message_id: String)

  if (args.length < 1) {
    println("A properties file is expected as 1st argument.")
    System.exit(1)
  }
  val projectPropertiesFilePath = args(0)

  val projectProps = new Properties()
  projectProps.load(Source.fromFile(projectPropertiesFilePath).bufferedReader())

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
  // val schemaFilePath = projectProps.getProperty("kafka.schema.filepath")
  // val jsonFormatSchema = new String(Files.readAllBytes(Paths.get(schemaFilePath)))
  // trying more scala-like way
  // val jsonFormatSchema = Source.fromResource(schemaFilePath).mkString

  // val Schema.Parser parser = new Schema.Parser(jsonFormatSchema);

  val kafkaProps = KafkaUtil.getProperties(projectProps, debugMode)

  val consumer = new KafkaConsumer[String, GenericRecord](kafkaProps);

  /////////////////////////////////
  // begin consuming from kafka
	
	// make a single item list before passing to kafka
	val topics = Collections.singletonList(topic)
  consumer.subscribe(topics)

  println(s"begin polling topics ${topics}...");
	var count = 0
	var totalRecordsFound = 0
  var totalSuccesfulWrites = 0
  var totalFailedWrites = 0

  if (debugMode) {
    println("--------------------------")
    println("hitting endpoint:" + apiHost)
  }

	while (true) {

		val records = consumer.poll(3000);
    count += 1
    totalRecordsFound += records.size
    val message = s"polling... (poll count: ${count}). Found ${records.size} new records. Total records processed: ${totalRecordsFound}; Total successes: ${totalSuccesfulWrites}; Total Failures: ${totalFailedWrites}"
    // backspaces to clear last message. 
    // add a couple more to message length in case previous message was longer
	  val clear = "\b"* (message.size + 5)

    print(clear);
    print(message);

		for (record <- records) {
			try {
        // then write to C* using our cassandra api
        val avroData : GenericRecord = record.value()

		    // convert to json for sending over the wire
		    val jsonData : String = avroData.toString()

				if (debugMode) {
					println("");
          val truncatedVal = if (jsonData.size > 30) jsonData.substring(0, 30) else jsonData
					printf("offset = %d, key = %s, value = %s%n", record.offset(), record.key(), truncatedVal);
				}

        // if data is null, just skip
        if (jsonData != null) {
          // NOTE currently fails since JSON we're receiving/sending has properties within single quotes, not double quotes. 
          val f = Future {
            val response: HttpResponse[String] = Http(s"${apiHost}/api/leaves").postData(jsonData : String).header("content-type", "application/json").asString;

            if (199 < response.code && response.code < 300) {
              totalSuccesfulWrites += 1
            } else if (response.code > 399) {
              totalFailedWrites += 1
            } else {
              if (debugMode) {
                println(s"Another code: ${response.code}")
                println(s"Response: ${response.body}")
              }
            }
          }

          // for now, no reason to do anything with this future
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
