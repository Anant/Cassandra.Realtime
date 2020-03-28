# Needs to provide configuration file path to execute the code

import sys
import configparser
import json, ast
from json import dumps
# Uncomment below import to execute process using pegs ksfka
from confluent_kafka import Producer
from pandas.io.json import json_normalize
#from kafka import KafkaProducer
import pandas as pd
from time import sleep
from jproperties import Properties

# Pre-requiest : Topic's for which message to be sent need to be present in Kafka 
# Kafka topic need to be last column in sheet
args=sys.argv
config_FileName = args[1]

with open(config_FileName, "rb") as f:
    p = Properties()
    # properties = p.load(f)
    p.load(f, "utf-8")
    properties = p.properties
f.close()

# Read configuration file to get Kafka server, excel name, sheet name and name of the column having 'Topic' name
# Update the configuration file to reflect correct location of excel file and correct sheet name
config = configparser.ConfigParser()
config.read(config_FileName)

excel_name=properties['EXCEL_NAME']
sheet_name=properties['SHEET_NAME']
topic_column_name=properties['TOPIC_COLUMN_NAME']
server_local=properties['BOOTSTRAP_SERVERS_LOCAL']

sheets = pd.read_excel(excel_name,sheet_name=sheet_name)
producer = Producer({'bootstrap.servers': 'localhost:29092'})

def f(row):
    # topic = row['Kafka topic']
    rowRemoveExtra = row.replace(['\u200b'],'')
    topic = row[topic_column_name]
    jsonData = (row.to_json())
    print(jsonData)
    jsonReadRow = json.loads(jsonData)
    jsonReadRow1 = pd.DataFrame(json_normalize(jsonReadRow))
    # jr = jsonReadRow1.drop(['Kafka topic'], axis=1)
    jr = jsonReadRow1.drop([topic_column_name], axis=1)
    hrhead = jr.to_json(orient='records')
    try:
        print("writing record")
        producer.produce(topic, value=jr.to_json(orient='records'))
        print("about to flush")
        producer.flush()
        print("flush complete")
    except Exception as e:
        sys.stderr.write('%% Error while sending message to kafka %s\n' %str(e))

    # ------- Apply the function to every row ----------------#
sheets.apply(f, axis=1)

# ------- Flush data to kafka --------------------"
status = producer.flush()
print("flush status: {0}".format(status))
if status > 0:
    print("All commands have not completed")
