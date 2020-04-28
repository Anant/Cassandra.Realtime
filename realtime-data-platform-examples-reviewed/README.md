# Reviewed version of realtime data platform

## Flow 1
<img src="https://github.com/xingh/DART.POC/blob/master/realtime-data-platform-examples-reviewed/diagrams/flow1.png"
 alt="flow1" width="800" style="float: left; margin-right: 10px;" />

1. a curl HTTP call will trigger python flask app
2. flask app reads the Excel file and sends the content to kafka
3. using a terminal a spark job is submitted to dse to consume kafka messages and write them to cassandra 

#### 1.1 Build docker image and run all docker containers
```
cd flow-1
docker-compose up
```
- cp-kafka
- cp-zookeeper
- kafka-manager
- python-flask-app-for-kafka
- dse-6.7.7

#### 2.1 create kafka topic
```
docker exec -it cp_kafka_007 kafka-topics --create --zookeeper 172.20.10.11:2181 --replication-factor 1 --partitions 1 --topic testMessage
```

#### 2.2 check the topic exists
```
docker exec -it cp_kafka_007 kafka-topics --list --zookeeper 172.20.10.11:2181
```

#### 2.3 ask python flask app to send a message to kafka by reading the excel file
```
curl -i http://127.0.0.1:5000/xls
```

#### 2.4 check the message arrived in kafka
```
docker exec -it cp_kafka_007 kafka-console-consumer --bootstrap-server localhost:9092 --topic testMessage --from-beginning
```
keep executing the curl command above to watch more messages arrive

#### 3.1 create cassandra keyspace and table where spark will write the data
```
docker exec -it dse_007 cqlsh -e "CREATE KEYSPACE IF NOT EXISTS customerkeyspace WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1}; \
CREATE TABLE IF NOT EXISTS customerkeyspace.messages ( \
    message_insert_time text, \
    message_date_time text, \
    message_id text, \
    message_type text, \
    message_value text, \
    PRIMARY KEY(message_insert_time, message_date_time) \
);"
``` 
#### 3.2 execute the spark job to pick up messages from kafka, analyze and write them to cassandra
```
mvn -f ./spark/processexl/pom.xml clean package
docker cp ./spark/processxl/src/main/resources/spark.properties dse_007:/opt/dse/
docker cp ./spark/processxl/target/processxl-1.0-SNAPSHOT-jar-with-dependencies.jar dse_007:/tmp/processexcel-1.0-SNAPSHOT.jar

### For test, this spark job will count sum from 1 to 100 
docker exec -it dse_007 dse spark-submit --class org.anant.DemoNumbersSum --master dse://172.20.10.9 /tmp/processexcel-1.0-SNAPSHOT.jar

### THIS IS AN ALTERNATIVE TO THE NEXT STEP 
mvn -f ./spark/processxl/pom.xml exec:java -Dexec.mainClass="org.anant.DemoKafkaConsumer"

### NOT WORKING YET This spark job will consume kafka messages and write them to cassandra 
docker exec -it dse_007 dse spark-submit --class org.anant.DemoKafkaConsumer --master dse://172.20.10.9 /tmp/processexcel-1.0-SNAPSHOT.jar spark.properties
```

Open spark-ui in a browser to check jobs status at `http://127.0.0.1:4040/jobs/` or `http://127.0.0.1:7080` - spark master

#### 3.3 trigger more messages
While spark streaming job is running trigger more kafka messages to be created with the same curl command 
```
curl -i http://127.0.0.1:5000/xls
```

#### 3.4 check cassandra new records
```
docker exec -it dse_007 cqlsh -e "SELECT * FROM customerkeyspace.messages;"
```
Result should looks similar to the next block
```
 message_insert_time      | message_date_time | message_id   | message_type | message_value
--------------------------+-------------------+--------------+--------------+---------------
 2020-04-25 20:31:06-0400 |        1578416560 | PAC-34f572ae |         Test |           100
 2020-04-25 20:18:57-0400 |        1578416560 | PAC-34f572ae |         Test |           100
 2020-04-25 20:07:59-0400 |        1578416560 | PAC-34f572ae |         Test |           100
 2020-04-25 20:19:15-0400 |        1578416560 | PAC-34f572ae |         Test |           100
 2020-04-25 20:18:05-0400 |        1578416560 | PAC-34f572ae |         Test |           100

(5 rows)
```

## Flow 2
<img src="https://github.com/xingh/DART.POC/blob/master/realtime-data-platform-examples-reviewed/diagrams/flow2.png"
 alt="flow2" style="float: left; margin-right: 10px;" />
