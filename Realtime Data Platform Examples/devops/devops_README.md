1. # Move to working directory #
	cd "/root/dartpoc/Realtime Data Platform Examples/"

2. # Install SBT
	Refer below link for instalation

		http://www.codebind.com/linux-tutorials/install-scala-sbt-java-ubuntu-18-04-lts-linux/
	
3. # Start RabbitMQ #
	cd "/root/dartpoc/Realtime Data Platform Examples/devops"

	docker-compose -f Docker-compose_kafka.yml down

	docker-compose -f Docker-compose_kafka_akka.yml down

	docker-compose -f Docker-compose_RabitMQ.yml up -d

4. # Start Kafka #
	cd "/root/dartpoc/Realtime Data Platform Examples/devops"

	docker-compose -f Docker-compose_kafka_akka.yml down

	docker-compose -f Docker-compose_RabitMQ.yml down

	docker-compose -f Docker-compose_kafka.yml up -d
	

5. # Start kafka stream for Akka #
	## Kafka setup ##
	cd "/root/dartpoc/Realtime Data Platform Examples/"
	
	## Download akka git example repository ##

	git clone https://github.com/akka/alpakka.git

	cd alpakka

	git checkout v1.0.0

	docker-compose -f Docker-compose_RabitMQ.yml down

	docker-compose -f Docker-compose_kafka.yml down

	docker-compose -f Docker-compose_kafka_akka.yml up -d

	docker exec kafka-connect-avro mkdir /etc/kafka-connect/jars/sink

	docker cp  /root/dartpoc/kafka-connect-cassandra-1.1.0-1.1.0-all.jar kafka-connect-avro:/etc/kafka-connect/jars/sink

	docker restart kafka-connect-avro
	## Cassandra setup
	docker exec -it cassandra bash

	cqlsh

	CREATE KEYSPACE akka
    WITH replication = {'class':'SimpleStrategy', 'replication_factor' : 1};

	use akka;

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

6. # Start DSE #
	## Install DSE 6.0 or above ##
	Refer below link for installation

		https://docs.datastax.com/en/install/6.0/install/installDEBdse.html
	Note: Instalation succeeded for version 6.0.0-1 mentioned in example. However it failed for Version (6.0.9). 
	## Stop Cassandra ##
		dse cassandra-stop
	## Start Cassandra with analytical mode ##
		dse cassandra -R -k
	## Check DSE status ##
		nodetool status
	## Check Cassandra status ##
		cqlsh
	Should be able connect to cassandra
