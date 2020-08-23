import json
import sys
import os
import argparse
import pathlib
import requests

import traceback
from confluent_kafka import Producer
from confluent_kafka import avro
from confluent_kafka.avro import AvroProducer
import configparser
from copy import deepcopy
from utils.kafka_helpers import import_json, prepare_record, fit_record_to_schema, value_schema, value_schema_str, dict_to_avro

parent_dir = pathlib.Path().absolute()


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

        self.rest_sent_to_schema_topic_count = 0
        self.producer_sent_to_schema_topic_count = 0
        self.producer_sent_to_schemaless_topic_count = 0

        # throws more errors. That's all it does for now
        self.debug_mode = True



    #######################
    # helpers
    #######################

    def send_one_record(self, record):
        """
        For excel, this will be called by rowFunction to send a single row.
        For json, this will send a single
        """
        prepared_record = prepare_record(record, self.data_type)
        # normalized_record = normalize_record(prepared_record)

        if self.key_for_topic is not None:
            topic = prepared_record[self.key_for_topic]
            del data_json[self.excel_topic_column_name]
        else:
            topic = self.default_topic


        try:
            if "avro-producer" in self.send_modes:
                self.send_one_with_schema(prepared_record, topic, "producer")

            # NOTE don't use elif, all of these should run if true
            if "rest-proxy" in self.send_modes:
                self.send_one_with_schema(prepared_record, topic, "rest-proxy")

            if "producer" in self.send_modes:
                self.send_one_without_schema(prepared_record, topic)

        except Exception as e:
            if self.debug_mode:
                # print(f"Failed producing record that has fields and types like:\n", self.schema_fields_for_record(record))

                raise e
            else:
                sys.stderr.write('%% Error while sending message to kafka %s\n' % str(e))
                traceback.print_exc()

        print(f"sent messages to topic with schema using producer ({self.producer_sent_to_schema_topic_count}) and over rest-proxy ({self.rest_sent_to_schema_topic_count}) and schemaless topic ({self.producer_sent_to_schemaless_topic_count})", flush=True)


    def send_one_with_schema(self, prepared_record, topic, send_using):
        """
        send one message to kafka to a topic, with avro schema
        - Can be sent using producer or using kafka rest-proxy
        """
        fit_message = fit_record_to_schema(prepared_record, debug_mode=self.debug_mode)

        avro_topic = "%s-avro" % topic
        if (send_using == 'producer'):
            self.avro_producer.produce(topic=avro_topic, value=fit_message)

            self.producer_sent_to_schema_topic_count += 1

        elif (send_using == 'rest-proxy'):
            # rest proxy requires a different format than AvroProducer
            avro_for_record = dict_to_avro(fit_message)

            rest_proxy_message_dict = {
                # they want this as a string, see here: 
                # https://docs.confluent.io/current/kafka-rest/api.html#post--topics-(string-topic_name)
                "value_schema": value_schema_str,

                # only sending one message at a time currently
                "records": [
                    {"value": avro_for_record}
                ],
            }

            json_message = json.dumps(rest_proxy_message_dict)

            print("sending headers", self.rest_proxy_avro_http_headers)
            res = requests.post(f"http://{self.rest_proxy_host}/topics/{avro_topic}", 
                                data=json_message,
                                #json=rest_proxy_message_dict,
                                headers=self.rest_proxy_avro_http_headers
                                )
            
            print("response text", res.text)
            print("response headers", res.headers)
            res.raise_for_status()

            self.rest_sent_to_schema_topic_count += 1

    def send_one_without_schema(self, prepared_record, topic):
        """
        currently only supports using producer
        """
        # send to topic without schema
        # schemaless requires just sending bytes. 
        self.producer.produce(topic, value=str(prepared_record))
        self.producer_sent_to_schemaless_topic_count += 1

        # TODO flush avro_producer also? or just don't flush
        self.producer.flush()


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
        props = configparser.ConfigParser()
        props.read(self.config_file_path)
        self.properties = props["DEFAULT"]

        # not necessarily set if we're not using rest proxy
        self.rest_proxy_host = self.properties.get('REST_PROXY_HOST', None)

        # array of strings indicating how we want to send to kafka (including whether to use schema or not)
        # also has bearing on what topics will be hit, since no schema == not hitting schema-less topics
        print(self.properties)
        self.send_modes = self.properties['SEND_USING']

        # headers for avro. Would need different headers for just sending schemaless
        self.rest_proxy_avro_http_headers = {
            'Content-type': 'application/vnd.kafka.avro.v2+json',
            #'Accept': 'application/vnd.kafka.avro.v2+json',
        }


        ####################
        # setup kafka

        # schema registry url
        sr_url = self.properties['SCHEMA_REGISTRY_URL']

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
            self.records = import_json(self.properties)

        elif self.data_type == "excel":
            # self.records = import_excel(self.properties)
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
