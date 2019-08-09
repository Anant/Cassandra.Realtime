# Start spark streaming process #
	Refer steps 7 in devops_README 

# Producer #

##	Start producer	##
	cd "/root/dartpoc/Realtime Data Platform Examples/spark/spark"
	bin/run-example streaming.KafkaWordCountProducer localhost:9092 topic1 10 10

## Check data is ingested in to kafka ##

	docker run --net=host --rm confluentinc/cp-kafka:latest kafka-console-consumer --bootstrap-server localhost:9092 --topic topic1 --from-beginning --max-messages 10

# Consumer #
## Start consumer ##
	cd "/root/dartpoc/Realtime Data Platform Examples/spark/spark"
	bin/run-example streaming.KafkaWordCount localhost:2181 my-consumer-group topic1 1

# Check data persistence
	"/root/dartpoc/Realtime Data Platform Examples/spark/spark_check_persistence.sh"
