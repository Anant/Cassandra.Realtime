import json
import sys

import pandas as pd
from confluent_kafka import Producer
from confluent_kafka import avro
from confluent_kafka.avro import AvroProducer
from jproperties import Properties
from pandas import json_normalize

sr_url = "http://172.20.10.14:8081"
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


class DataImporter
    """
    this method publishes the same message to 2 separate kafka topics
    1. a schema-less topic consumed by a spark job and written ta a cassandra table
    2. a schema-full topic consumed by dse-connector and written to a separate cassandra table
    """
    def __init__(self):
        # right now, json or excel
        self.data_type = "json"

        # whether we will use metadata from within the data file itself to determine things such as topic name
        self.using_metadata_from_data_file = True

        self.config_file_name = "KafkaConfig.ini"
        self.file_name = self.properties['FILE_NAME']


    #######################
    # helpers
    #######################

    def import_json(self):
        json.load(self.file_name)
        self.path_to_items = self.properties['PATH_TO_ITEMS']

        self.messages = 

    def import_excel(self):
        # excel_sheet_name = self.properties['SHEET_NAME']
        # sheets = pd.read_excel(self.file_name, sheet_name=excel_sheet_name)
        pass

    def prepare_message(self, message):
        if self.data_type == "excel":
            # create alias for message, "row" 
            # row = message
            # row.replace(['\u200b'], '')
            # topic = row[self.excel_topic_column_name]
            # avro_topic = "%s-avro" % topic
            # del data_json[self.excel_topic_column_name]

        elif self.data_type == "json":

    def send_one_message(self, message):
        """
        For excel, this will be called by rowFunction to send a single row.
        For json, this will send a single
        """
        data_json_string = (row.to_json())
        data_json = json.loads(data_json_string)
        try:
            # send to topic with schema (avro)
            # value is a dict
            avro_producer.produce(topic=avro_topic, value=data_json)

            # send to topic without schema
            # value is a json string
            message_df = pd.DataFrame(json_normalize(data_json))
            ### jr = message_df.drop([excel_topic_column_name], axis=1)
            producer.produce(topic, value=message_df.to_json(orient='records'))

            producer.flush()

        except Exception as e:
            sys.stderr.write('%% Error while sending message to kafka %s\n' % str(e))

    def delivery_report(err, msg):
        """
        callback passed into avro producer, printing whether message failed or succeeded to send
        """
        if err is not None:
            print('Message delivery failed: {}'.format(err))
        else:
            print('Message delivered to {} [{}]'.format(msg.topic(), msg.partition()))

    ######################
    # main operations
    #####################


    def setup_kafka(self):
        """
        - extract data from config (.ini) file
        - initialize producer(s)
        """
        with open(self.config_file_name, "rb") as f:
            p = Properties()
            p.load(f, "utf-8")
            self.properties = p.properties
        f.close()

        kafka_server = self.properties['BOOTSTRAP_SERVERS_LOCAL']

        # TODO add on_delivery callback here too?
        self.producer = Producer({'bootstrap.servers': kafka_server})

        self.avro_producer = AvroProducer({'bootstrap.servers': kafka_server, 'schema.registry.url': sr_url, 'on_delivery': self.delivery_report},
                                          default_value_schema=value_schema)

        # set topic
        if self.data_type == "json":
            self.key_for_topic = self.properties['KEY_FOR_TOPIC']

        elif self.data_type == "excel":
            self.excel_topic_column_name = self.properties['TOPIC_COLUMN_NAME']


    def import_data(self):
        if self.data_type == "json":
            self.messages = import_json(self)

        elif self.data_type == "excel":
            # self.messages = import_excel(self)

    def send_messages(self):

        if self.data_type == "excel":
            # sheets.apply(rowFunction, axis=1)

        elif self.data_type == "json":
            for message in self.messages:
                send_one_message(message)

    def send_to_kafka(self):
        """
        using data.json
        """

    #############################
    # main method
    #############################
    def run(self):
        setup_kafka()
        load_data()
        delivery_report(err, msg)

#if __name__ == '__main__':
#    sendJsonToKafka()
