# Start kafka to ingest csv data from file #
	Refer steps 5 in devops_README 

# Producer #
##	Start producer	##
	"/root/dartpoc/Realtime Data Platform Examples/kafka.connect/csv_producer.sh"

## Check "orders" topic is created in kafka ##
	./check_topic.sh

## Check connector status ##
	./csv_check_status.sh

## Check data is ingested into Kakfa
	./csv_check_data.sh

# Consumer #
## Start consumer ##
	"/root/dartpoc/Realtime Data Platform Examples/kafka.connect/csv_consumer.sh"

## Check data is ingested into Cassandra
	docker exec -i cassandra cqlsh -e 'select * from akka.salesjan2009 limit 10'