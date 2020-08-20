package org.anant

import org.apache.kafka.common.serialization.StringDeserializer
import java.util.Properties

import scala.io.Source

object KafkaUtil {

  def getProperties(projectProps : Properties, debugMode : Boolean): Properties = {
    /* 
     * sets properties for kafka
     */
    val props = new Properties()
    props.setProperty("bootstrap.servers", projectProps.getProperty("kafka.host"))
    props.setProperty("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer") 
    props.setProperty("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    props.setProperty("group.id", projectProps.getProperty("kafka.consumer.group"))
    props.setProperty("auto.offset.reset", if (debugMode) "earliest" else "latest")
    props.setProperty("enable.auto.commit", "false")

    if (debugMode) {
      println(s"Using Debug mode, so auto.offset.reset is ${props.getProperty("auto.offset.reset")}")
    }

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
