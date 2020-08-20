#### 1.1 Build docker image and run all docker containers
```
docker-compose up
```
- cp-kafka
- cp-zookeeper
- schema-registry
- akhq (former kafka-hq)
- python-flask-app-for-kafka

#### 2.1 create kafka topics
Assuming we have two topics, one with schema (`record-cassandra-leaves-avro`) and one without schema (`record.cassandra.leaves`):
```
docker exec -it cp_kafka_007 kafka-topics --create --zookeeper 172.20.10.11:2181 --replication-factor 1 --partitions 1 --topic record-cassandra-leaves
```
```
docker exec -it cp_kafka_007 kafka-topics --create --zookeeper 172.20.10.11:2181 --replication-factor 1 --partitions 1 --topic record-cassandra-leaves-avro
```

#### 2.2 check that both topics exist
```
docker exec -it cp_kafka_007 kafka-topics --list --zookeeper 172.20.10.11:2181
```

#### 2.3 create the schema for topic's messages value
make sure your python environment has `requests` module installed
```
python3 ./kafka/create-schema.py http://172.20.10.14:8081 record-cassandra-leaves ./kafka/leaves-record-schema.avsc

- If you are using records different from ours, you can generate fields for your own schema by taking one record (in json format) and using a tool such as [this one at toolslick.com](https://toolslick.com/generation/metadata/avro-schema-from-json) that takes a json record and creates a sample schema. 
		* For example, take json in a format similar to what we have in `python/assets/sample-record.json`, and copy it into the toolslick tool. Then just change the name, type and namespace fields. 
		* Also make sure the example record you use has all the fields in your db, or else if one record has a field that you don't include, kafka will throw an error for that as well
		* If you have any timestamps, make sure to change logical_type of those in the schema from "date" to whatever you need, and make `type` `long` instead of `int`. Our app is currently configured only for timestamp-milliseconds. [See here](https://avro.apache.org/docs/1.8.0/spec.html#Timestamp+%28millisecond+precision%29) for avro docs on this issue.
		* Also make sure to allow for any null values in your fields in your avro schema, if you have any null values (e.g., see [here](https://stackoverflow.com/a/45662702/6952495))
		* Alternately, you can just write out your own schema manually.
- If there's an HTTP error while creating the schema (e.g., a 422), you can check the schema registry logs:
    ```
    docker logs -f schema-registry
    ```

### check that the schema exists
curl http://127.0.0.1:8081/subjects
```
or alternatively you can check AKHQ for all kafka resources getting created at `http://127.0.0.1:8085/` 

# Setup Astra

See instructions [here](https://github.com/Anant/cassandra.api/blob/master/README.md#setup), which apply for this project as well. 

- We are assuming keyspace of `demo` and table of `leaves`, as mentioned in these instructions, but if you want to use a different keyspace or table it should work fine. Just know that this is why we named the kafka topic (as specified in config.ini) "record-cassandra-leaves"

# Import the data into Kafka

```
pip3 install -r requirement.txt
python3 data_importer.py
```
## check the message arrived in kafka topics
check schema-less topic
```
docker exec -it cp_kafka_007 kafka-console-consumer --bootstrap-server localhost:9092 --topic record-cassandra-leaves --from-beginning
```

check topic that has schema:
```
docker exec -it kafka-connect kafka-avro-console-consumer --topic record-cassandra-leaves-avro --bootstrap-server 172.20.10.12:9092 --from-beginning --property schema.registry.url=http://172.20.10.14:8081



# Consume from Kafka, write to Cassandra

#### 3.2 execute the scala job to pick up messages from Kafka, deserialize and write them to Cassandra
```
mvn -f ./kafka/kafka-to-cassandra/pom.xml clean package
docker cp ./spark/processexcel/src/main/resources/spark.properties dse_007:/opt/dse/
docker cp ./spark/processexcel/target/processexcel-1.0-SNAPSHOT-jar-with-dependencies.jar dse_007:/tmp/processexcel-1.0-SNAPSHOT.jar








### You can of course run the spark job in standalone mode - but this is NOT FUN 
mvn -f ./spark/processexcel/pom.xml exec:java -Dexec.mainClass="org.anant.DemoKafkaConsumer"

### This is where the FUN part is, a spark job running on dse cluster will consume kafka messages and write them to cassandra 
docker exec -it dse_007 dse spark-submit --class org.anant.DemoKafkaConsumer --master dse://172.20.10.9 /tmp/processexcel-1.0-SNAPSHOT.jar spark.properties
```

Optionally open spark-ui in a browser to check jobs status at `http://127.0.0.1:4040/jobs/` or `http://127.0.0.1:7080` - spark master














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
