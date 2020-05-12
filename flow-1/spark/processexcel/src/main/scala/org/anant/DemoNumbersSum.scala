package org.anant

import org.apache.log4j.LogManager
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession

object DemoNumbersSum extends App {
  private val log = LogManager.getLogger(this.getClass.getSimpleName)

  val sparkConf = new SparkConf()
    .setAppName("DemoNumbersSum")
    .set("spark.driver.host", "localhost")

  val spark = SparkSession
    .builder
    .config(sparkConf)
    .getOrCreate()

  val sc = spark.sparkContext
  val par100 = sc.parallelize(1 to 100)
  val sum100 = par100.sum

  println(f"SUM 1 to 100 = $sum100%1.0f")
}
