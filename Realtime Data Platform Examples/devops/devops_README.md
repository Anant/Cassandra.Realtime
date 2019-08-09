1. # Move to working directory #
		cd "/root/dartpoc/Realtime Data Platform Examples/"

2. # Install open JDK8 #
		apt-get update
		apt-get install -y openjdk-8-jdk

3. # Install Maven #
		apt-get install -y maven

4. # Install Git #
		apt-get install -y git

3. # Install SBT
	Refer http://www.codebind.com/linux-tutorials/install-scala-sbt-java-ubuntu-18-04-lts-linux/ link for instalation

4. # Start RabbitMQ #
		cd "/root/dartpoc/Realtime Data Platform Examples/devops"
		docker-compose -f Docker-compose_kafka.yml down
		docker-compose -f Docker-compose_kafka_akka.yml down
		docker-compose -f Docker-compose_RabitMQ.yml up -d

5. # Start kafka to ingest csv data from file #
		cd "/root/dartpoc/Realtime Data Platform Examples/kafka.connect"
		git clone https://github.com/jcustenborder/kafka-connect-spooldir.git
		cd kafka-connect-spooldir
		mvn clean package
	## Start kafka and Cassandra ##
		cd "/root/dartpoc/Realtime Data Platform Examples/devops"
		docker-compose -f Docker-compose_kafka_akka.yml down
		docker-compose -f Docker-compose_RabitMQ.yml down
		docker-compose -f Docker-compose_kafka.yml up -d
	## Copy csv jar's to kafka-connect container, folder /etc/kafka-connect/jars ##
	### create temporary jar folder on local drive and copy jar files created in setp I ###
		mkdir "/root/dartpoc/Realtime Data Platform Examples/devops/tmp/quickstart/jars"
		cp "/root/dartpoc/Realtime Data Platform Examples/kafka.connect/kafka-connect-spooldir/target/kafka-connect-target/usr/share/kafka-connect/kafka-connect-spooldir/"*.jar tmp/quickstart/jars/
	## Copy all jars to kafka connect container ##
		./copy_csv_jars.sh
	
	## Copy cassandra sink connector to kafka-connect container, folder /etc/kafka-connect/jars ##
		docker exec -i kafka-connect-avro mkdir /etc/kafka-connect/jars/sink /tmp/source /tmp/error /tmp/finished
		docker cp "/root/dartpoc/kafka-connect-cassandra-1.1.0-1.1.0-all.jar" kafka-connect-avro:/etc/kafka-connect/jars/sink
		docker restart kafka-connect-avro

	## Copy csv file is copied to /tmp/source ##
		docker cp "/root/dartpoc/Realtime Data Platform Examples/kafka.connect/data/orders.csv" kafka-connect-avro:/tmp/source

	## Cassandra setup ##
		docker cp csvCreateScript.cql cassandra:/
		docker exec -i cassandra cqlsh -f csvCreateScript.cql

6. # Start kafka stream for Akka #
	## Kafka setup ##
		cd "/root/dartpoc/Realtime Data Platform Examples/alpakka"
		git clone https://github.com/akka/alpakka.git
		cd alpakka
		git checkout v1.0.0

	## Edit FetchHttpEvery30SecondsAndConvertCsvToJsonToKafka.scala file under doc-examples/src/main/scala/csvsamples ##
		replace "https://www.nasdaq.com/screening/companies-by-name.aspx?exchange=NASDAQ&render=download" url with
           "http://samplecsvs.s3.amazonaws.com/SalesJan2009.csv?render=download"

		comment kafka port line no 54
		comment embedded kafka line no 55
		comment stoping of embedded kafka line no 100
	## ##
		docker-compose -f Docker-compose_RabitMQ.yml down
		docker-compose -f Docker-compose_kafka.yml down
		docker-compose -f Docker-compose_kafka_akka.yml up -d
		docker exec kafka-connect-avro mkdir /etc/kafka-connect/jars/sink
		docker cp  /root/dartpoc/kafka-connect-cassandra-1.1.0-1.1.0-all.jar kafka-connect-avro:/etc/kafka-connect/jars/sink
		docker restart kafka-connect-avro
	## Cassandra setup ##
	docker cp createScript.cql cassandra:/
	
	docker exec -i cassandra cqlsh -f createScript.cql

7. # Start spark streaming #
	## Start DSE ##
		### DSE 6.0 or above installation ###
			Refer below link for installation

			https://docs.datastax.com/en/install/6.0/install/installDEBdse.html
			Note: Instalation succeeded for version 6.0.0-1 mentioned in example. However it failed for Version (6.0.9). 
		### Stop Cassandra ###
			dse cassandra-stop
		### Start Cassandra with analytical mode ###
			dse cassandra -R -k
		### Check DSE status ###
			nodetool status
		### Check connectivity to Cassandra ###
			cqlsh
			Should be able connect to cassandra
	## Build spark streaming package
		cd "/root/dartpoc/Realtime Data Platform Examples/spark"
		git clone https://github.com/apache/spark.git
		cd spark
		git checkout v2.2.0
		
		edit Pom.xml under example folder to include dependencies. 
		edit KafkaWordCount.scala and DirectKafkaWordCount.scala code to use "," as seperator

		Use README.md to complete build 
		(build/mvn -DskipTests clean package)