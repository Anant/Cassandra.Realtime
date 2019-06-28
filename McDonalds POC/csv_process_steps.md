I   -> Generate jar's in Ubuntu
	   i) Install open JDK8
			apt-get update 
			apt-get install -y openjdk-8-jdk
	  ii) Install mvn
			apt-get install -y maven
	 iii) Install git 
			apt-get install -y git
	  iv) Download kafka-connect-spooldir from git
			git clone https://github.com/jcustenborder/kafka-connect-spooldir.git
	   v) cd kafka-connect-spooldir
	  vi) kafka-connect-spooldir
II  -> Start docker-compose
	   i) docker-compose up -d
III -> Copy csv's jar's to kafka-connect container, folder /etc/kafka-connect/jars
		  i) create tmp/quickstart/jars folder on local drive
		 ii) cd tmp/quickstart/jars
		iii) copy jar files (created in above step I) from ~/target/kafka-connect-target/usr/share/kafka-connect/kafka-connect-spooldir/
		    cp ~/target/kafka-connect-target/usr/share/kafka-connect/kafka-connect-spooldir/animal-sniffer-annotations-1.17.jar .
			use above copy command to copy all jars
		 iv) copy jars to kafka connect container
		     docker cp animal-sniffer-annotations-1.17.jar kafka-connect-avro:/etc/kafka-connect/jars
			 use above command to copy all jars
IV  -> Copy cassandra sink connector to kafka-connect container, folder /etc/kafka-connect/jars
		  i) connect to kafka connect container
		     docker exec -it kafka-connect-avro bash
		 ii) change to jars directory
			 cd /etc/kafka-connect/jars
		iii) create sink directory under /etc/kafka-connect/jars
		     mkdir sink
         iv) exit from container
		     exit
		  v) docker cp kafka-connect-cassandra-1.1.0-1.1.0-all.jar kafka-connect-avro:/etc/kafka-connect/jars/sink
V   ->  Restart kafka connect container
	   i) docker restart kafka-connect-avro
VI  ->  Wait for 3 min and check container status
	   i) docker ps -a
		  
		Kafka connect container status should be "UP" and running.
VII -> Export local host variables
	   i) export CONNECT_HOST="192.168.99.100"
VIII -> create connector to load csv files
       i)   curl -i -X POST \
				-H "Accept:application/json" \
				-H  "Content-Type:application/json" http://$CONNECT_HOST:8083/connectors/ \
				-d '{"name": "csv-source-orders",
					 "config": {"tasks.max": "1",
								"connector.class": "com.github.jcustenborder.kafka.connect.spooldir.SpoolDirCsvSourceConnector",
								"input.file.pattern": "^orders.*.csv$",
								"input.path": "/tmp/source",
								"finished.path": "/tmp/finished",
								"error.path": "/tmp/error",
								"halt.on.error": "false",
								"topic": "orders",
								"value.schema":"{\"name\":\"com.github.jcustenborder.kafka.connect.model.Value\",\"type\":\"STRUCT\",\"isOptional\":false,\"fieldSchemas\":{\"order_id\":{\"type\":\"INT64\",\"isOptional\":false},\"customer_id\":{\"type\":\"INT64\",\"isOptional\":false},\"order_ts\":{\"type\":\"STRING\",\"isOptional\":false},\"product\":{\"type\":\"STRING\",\"isOptional\":false},\"order_total_usd\":{\"type\":\"STRING\",\"isOptional\":false}}}",
								"key.schema":"{\"name\":\"com.github.jcustenborder.kafka.connect.model.Key\",\"type\":\"STRUCT\",\"isOptional\":false,\"fieldSchemas\":{\"order_id\":{\"type\":\"INT64\",\"isOptional\":false}}}",
								"csv.first.row.as.header": "true"
								}
					}'
IX  -> 	check connector state 
	  i) curl -s -X GET http://$CONNECT_HOST:8083/connectors/csv-source-orders/status
	   
	   connector state should be "RUNNING" 
X   -> Check "orders" topic is created in kafka
	  i) docker run --net=host --rm confluentinc/cp-kafka:latest kafka-topics --list --zookeeper localhost:2181
	   
	   "orders" topic will be created after "orders.csv" file is copied to /tmp/source
XI  -> copy "orders.csv" file is copied to /tmp/source
	  i) docker cp orders.csv kafka-connect-avro:/tmp/source
XII -> Check "orders" topic is created in kafka
	  i) docker run --net=host --rm confluentinc/cp-kafka:latest kafka-topics --list --zookeeper localhost:2181
	   
	   should display "orders" topic
XIII-> Check "orders" topic details
	  i) docker run --net=host --rm confluentinc/cp-kafka:latest kafka-topics --describe --zookeeper localhost:2181|grep orders
XIV -> create keyspace and table in cassandra container
	  i) docker exec -it cassandra bash
	 ii) cqlsh
	iii) CREATE KEYSPACE csvFiles WITH replication = {'class':'SimpleStrategy', 'replication_factor' : 1};
	 iv) use csvFiles;
	  v) create table orders(order_id int, customer_id int, order_ts text, product text, order_total_usd int, primary key (order_id,customer_id));
XV  -> exist from cqlsh and cassandra container
XVI -> Create cassandra sink connector to consume "orders" topic from kafka
      i) curl -X POST \
				-H "Content-Type: application/json" \
				--data '{"name": "quickstart-cassandra-sink", 
						 "config": {"connector.class":"com.datamountaineer.streamreactor.connect.cassandra.sink.CassandraSinkConnector", 
									"tasks.max":"1", 
									"topics":"orders", 
									"connect.cassandra.port":"9042",
									"connect.cassandra.contact.points":"192.168.99.100",
									"connect.cassandra.key.space":"fromFiles",
									"connect.cassandra.username":"cassandra",
									"connect.cassandra.password":"cassandra",
									"connect.cassandra.kcql":"INSERT INTO orders SELECT * FROM orders",
									"connect.progress.enabled":"true" 
									}
						}' \
				http://$CONNECT_HOST:8083/connectors
XVII-> Check data is available in cassandra
	  i) docker exec -it cassandra bash
	 ii) cqlsh
	iii) use csvFiles;
	 iv) select count(*) from orders ;
	 count should match number of lines in csv file
XV  -> exist from cqlsh and cassandra container