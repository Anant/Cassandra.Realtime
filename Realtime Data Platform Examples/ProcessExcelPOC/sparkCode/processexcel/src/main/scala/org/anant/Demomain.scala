package org.anant

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.log4j.LogManager
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import org.apache.spark.streaming.kafka010.{ConsumerStrategies, KafkaUtils, LocationStrategies}
import org.apache.spark.streaming.{Seconds, StreamingContext}

object Demomain {

  private val log = LogManager.getLogger(this.getClass.getSimpleName)

  def main(args: Array[String]): Unit = {

    //--------- Reading the Property File --------------------//
    if (args.length < 1)
      System.exit(1)

    //------------- Creating Variables ----------------------//
    val props = SparkUtil.getProperties(args(0))
    val custDataKeyspace = props.getProperty("customerkeyspace")
    val topic = props.getProperty("kafka.TestTopic")
    val groupId = props.getProperty("kafka.TestGroupId")

    //---------------- Spark Configuration ---------------------------//
    val sparkConf = new SparkConf()
      .setAppName("TestAppName")
      .setMaster("local[2]").set("spark.driver.host", "localhost")
      .set("spark.cassandra.connection.host", props.getProperty("cassandra.host"))

    val spark = SparkSession
      .builder
      .config(sparkConf)
      .getOrCreate()
    val ssc = new StreamingContext(spark.sparkContext, Seconds(2))

    //-------------- Kafka Definition ------------------------------------------//
    val kafkaParams = Map[String, Object](
      "bootstrap.servers" -> "localhost:29092",
      ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG -> classOf[StringDeserializer],
      ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG -> classOf[StringDeserializer],
      "group.id" -> groupId,
      "auto.offset.reset" -> "latest",
      "debug" -> "all"
    )

    val topicsArray = Array(topic)
    val inputDStream  = KafkaUtils.createDirectStream[String, String](
      ssc,
      LocationStrategies.PreferConsistent,
      ConsumerStrategies.Subscribe[String, String](topicsArray, kafkaParams))
    //------------ Kafka Data ------------------------------------------//

    inputDStream.foreachRDD(streamRdd => {
      if (streamRdd.count() > 0)
      {
        log.info("Processing Dstream RDD")

        //---------------- Read
        import spark.implicits._
        val messageDf = SparkUtil.processKafkaMessage(streamRdd.map(record => record.value()), spark)
        messageDf.createOrReplaceTempView("messageDfTable")
        val messageDS = messageDf.as[kafkaMessage]

        val sqlsteatement = "select * from messageDfTable where "
        val validDf =spark.sqlContext.sql(sqlsteatement)
        validDf.createOrReplaceTempView("validDfTable")
        val validDfForInsert =spark.sqlContext.sql("select message_date_time, message_type, message_value, message_id from validDfTable")

        validDfForInsert.write
          .format("org.apache.spark.sql.cassandra")
          .mode("append")
          .options(Map( "table" -> "message", "keyspace" -> custDataKeyspace))
          .save()

      }
    })
    ssc.start()
    ssc.awaitTermination()
  }
}
case class  kafkaMessage(message_date_time: String,
                         message_type: String,
                         message_value: String,
                         message_id: String)