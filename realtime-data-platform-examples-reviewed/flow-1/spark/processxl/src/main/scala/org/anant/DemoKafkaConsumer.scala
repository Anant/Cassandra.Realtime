package org.anant

import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import com.datastax.spark.connector._
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe
import org.apache.spark.streaming.kafka010.KafkaUtils
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.streaming.{Seconds, StreamingContext}

object DemoKafkaConsumer extends  App {

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
    .setMaster("local[1]")
    .set("spark.driver.host", "localhost")
    .set("spark.cassandra.connection.host", "127.0.0.1")

  val spark = SparkSession
    .builder
    .config(sparkConf)
    .enableHiveSupport()
    .getOrCreate()

  val streamingContext = new StreamingContext(spark.sparkContext, Seconds(3))
  val topics = Array("testMessage")
  val stream = KafkaUtils.createDirectStream[String, String](
    streamingContext,
    PreferConsistent,
    Subscribe[String, String](topics, kafkaParams)
  )
  import spark.implicits._
  // stream.print()
  stream.foreachRDD(rdd => {
    if (rdd.count() > 0) {
      val messageDf = SparkUtil.processKafkaMessage(rdd.map(record => record.value()), spark)
      val messageDataSource = messageDf.as[KafkaMessage]
      println("processing message recieved .. " + messageDataSource)

      import org.apache.spark.sql.functions._
      val msgWithTimeCol = messageDataSource
        .withColumn("message_insert_time", lit(current_timestamp()))
      msgWithTimeCol.createOrReplaceTempView("messageDfTable")
      val validDfForInsert =spark.sqlContext.sql("select message_insert_time, message_date_time, message_id, message_type,message_value from messageDfTable")


      validDfForInsert.rdd.saveToCassandra("customerkeyspace", "messages")
    }
  })

  streamingContext.start()
  streamingContext.awaitTermination()
}
