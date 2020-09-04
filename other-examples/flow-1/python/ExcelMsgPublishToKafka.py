import json
import sys

import pandas as pd
from confluent_kafka import Producer
from confluent_kafka import avro
from confluent_kafka.avro import AvroProducer
from jproperties import Properties
from pandas import json_normalize

sr_ulr = "http://172.20.10.14:8081"
value_schema_str = """
{
  "type": "record",
  "name": "MessageRecord",
  "namespace": "org.anant.test.schema01",
  "version": "1",
  "fields": [
    {
      "name": "MessageID",
      "type": "string"
    },
    {
      "name": "MessageType",
      "type": "string"
    },
    {
      "name": "MessageValue",
      "type": "long"
    },
    {
      "name": "MessageDateTime",
      "type": "long"
    }
  ]
}
"""
value_schema = avro.loads(value_schema_str)


# this method publishes the same message to 2 separate kafka topics
# 1. a schema-less topic consumed by a spark job and written ta a cassandra table
# 2. a schema-full topic consumed by dse-connector and written to a separate cassandra table
def sendXlsFileToKafka():
    global excel_topic_column_name
    global producer
    global avro_producer
    global value_schema

    def delivery_report(err, msg):
        if err is not None:
            print('Message delivery failed: {}'.format(err))
        else:
            print('Message delivered to {} [{}]'.format(msg.topic(), msg.partition()))

    def rowFunction(row):
        row.replace(['\u200b'], '')
        topic = row[excel_topic_column_name]
        avro_topic = "%s-avro" % topic
        data_json_string = (row.to_json())
        data_json = json.loads(data_json_string)
        del data_json[excel_topic_column_name]
        message_df = pd.DataFrame(json_normalize(data_json))
        ### jr = message_df.drop([excel_topic_column_name], axis=1)
        try:
            producer.produce(topic, value=message_df.to_json(orient='records'))
            avro_producer.produce(topic=avro_topic, value=data_json)
            producer.flush()
        except Exception as e:
            sys.stderr.write('%% Error while sending message to kafka %s\n' % str(e))

    config_file_name = "ExcelKafkaConfig.ini"

    with open(config_file_name, "rb") as f:
        p = Properties()
        p.load(f, "utf-8")
        properties = p.properties
    f.close()

    excel_file_name = properties['EXCEL_NAME']
    excel_sheet_name = properties['SHEET_NAME']
    excel_topic_column_name = properties['TOPIC_COLUMN_NAME']
    kafka_server = properties['BOOTSTRAP_SERVERS_LOCAL']

    producer = Producer({'bootstrap.servers': kafka_server})
    avro_producer = AvroProducer({'bootstrap.servers': kafka_server, 'schema.registry.url': sr_ulr, 'on_delivery': delivery_report},
                                 default_value_schema=value_schema)

    sheets = pd.read_excel(excel_file_name, sheet_name=excel_sheet_name)
    sheets.apply(rowFunction, axis=1)


#if __name__ == '__main__':
#    sendXlsFileToKafka()
