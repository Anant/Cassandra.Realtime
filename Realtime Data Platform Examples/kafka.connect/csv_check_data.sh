docker run --net=host --rm confluentinc/cp-kafka:latest kafka-console-consumer --bootstrap-server localhost:9092 --topic orders --from-beginning --max-messages 10
