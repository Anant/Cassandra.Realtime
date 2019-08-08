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

curl -s -X GET http://$CONNECT_HOST:8083/connectors/akka-cassandra-sink/status

docker exec -i cassandra cqlsh -e 'select * from akka.salesjan2009 limit 10'

