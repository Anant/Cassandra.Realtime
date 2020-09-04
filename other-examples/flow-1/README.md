# Reviewed version of realtime data platform

## Flow 1
<img src="https://github.com/xingh/cassandra.realtime/blob/master/diagrams/flow1.png"
 alt="flow1" width="800" style="float: left; margin-right: 10px;" />

1. a curl HTTP call will trigger python flask app
2. flask app reads the Excel file and sends the content to 2 separate kafka topics
3. using a terminal a spark job is submitted to dse to consume kafka messages from one of the topics and write them to cassandra table 
4. datastax kafka connector is configured to consume the other topic and directly write messages to a separate cassandra table 

#### PRE-STEP download datastax kafka connector
https://downloads.datastax.com/#akc

place `kafka-connect-dse-1.3.1.jar` next to this `README.md` file, because `docker-compose.yml` file expects to find it there.

In gitpod, you can drag this file into the file explorer from your desktop.

#### 1.1 Build docker image and run all docker containers
```
cd flow-1
docker-compose up
```
- cp-kafka
- cp-zookeeper
- kafka-connect
- schema-registry
- akhq (former kafka-hq)
- python-flask-app-for-kafka
- dse-6.7.7

#### 2.1 create kafka topics
```
docker exec -it cp_kafka_007 kafka-topics --create --zookeeper 172.20.10.11:2181 --replication-factor 1 --partitions 1 --topic testMessage
```
```
docker exec -it cp_kafka_007 kafka-topics --create --zookeeper 172.20.10.11:2181 --replication-factor 1 --partitions 1 --topic testMessage-avro
```

#### 2.2 check that both topics exist
```
docker exec -it cp_kafka_007 kafka-topics --list --zookeeper 172.20.10.11:2181
```

#### 2.3 create the schema for topic's messages value
make sure your python environment has `requests` module installed
```
python ./kafka/create-schema.py http://172.20.10.14:8081 testMessage-avro ./kafka/test-message.avsc

### check that the schema exists
curl http://127.0.0.1:8081/subjects
```
or alternatively you can check AKHQ for all kafka resources getting created at `http://127.0.0.1:8085/` 

#### 3.1 create cassandra keyspace and tables where spark and dse-connector will write the data
```
docker exec -it dse_007 cqlsh -e "CREATE KEYSPACE IF NOT EXISTS customerkeyspace WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1}; \
CREATE TABLE IF NOT EXISTS customerkeyspace.messages ( \
    message_insert_time text, \
    message_date_time text, \
    message_id text, \
    message_type text, \
    message_value text, \
    PRIMARY KEY(message_insert_time, message_date_time) \
);
CREATE TABLE IF NOT EXISTS customerkeyspace.messages_avro ( \
    message_insert_time timeuuid, \
    message_date_time text, \
    message_id text, \
    message_type text, \
    message_value text, \
    PRIMARY KEY(message_insert_time, message_date_time) \
);"
``` 

#### 3.2 execute the spark job to pick up messages from kafka, analyze and write them to cassandra
```
mvn -f ./spark/processexcel/pom.xml clean package
docker cp ./spark/processexcel/src/main/resources/spark.properties dse_007:/opt/dse/
docker cp ./spark/processexcel/target/processexcel-1.0-SNAPSHOT-jar-with-dependencies.jar dse_007:/tmp/processexcel-1.0-SNAPSHOT.jar

### For test, this spark job will count sum from 1 to 100 
docker exec -it dse_007 dse spark-submit --class org.anant.DemoNumbersSum --master dse://172.20.10.9 /tmp/processexcel-1.0-SNAPSHOT.jar

### You can of course run the spark job in standalone mode - but this is NOT FUN 
mvn -f ./spark/processexcel/pom.xml exec:java -Dexec.mainClass="org.anant.DemoKafkaConsumer"

### This is where the FUN part is, a spark job running on dse cluster will consume kafka messages and write them to cassandra 
docker exec -it dse_007 dse spark-submit --class org.anant.DemoKafkaConsumer --master dse://172.20.10.9 /tmp/processexcel-1.0-SNAPSHOT.jar spark.properties
```

Optionally open spark-ui in a browser to check jobs status at `http://127.0.0.1:4040/jobs/` or `http://127.0.0.1:7080` - spark master

#### 3.3 configure dse connector to write messages from kafka topic into cassandra table
```
./kafka/deploy-dse-connector.sh
```
check the connector exists
```
curl http://127.0.0.1:8084/connectors
```

### 4.1 Start triggering messages by calling python flask app rest api
```
curl -i http://127.0.0.1:5000/xls
```

#### 4.2 check the message arrived in kafka topics
check schema-less topic
```
docker exec -it cp_kafka_007 kafka-console-consumer --bootstrap-server localhost:9092 --topic testMessage --from-beginning
```
check schema-full topic
```
docker exec -it kafka-connect kafka-avro-console-consumer --topic testMessage-avro --bootstrap-server 172.20.10.12:9092 --from-beginning --property schema.registry.url=http://172.20.10.14:8081
```
hit the url a few times to generate more messages

#### 4.3 check cassandra records
```
docker exec -it dse_007 cqlsh -e "SELECT * FROM customerkeyspace.messages; SELECT * FROM customerkeyspace.messages_avro;"
```
Result should looks similar to the next block, both cassandra tables containe similar info, except one is filled by the spark job and the other by dse-kafka-connector.
```
 message_insert_time      | message_date_time | message_id   | message_type | message_value
--------------------------+-------------------+--------------+--------------+---------------
 2020-05-10 23:06:45+0000 |        1578416560 | PAC-34f572ae |         Test |           100
 2020-05-10 23:06:21+0000 |        1578416560 | PAC-34f572ae |         Test |           100
 2020-05-10 23:00:47+0000 |        1578416560 | PAC-34f572ae |         Test |           100

(3 rows)

 message_insert_time                  | message_date_time | message_id   | message_type | message_value
--------------------------------------+-------------------+--------------+--------------+---------------
 cb663930-9312-11ea-a4b2-c9b00f6c8b52 |        1578416560 | PAC-34f572ae |         Test |           100
 e9e22db0-9312-11ea-a4b2-c9b00f6c8b52 |        1578416560 | PAC-34f572ae |         Test |           100
 db47c670-9312-11ea-a4b2-c9b00f6c8b52 |        1578416560 | PAC-34f572ae |         Test |           100

(3 rows)
```

#### 4.4 truncate cassandra tables
Optionally truncate cassandra tables to start fresh
```
docker exec -it dse_007 cqlsh -e "TRUNCATE TABLE customerkeyspace.messages; TRUNCATE TABLE customerkeyspace.messages_avro;"
```
