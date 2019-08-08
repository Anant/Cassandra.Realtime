curl -X POST \
     -H "Content-Type: application/json" \
     --data '{"name": "quickstart-cassandra-sink", 
	"config": {"connector.class":"com.datamountaineer.streamreactor.connect.cassandra.sink.CassandraSinkConnector", 
		"tasks.max":"1", 
		"topics":"orders", 
		"connect.cassandra.port":"9042",
		"connect.cassandra.contact.points":"142.93.126.190",
		"connect.cassandra.key.space":"csvFiles",
		"connect.cassandra.username":"cassandra",
		"connect.cassandra.password":"cassandra",
		"connect.cassandra.kcql":"INSERT INTO orders SELECT * FROM orders",
		"connect.progress.enabled":"true" 
			}
		}' \
     http://localhost:8083/connectors 
