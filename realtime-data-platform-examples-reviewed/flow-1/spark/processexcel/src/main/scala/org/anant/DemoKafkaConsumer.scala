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

  val kafkaParams = Map[String, Object](
    "bootstrap.servers" -> "172.20.10.12:9092",
    "key.deserializer" -> classOf[StringDeserializer],
    "value.deserializer" -> classOf[StringDeserializer],
    "group.id" -> "use_a_separate_group_id_for_each_stream",
    "auto.offset.reset" -> "latest",
    "enable.auto.commit" -> (false: java.lang.Boolean)
  )

  val sparkConf = new SparkConf()
    .setAppName("DemoKafkaCassandra")
    .setMaster("local[2]")
    .set("spark.driver.host", "localhost")

  val spark = SparkSession
    .builder
    .config(sparkConf)
    .getOrCreate()

  val streamingContext = new StreamingContext(spark.sparkContext, Seconds(3))
  val topics = Array("testMessage")
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
        .options(Map("table" -> "messages", "keyspace" -> "customerkeyspace"))
        .save()
    }
  })

  streamingContext.start()
  streamingContext.awaitTermination()
}
