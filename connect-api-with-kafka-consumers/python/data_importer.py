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


class DataImporter:
    """
    this method publishes the same message to 2 separate kafka topics
    1. a schema-less topic consumed by a spark job and written ta a cassandra table
    2. a schema-full topic consumed by dse-connector and written to a separate cassandra table

    TODOs
    - add support for excel importing
    """
    def __init__(self):
        # right now, json or excel. But really, excel version isn't fully built out yet, just outlined
        self.data_type = "json"

        # whether we will use metadata from within the data file itself to determine things such as topic name
        self.using_metadata_from_data_file = True

        self.config_file_name = "config.ini"


    #######################
    # helpers
    #######################

    def import_json(self):
        data_json = None
        data_file_name = self.properties['DATA_FILE_NAME']
        with open(data_file_name) as read_file:
            data_json = json.load(read_file)

        # dig into the json to get the list we want
        path_to_items_str = self.properties['PATH_TO_ITEMS']
        path_to_items_list = path_to_items_str.split(".")

        # traverse the path
        data = data_json
        for segment in path_to_items_list:
            data = data[segment]

        # data should now be list of dicts, and each dict will be a message to send to kafka
        self.messages = data

    def import_excel(self):
        # excel_sheet_name = self.properties['SHEET_NAME']
        # sheets = pd.read_excel(self.file_name, sheet_name=excel_sheet_name)
        pass

    def prepare_message(self, message):
        if self.data_type == "excel":
            # create alias for message, "row" 
            # row = message
            # row.replace(['\u200b'], '')
            pass

        elif self.data_type == "json":
            # don't need to do anything yet
            return message

    def normalize_message(self, message):
        """
        use pandas to normalize json. 
        Might not be necessary for when self.data_type is json, but was used for when it was excel
        """
        message_df = pd.DataFrame(json_normalize(message))
        ### jr = message_df.drop([excel_topic_column_name], axis=1)
        return message_df.to_json(orient='records')

    def send_one_message(self, message):
        """
        For excel, this will be called by rowFunction to send a single row.
        For json, this will send a single
        """
        prepared_message = self.prepare_message(message)
        normalized_message = self.normalize_message(prepared_message)

        if self.key_for_topic is not None:
            topic = prepared_message[self.key_for_topic]
            del data_json[self.excel_topic_column_name]
        else:
            topic = self.default_topic

        avro_topic = "%s-avro" % topic

        try:
            # send to topic with schema (avro)
            # value is a dict
            self.avro_producer.produce(topic=avro_topic, value=prepared_message)

            # send to topic without schema
            self.producer.produce(topic, value=normalized_message)

            # TODO flush avro_producer also?
            self.producer.flush()

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

    def setup(self):
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

        # set topic, or key_for_topic if topic depends on the message itself
        # if both are set, then will just use key_for_topic
        self.default_topic = self.properties.get('DEFAULT_TOPIC', None)

        if self.data_type == "json":
            self.key_for_topic = self.properties.get('KEY_FOR_TOPIC', None)

        elif self.data_type == "excel":
            self.excel_topic_column_name = self.properties['TOPIC_COLUMN_NAME']


    def import_data(self):
        """
        import data from file into this python job so it's ready to be sent
        """
        if self.data_type == "json":
            self.import_json()

        elif self.data_type == "excel":
            # self.import_excel()
            pass

    def send_messages(self):

        if self.data_type == "excel":
            # sheets.apply(rowFunction, axis=1)
            pass

        elif self.data_type == "json":
            for message in self.messages:
                self.send_one_message(message)

    #############################
    # main method
    #############################
    def run(self):
        self.setup()
        self.import_data()
        self.send_messages()

if __name__ == '__main__':
    import_job = DataImporter()
    import_job.run()
