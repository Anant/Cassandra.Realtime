# Reviewed version of realtime data platform

## Flow 1
<img src="https://github.com/xingh/DART.POC/blob/master/realtime-data-platform-examples-reviewed/diagrams/flow1.png"
 alt="flow1" width="800" style="float: left; margin-right: 10px;" />

1. a curl command will trigger a HTTP call to python flask app
2. flask app will read an Excel file and send the content to kafka
3. a spark job is submitted to dse to consume kafka messages and write them to cassandra 

#### Build docker image and run all docker containers
```
docker compose up
```
- cp-kafka
- cp-zookeeper
- kafka-manager
- python-flask-app-for-kafka

#### create kafka topic
```
docker exec -it cp_kafka_007 kafka-topics --create --zookeeper 172.20.10.11:2181 --replication-factor 1 --partitions 1 --topic testMessage
```

#### check the topic exists
```
docker exec -it cp_kafka_007 kafka-topics --list --zookeeper 172.20.10.11:2181
```

#### ask python flask app to send a message to kafka by reading the excel file
```
curl -i http://127.0.0.1:5000/xls
```

#### check the message arrived in kafka
```
docker exec -it cp_kafka_007 kafka-console-consumer --bootstrap-server localhost:9092 --topic testMessage --from-beginning
```
keep executing the curl command above to watch more messages arrive

## Flow 2
<img src="https://github.com/xingh/DART.POC/blob/master/realtime-data-platform-examples-reviewed/diagrams/flow2.png"
 alt="flow2" style="float: left; margin-right: 10px;" />
