package org.anant

import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.streaming.kafka010._
import org.apache.spark.streaming.{Seconds, StreamingContext}

object DemoKafkaConsumer extends App {

  case class KafkaMessage(message_date_time: String,
                          message_type: String,
                          message_value: String,
                          message_id: String)

  if (args.length < 1) {
    println("A properties file is expected as 1st argument.")
    System.exit(1)
  }
  val props = SparkUtil.getProperties(args(0))
  val cassandraKeyspace = props.getProperty("cassandra.keyspace")
  val cassandraTable = props.getProperty("cassandra.table")
  val cassandraHost = props.getProperty("cassandra.host")
  val kafkaHost = props.getProperty("kafka.host")
  val topic = props.getProperty("kafka.topic")
  val groupId = props.getProperty("kafka.consumer.group")

  val kafkaParams = Map[String, Object](
    "bootstrap.servers" -> kafkaHost,
    "key.deserializer" -> classOf[StringDeserializer],
    "value.deserializer" -> classOf[StringDeserializer],
    "group.id" -> groupId,
    "auto.offset.reset" -> "latest",
    "enable.auto.commit" -> (false: java.lang.Boolean)
  )

  val sparkConf = new SparkConf()
    .setAppName("DemoKafkaCassandra")
    .setMaster("local[2]")
    .set("spark.driver.host", "localhost")
    .set("spark.cassandra.connection.host", cassandraHost)

  val spark = SparkSession
    .builder
    .config(sparkConf)
    .getOrCreate()

  val streamingContext = new StreamingContext(spark.sparkContext, Seconds(3))
  val topics = Array(topic)
  val stream = KafkaUtils.createDirectStream[String, String](
    streamingContext,
    PreferConsistent,
    Subscribe[String, String](topics, kafkaParams)
  )

  // stream.print()

  stream.foreachRDD(rdd => {
    if (rdd.count() > 0) {
      val messageDf = SparkUtil.processKafkaMessage(rdd.map(record => record.value()), spark)
      messageDf.createOrReplaceTempView("messageDfTable")

      import spark.implicits._
      val messageDataSource = messageDf.as[KafkaMessage]

      import org.apache.spark.sql.functions._
      messageDataSource
        .withColumn("message_insert_time", lit(current_timestamp()))
        .write
        .format("org.apache.spark.sql.cassandra")
        .mode("append")
        .options(Map("table" -> cassandraTable, "keyspace" -> cassandraKeyspace))
        .save()
    }
  })

  streamingContext.start()
  streamingContext.awaitTermination()
}
