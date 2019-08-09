# Start kafka stream for Akka #
	Refer steps 8 in devops_README 

# Producer #

##	Start producer	##
	"/root/dartpoc/Realtime Data Platform Examples/alpakka/akka_producer.sh"

	select option 1

## Check data is ingested in to kafka ##

	docker run --net=host --rm confluentinc/cp-kafka:latest kafka-console-consumer --bootstrap-server localhost:9092 --topic topics --from-beginning --max-messages 10

# Consumer #
## Start consumer ##
	"/root/dartpoc/Realtime Data Platform Examples/alpakka/akka_consumer.sh"

## Check data is ingested into Cassandra
	docker exec -i cassandra cqlsh -e 'select * from akka.salesjan2009 limit 10'