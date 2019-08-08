# Producer #
	cd "/root/dartpoc/Realtime Data Platform Examples/alpakka"

	sbt doc-examples/run

## Check data is ingested in to kafka ##

	docker run --net=host --rm confluentinc/cp-kafka:latest kafka-console-consumer --bootstrap-server localhost:9092 --topic topics --from-beginning --max-messages 10

# Consumer #
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

	
## Consumer command output
    {"name":"akka-cassandra-sink","config":{"connector.class":"com.datamountaineer.streamreactor.connect.cassandra.sink.CassandraSinkConnector","tasks.max":"1","topics":"topics","connect.cassandra.port":"9042","connect.cassandra.      contact.points":"cassandra","connect.cassandra.key.space":"akka","connect.cassandra.username":"cassandra","connect.cassandra.password":"cassandra",
      "value.converter":"org.apache.kafka.connect.storage.StringConverter","connect.cassandra.kcql":"INSERT INTO salesjan2009 SELECT * FROM topics",
      "name":"akka-cassandra-sink"},"tasks":[{"connector":"akka-cassandra-sink","task":0}],"type":"sink"}
## Check connector is created an its status ##
	curl -s -X GET http://$CONNECT_HOST:8083/connectors/akka-cassandra-sink/status
## output
	{"name":"akka-cassandra-sink","connector":{"state":"RUNNING","worker_id":"kafka-connect-avro:8083"},"tasks":[{"id":0,"state":"RUNNING","worker_id":"kafka-connect-avro:8083"}],"type":"sink"}
## Check data is ingested into Cassandra
	docker exec -i cassandra cqlsh -e 'select * from akka.salesjan2009 limit 10'