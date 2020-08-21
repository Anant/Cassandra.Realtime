import json
import sys
import os
from datetime import datetime
import argparse
import pathlib

import traceback
import pandas as pd
from confluent_kafka import Producer
from confluent_kafka import avro
from confluent_kafka.avro import AvroProducer
from jproperties import Properties
from pandas import json_normalize

sr_url = "http://172.20.10.14:8081"

parent_dir = pathlib.Path().absolute()

absolute_path_to_schema = os.path.join(parent_dir, "../kafka", "leaves-record-schema.avsc")

value_schema = None
schema_dict = None
with open(absolute_path_to_schema, "r") as f:
    schema_dict = json.load(f)
    value_schema_str = json.dumps(schema_dict)
    # TODO do we have to do this if schema is in schema registry already? Or is this just a check within this job before we send it?
    value_schema = avro.loads(value_schema_str)

class DataImporter:
    """
    this method publishes the same message to 2 separate kafka topics
    1. a schema-less topic consumed by a spark job and written ta a cassandra table
    2. a schema-full topic consumed by dse-connector and written to a separate cassandra table

    TODOs
    - add support for excel importing
    """
    def __init__(self, **kwargs):
        # right now, json or excel. But really, excel version isn't fully built out yet, just outlined
        self.data_type = "json"

        # real config
        self.config_file_path = kwargs["config_file_path"]

        self.sent_to_schema_topic_count = 0
        self.sent_to_schemaless_topic_count = 0

        # throws more errors. That's all it does for now
        self.debug_mode = True


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
        self.records = data

    def import_excel(self):
        # excel_sheet_name = self.properties['SHEET_NAME']
        # sheets = pd.read_excel(self.file_name, sheet_name=excel_sheet_name)
        pass

    def prepare_record(self, record):
        if self.data_type == "excel":
            # create alias for record, "row" 
            # row = record
            # row.replace(['\u200b'], '')
            pass

        elif self.data_type == "json":
            # don't need to do anything yet
            return record

    def normalize_record(self, record):
        """
        use pandas to normalize json. 
        Might not be necessary for when self.data_type is json, but was used for when it was excel
        """
        message_df = pd.DataFrame(json_normalize(record))
        ### jr = message_df.drop([excel_topic_column_name], axis=1)
        json_str = message_df.to_json(orient='records')

        return json_str
    
    def record_to_message(self, record):
        """
        takes record dict and into message according to our schema
        - Currently only have to convert timestamps into unix timestamps
            Support only for one format: "2019-08-02T21:45:18Z"

        NOTE currently mutates original record. 

        TODO iterate over schema once per job, rather than once per record (?). Small performance boost.
        """
        # check all fields for logical type of timestamp-millis
        timestamp_fields = []

        for field_def in schema_dict["fields"]:
            field_name = field_def["name"]
            field_type = None
            if type(field_def["type"]) == dict:
                field_type = field_def["type"]["type"]
            else:
                field_type = field_def["type"]

            logical_type = field_def.get("logicalType", None)

            if logical_type == "timestamp-millis":
                timestamp_fields.append(field_name)

            # check if record has field
            if record.get(field_name, None) is None:

                # should work whether field_type is str or list (?) but hopefully no one is setting their field type to just a string called "null"!
                if "null" not in field_type:
                    print(f"\n\nDANGER: {field_name} is None\n\n")
                    raise Exception(f"field {field_name} is not defined for record {record['id']}, stopping right there")

        for field_name in timestamp_fields:
            # if currently value is string, try to convert to unix timestamp, with millisecond precision
            record_field_value = record[field_name]
            if type(record_field_value) == str:
                dt_obj = datetime.strptime(record_field_value, '%Y-%m-%dT%H:%M:%SZ')
                millisec = int(dt_obj.timestamp() * 1000)
                record[field_name] = millisec

        if self.debug_mode:
            # if record has extra field, throw error
            self.check_record_for_extra_field(record)

        return record


    def send_one_record(self, record):
        """
        For excel, this will be called by rowFunction to send a single row.
        For json, this will send a single
        """
        prepared_record = self.prepare_record(record)
        # normalized_record = self.normalize_record(prepared_record)

        if self.key_for_topic is not None:
            topic = prepared_record[self.key_for_topic]
            del data_json[self.excel_topic_column_name]
        else:
            topic = self.default_topic

        avro_topic = "%s-avro" % topic

        try:
            # send to topic with schema (avro)
            # value is a dict
            prepared_message = self.record_to_message(prepared_record)
            self.avro_producer.produce(topic=avro_topic, value=prepared_message)
            self.sent_to_schema_topic_count += 1

            # send to topic without schema
            # schemaless requires just sending bytes. 
            self.producer.produce(topic, value=str(prepared_record))
            self.sent_to_schemaless_topic_count += 1

            # TODO flush avro_producer also?
            self.producer.flush()

            print(f"sent message to schemaless topic with schema ({self.sent_to_schema_topic_count}) and schemaless topic ({self.sent_to_schemaless_topic_count})", flush=True)

        except Exception as e:
            if self.debug_mode:
                print(f"Failed producing record that has fields and types like:\n", self.schema_fields_for_record(record))
                print(f"Limit was {self.max_bytes_limit}")
                print(f"Bytes size of record as str was", sys.getsizeof(str(prepared_record)))

                record.keys()
                raise e
            else:
                sys.stderr.write('%% Error while sending message to kafka %s\n' % str(e))
                traceback.print_exc()

    def delivery_report(err, msg):
        """
        callback passed into avro producer, printing whether message failed or succeeded to send
        """
        if err is not None:
            print('Message delivery failed: {}'.format(err))
        else:
            print('Message delivered to {} [{}]'.format(msg.topic(), msg.partition()))

    ########################
    # debugging helpers (can remove)
    ########################
    def check_record_for_extra_field(self, record):
        """
        checks if record has a field that schema doesn't have
        """
        for key in record.keys():
            match = False
            for field_def in schema_dict["fields"]:
                if field_def["name"] == key:
                    match = True
                    break

            if not match:
                raise Exception(f"Record {record['id']} has extra field: {key}")

    def schema_fields_for_record(self, record):
        """
        for this record, produce the fields that it has and their types, for comparison with our actual schema
        presumably, if we wanted to we could also produce a schema with something similar to this. But not doing that for now
        """
        schema_fields = []
        for key, value in record.items():
            field_type = type(value).__name__
            schema_fields.append(
                {
                    "name": key,
                    "type": field_type,
                }
            )

        return schema_fields

    ######################
    # main operations
    #####################

    def setup(self):
        """
        - extract data from config (.ini) file
        - initialize producer(s)
        """
        print("started reading config", self.config_file_path)
        with open(self.config_file_path, "rb") as f:
            p = Properties()
            p.load(f, "utf-8")
            self.properties = p.properties
        f.close()
        print("finished reading")

        kafka_server = self.properties['BOOTSTRAP_SERVERS_LOCAL']

        # TODO add on_delivery callback here too?
        self.max_bytes_limit = 1500000
        self.producer = Producer({
            'bootstrap.servers': kafka_server,
            'message.max.bytes': self.max_bytes_limit, 
        })

        
        # default max_request_size is 1000000. We have some big messages, especially when sending non-avro messages 
        self.avro_producer = AvroProducer({
            'bootstrap.servers': kafka_server, 
            'schema.registry.url': sr_url, 
            'on_delivery': self.delivery_report,
            'message.max.bytes': self.max_bytes_limit, 
        }, default_value_schema=value_schema, )
        print("finished setting up producers")

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
            for record_dict in self.records:
                self.send_one_record(record_dict)

    #############################
    # main method
    #############################
    def run(self):
        print("== setup ==")
        self.setup()

        print("== import data ==")
        self.import_data()

        print("== send messages ==")
        self.send_messages()

if __name__ == '__main__':
    parser = argparse.ArgumentParser(
        description='import data from files into kafka',
        usage='{config-file-path}')

    parser.add_argument('--config-file-path', dest='config_file_path', help='path to your config.ini file')
    parser.set_defaults(config_file_path=f"{parent_dir}/configs/config.ini")

    args = parser.parse_args()

    import_job = DataImporter(**args.__dict__)
    import_job.run()
