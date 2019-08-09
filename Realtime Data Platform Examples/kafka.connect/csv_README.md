# Start kafka to ingest csv data from file #
	Refer steps 7 in devops_README 

# Producer #
##	Start producer	##
	"/root/dartpoc/Realtime Data Platform Examples/kafka.connect/csv_producer.sh"

## Check "orders" topic is created in kafka ##
	"/root/dartpoc/Realtime Data Platform Examples/kafka.connect/check_topic.sh"

## Check source connector status ##
	"/root/dartpoc/Realtime Data Platform Examples/kafka.connect/csv_source_connector_status.sh"

## Check data is ingested into Kakfa
	"/root/dartpoc/Realtime Data Platform Examples/kafka.connect/csv_check_data.sh"

# Consumer #
## Start consumer ##
	"/root/dartpoc/Realtime Data Platform Examples/kafka.connect/csv_consumer.sh"

## Check sink connector status ##
	"/root/dartpoc/Realtime Data Platform Examples/kafka.connect/csv_sink_connector_status.sh"

## Check data is ingested into Cassandra
	"/root/dartpoc/Realtime Data Platform Examples/kafka.connect/csv_check_persistence.sh"