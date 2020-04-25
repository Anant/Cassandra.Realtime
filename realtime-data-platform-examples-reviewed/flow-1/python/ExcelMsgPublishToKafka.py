import json
import sys

import pandas as pd
from confluent_kafka import Producer
from jproperties import Properties
from pandas.io.json import json_normalize


def sendXlsFileToKafka():
    global excel_topic_column_name
    global producer

    def rowFunction(row):
        row.replace(['\u200b'], '')
        topic = row[excel_topic_column_name]
        jsonData = (row.to_json())
        jsonReadRow = json.loads(jsonData)
        jsonReadRow1 = pd.DataFrame(json_normalize(jsonReadRow))
        jr = jsonReadRow1.drop([excel_topic_column_name], axis=1)
        try:
            producer.produce(topic, value=jr.to_json(orient='records'))
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

    sheets = pd.read_excel(excel_file_name, sheet_name=excel_sheet_name)
    sheets.apply(rowFunction, axis=1)
