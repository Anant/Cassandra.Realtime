package org.anant

import java.io.FileInputStream
import java.util.Properties

import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, SparkSession}

object SparkUtil {
  def getProperties(filePath: String): Properties = {
    val inputFile = new FileInputStream(filePath)
    val props = new Properties()
    props.load(inputFile)
    props
  }

  def processKafkaMessage(row: RDD[String], spark: SparkSession): DataFrame = {
    val messageDf = spark.read.json(row)
    val columnMap = getColumnMapping()
    val messageColRenamed = SparkUtil.rename_cols(messageDf, columnMap, spark)
    messageColRenamed
  }

  def getColumnMapping(): Map[String, (String, Boolean)] = {
    Map(
      "MessageDateTime" -> ("message_date_time", true),
      "MessageType" -> ("message_type", true),
      "MessageID" -> ("message_id", true),
      "MessageValue" -> ("message_value", true))
  }

  def rename_cols(inputDataFrame: DataFrame, renameColumns: Map[String, (String, Boolean)], spark: SparkSession): DataFrame = {
    var selectStatement = "SELECT "
    val tablename = "TemporaryTable"
    val dfColumns = inputDataFrame.columns

    inputDataFrame.createOrReplaceTempView(tablename)

    for ((mapColumn, value) <- renameColumns) {
      val colRenameTo = value._1
      val isRequired = value._2

      if (dfColumns.contains(mapColumn)) {
        selectStatement = selectStatement + " " + mapColumn + " as " + colRenameTo + " ,"
      } else {
        selectStatement = selectStatement + " \"\" as  " + colRenameTo + " ,"
      }

    }

    selectStatement = selectStatement.dropRight(1)
    selectStatement = selectStatement + " from " + tablename

    spark.sqlContext.sql(selectStatement)
  }
}
