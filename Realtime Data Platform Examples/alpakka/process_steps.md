1) cd /root/alpakka
2) Start kafka and zookeeper
   docker-compose -f Docker-compose_kafka.yml up -d
3) edit "FetchHttpEvery30SecondsAndConvertCsvToJsonToKafka.scala" file under "doc-examples/src/main/scala/csvsamples"
   replace "https://www.nasdaq.com/screening/companies-by-name.aspx?exchange=NASDAQ&render=download" url with 
           "http://samplecsvs.s3.amazonaws.com/SalesJan2009.csv?render=download"
4) strat csvsamples
   sbt doc-examples/run
5) select option 1 to start FetchHttpEvery30SecondsAndConvertCsvToJsonToKafka process
6) list topics created by above process
   docker run --net=host --rm confluentinc/cp-kafka:latest kafka-topics --list --zookeeper localhost:2181
   output:
     __confluent.support.metrics
     topics
7) consume topics
   docker run --net=host --rm confluentinc/cp-kafka:latest kafka-console-consumer --bootstrap-server localhost:9092 --topic topics --from-beginning --max-messages 10

   output
   should display 10 messages

8) create sink directory in kafka connector container
   docker exec kafka-connect-avro mkdir /etc/kafka-connect/jars/sink

9) copy kafka-connect-cassandra-1.1.0-1.1.0-all.jar to "kafka connector" container
   docker cp  /root/dartpoc/kafka-connect-cassandra-1.1.0-1.1.0-all.jar kafka-connect-avro:/etc/kafka-connect/jars/sink

10) Connect to cassandra container
   docker exec -it cassandra bash

11) login to cql
    cqlsh

12) create keyspace
    CREATE KEYSPACE akka
    WITH replication = {'class':'SimpleStrategy', 'replication_factor' : 1};

    use akka

13) create table under newly created keyspace
    create table salesjan2009(
	Account_Created text,
	Payment_Type text,
	Name text,
	Last_Login text,
	Transaction_date text,
	Latitude text,
	Country text,
	Longitude text,
	Price text,
	City text,
	Product text,
	State text,
    primary key (Transaction_date) 
   );

14) Restart kafka connect container
    docker restart kafka-connect-avro

    check container status after 3 min. It should be running.

15) export local variable to host OS
    export CONNECT_HOST=localhost

16) Start kafka connector to read from topic and load data to cassandra
    curl -X POST -H "Content-Type: application/json" \
               --data '{"name": "akka-cassandra-sink", "config": {"connector.class":"com.datamountaineer.streamreactor.connect.cassandra.sink.CassandraSinkConnector", 
	 							  "tasks.max":"1", 
							  	  "topics":"topics", 
								  "connect.cassandra.port":"9042", 
								  "connect.cassandra.contact.points":"cassandra", 
								  "connect.cassandra.key.space":"akka", 
								  "connect.cassandra.username":"cassandra", 
								  "connect.cassandra.password":"cassandra",
								  "connect.cassandra.kcql": "INSERT INTO salesjan2009 SELECT * FROM topics" }}' \
          http://localhost:8083/connectors

    Output
    {"name":"akka-cassandra-sink","config":{"connector.class":"com.datamountaineer.streamreactor.connect.cassandra.sink.CassandraSinkConnector","tasks.max":"1","topics":"topics","connect.cassandra.port":"9042","connect.cassandra.      contact.points":"cassandra","connect.cassandra.key.space":"akka","connect.cassandra.username":"cassandra","connect.cassandra.password":"cassandra",
      "value.converter":"org.apache.kafka.connect.storage.StringConverter","connect.cassandra.kcql":"INSERT INTO salesjan2009 SELECT * FROM topics",
      "name":"akka-cassandra-sink"},"tasks":[{"connector":"akka-cassandra-sink","task":0}],"type":"sink"}

17) Check connector is created an its status
    curl -s -X GET http://$CONNECT_HOST:8083/connectors/akka-cassandra-sink/status

    output
    {"name":"akka-cassandra-sink","connector":{"state":"RUNNING","worker_id":"kafka-connect-avro:8083"},"tasks":[{"id":0,"state":"RUNNING","worker_id":"kafka-connect-avro:8083"}],"type":"sink"}   

18) check data is loaded in cassandra

 
