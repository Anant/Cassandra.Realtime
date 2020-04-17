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
    val messageColRenamed = SparkUtil.rename_cols(messageDf,columnMap,spark)
    messageColRenamed

  }




}
