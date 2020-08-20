package org.anant

import java.util.Properties

import scala.io.Source

object KafkaUtil {

  def getProperties(filePath: String): Properties = {
    val props = new Properties()
    props.load(Source.fromFile(filePath).bufferedReader())
    props
  }

  def getColumnMapping(): Map[String, (String, Boolean)] = {
    Map(
      "MessageDateTime" -> ("message_date_time", true),
      "MessageType" -> ("message_type", true),
      "MessageID" -> ("message_id", true),
      "MessageValue" -> ("message_value", true))
  }
}
