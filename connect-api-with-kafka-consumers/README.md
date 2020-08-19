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
python3 ./kafka/create-schema.py http://172.20.10.14:8081 testMessage-avro ./kafka/test-message.avsc

### check that the schema exists
curl http://127.0.0.1:8081/subjects
```
or alternatively you can check AKHQ for all kafka resources getting created at `http://127.0.0.1:8085/` 

# Setup Astra

See instructions [here](https://github.com/Anant/cassandra.api/blob/master/README.md#setup), which apply for this project as well. 

- We are assuming keyspace of `demo` and table of `leaves`, as mentioned in these instructions, but if you want to use a different keyspace or table it should work fine. Just know that this is why we named the kafka topic (as specified in config.ini) "record.cassandra.leaves"

# Import the data

```
pip3 install -r requirement.txt
python3 data_importer.py
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
