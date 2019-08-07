1. # Generate jar's in Ubuntu #
	## Install open JDK8 ##
	### apt-get update ###
	### apt-get install -y openjdk-8-jdk ###
	## Install mvn ##
	### apt-get install -y maven ###
	## Install git ##
	### apt-get install -y git ###
	### cd "/root/dartpoc/McDonalds POC"
	## Download kafka-connect-spooldir from git ##
	### git clone https://github.com/jcustenborder/kafka-connect-spooldir.git ###
	### cd kafka-connect-spooldir ###
	### mvn clean package ###
	
2. # Start kafka and Cassandra #
	## cd "/root/dartpoc/McDonalds POC" ##
	## docker-compose up -d

3. # Copy csv jar's to kafka-connect container, folder /etc/kafka-connect/jars #
	## create temporary jar folder on local drive and copy jar files created in setp I ##
		mkdir tmp/quickstart/jars
		cd tmp/quickstart/jars
		cp "/root/dartpoc/McDonalds POC/kafka-connect-spooldir/target/kafka-connect-target/usr/share/kafka-connect/kafka-connect-spooldir/*.jar" .
	## Copy all jars to kafka connect container ##
		docker cp animal-sniffer-annotations-1.17.jar kafka-connect-avro:/etc/kafka-connect/jars
		docker cp checker-qual-2.5.2.jar kafka-connect-avro:/etc/kafka-connect/jars
		docker cp commons-beanutils-1.9.3.jar kafka-connect-avro:/etc/kafka-connect/jars
		docker cp commons-collections-3.2.2.jar kafka-connect-avro:/etc/kafka-connect/jars
		docker cp commons-collections4-4.2.jar kafka-connect-avro:/etc/kafka-connect/jars
		docker cp commons-compress-1.18.jar kafka-connect-avro:/etc/kafka-connect/jars
		docker cp commons-lang3-3.8.1.jar kafka-connect-avro:/etc/kafka-connect/jars
		docker cp commons-logging-1.2.jar kafka-connect-avro:/etc/kafka-connect/jars
		docker cp commons-text-1.3.jar kafka-connect-avro:/etc/kafka-connect/jars
		docker cp connect-utils-0.4.155.jar kafka-connect-avro:/etc/kafka-connect/jars
		docker cp error_prone_annotations-2.2.0.jar kafka-connect-avro:/etc/kafka-connect/jars
		docker cp extended-log-format-0.0.1.5.jar kafka-connect-avro:/etc/kafka-connect/jars
		docker cp failureaccess-1.0.1.jar kafka-connect-avro:/etc/kafka-connect/jars
		docker cp freemarker-2.3.28.jar kafka-connect-avro:/etc/kafka-connect/jars
		docker cp guava-27.1-jre.jar kafka-connect-avro:/etc/kafka-connect/jars
		docker cp j2objc-annotations-1.1.jar kafka-connect-avro:/etc/kafka-connect/jars
		docker cp jackson-annotations-2.9.0.jar kafka-connect-avro:/etc/kafka-connect/jars
		docker cp jackson-core-2.9.9.jar kafka-connect-avro:/etc/kafka-connect/jars
		docker cp jackson-databind-2.9.9.jar kafka-connect-avro:/etc/kafka-connect/jars
		docker cp javassist-3.21.0-GA.jar kafka-connect-avro:/etc/kafka-connect/jars
		docker cp jsr305-3.0.2.jar kafka-connect-avro:/etc/kafka-connect/jars
		docker cp kafka-connect-spooldir-1.0-SNAPSHOT.jar kafka-connect-avro:/etc/kafka-connect/jars
		docker cp listenablefuture-9999.0-empty-to-avoid-conflict-with-guava.jar kafka-connect-avro:/etc/kafka-connect/jars
		docker cp opencsv-4.5.jar kafka-connect-avro:/etc/kafka-connect/jars
		docker cp reflections-0.9.11.jar kafka-connect-avro:/etc/kafka-connect/jars
		docker cp value-2.5.5.jar kafka-connect-avro:/etc/kafka-connect/jars
3. # Copy cassandra sink connector to kafka-connect container, folder /etc/kafka-connect/jars #
	## Connect to kafka connect container ##
		docker exec -it kafka-connect-avro bash
	## Change to jars directory ##
		cd /etc/kafka-connect/jars
	## Create sink directory under /etc/kafka-connect/jars
		mkdir sink
	## Create source, error, finished directories
		mkdir /tmp/source /tmp/error /tmp/finished
	## Come out of container
		exit
	## Copy jar
		docker cp kafka-connect-cassandra-1.1.0-1.1.0-all.jar kafka-connect-avro:/etc/kafka-connect/jars/sink
4. # Restart kafka connect container #
	docker restart kafka-connect-avro
	
	Wait for 3 min and check container status

	docker ps -a

	kafka-connect-avro container status should be "UP" and running.

5. # Export local host variables #
	export CONNECT_HOST="142.93.126.190"

6. # Create connector to load csv files #
	curl -i -X POST \
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
6. # Check connector status #
	curl -s -X GET http://$CONNECT_HOST:8083/connectors/csv-source-orders/status

	connector state should be "RUNNING"

7. # Check "orders" topic is created in kafka #
	 docker run --net=host --rm confluentinc/cp-kafka:latest kafka-topics --list --zookeeper localhost:2181

	"orders" topic will be created after "orders.csv" file is copied to /tmp/source

8. # Copy "orders.csv" file is copied to /tmp/source #
	docker cp "/root/dartpoc/McDonalds POC/orders.csv" kafka-connect-avro:/tmp/source

9. # Check "orders" topic is created in kafka #
	 docker run --net=host --rm confluentinc/cp-kafka:latest kafka-topics --list --zookeeper localhost:2181

	Should display "orders" topic

10. # Describe "orders" topic #
	docker run --net=host --rm confluentinc/cp-kafka:latest kafka-topics --describe --zookeeper localhost:2181|grep orders

11. # Create keyspace and table in cassandra container #
	docker exec -it cassandra bash

	cqlsh

	CREATE KEYSPACE csvFiles WITH replication = {'class':'SimpleStrategy', 'replication_factor' : 1};

	use csvFiles;

	create table orders(order_id int, customer_id int, order_ts text, product text, order_total_usd int, primary key (order_id,customer_id));

	exit

	exit

12. # Create cassandra sink connector to consume "orders" topic from kafka #
	curl -X POST \
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

13. # Use Kafka consumer api to check the data #
	docker run  --net=host  --rm  confluentinc/cp-kafka:latest  kafka-console-consumer --bootstrap-server $CONNECT_HOST:9092 --topic orders --from-beginning --max-messages 10
14. # Check data is available in cassandra #
	docker exec -it cassandra bash

	cqlsh

	use csvFiles;

	select count(*) from orders ;

	Count should match number of lines in csv file

14. # exist from cqlsh and cassandra container #
	exit
	
	exit