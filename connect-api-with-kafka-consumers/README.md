# 1 Setup the Api
#### 1.1 Setup Cassandra Astra and Cassandra API
Following directions from [cassandra.api](https://github.com/Anant/cassandra.api). You can just clone it and run it.

- However, be sure to clear your C* table at the end (you can use cqlsh in astra GUI if you want to) since we are going to see how to get records in from Kafka.

```
cqlsh > TRUNCATE <keyspace>.<tablename> ;
```

- Make sure the server is running at the end of it, and put the api port into your `kafka-to-cassandra-worker/src/main/resources/project.properties` (unless it is already at localhost:8000)


#### 1.4 Build docker image and run all docker containers
```
docker-compose up
```
- cp-kafka
- cp-zookeeper
- schema-registry
- akhq (former kafka-hq)
- python data importer (imports into kafka)

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

#### 2.3 create the Kafka schema for topic's messages value
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
mvn -f ./kafka-to-cassandra-worker/pom.xml clean package

# there should now be two jars in ./kafka-to-cassandra-worker/target, one with-dependencies, one without. We'll use the one with dependencies
mvn -f ./kafka-to-cassandra-worker/pom.xml exec:java -Dexec.mainClass="org.anant.DemoKafkaConsumer" -Dexec.args="kafka-to-cassandra-worker/target/classes/project.properties"
```
You can confirm we are consuming the correct topic using AKHQ, at `http://localhost:8085/ui/docker-kafka-server/topic`. 
- By default we are getting all messages every time by using offset of "earliest", but you can turn that off by setting "debug-mode" to false in your properties file.
- Send more messages whenever you want to by re-running the python script from the python dir:
    ```
    python3 data_importer.py
    ```


# Kafka REST Proxy

Check your topics 
```
curl http://172.20.10.20:8082/topics/
curl http://172.20.10.20:8082/topics/record-cassandra-leaves-avro
```
