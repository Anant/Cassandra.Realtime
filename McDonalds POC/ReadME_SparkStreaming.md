# Spark streaming and kafka integration #

1. Download code from https://github.com/apache/spark.git. Use v2.2.0 branch. 
	Pom.xml is modified to include dependencies. KafkaWordCount.scala and DirectKafkaWordCount.scala code is modified to use "," as seperator 
2. Use README.md for building
3. Start kafka and zookeeper 

There are two section. One for creating topic/sending message to kafka. Other one is for consumes topics. 
Start two terminals

4. Producer : Start producer program in one terminal
	- Syntax : KafkaWordCountProducer <metadataBrokerList> <topic> <messagesPerSec> <wordsPerMessage>
	- Command : bin/run-example streaming.KafkaWordCountProducer 142.93.126.190:9092 topic1,topc2 10 10 

5. Consumer : Start Consumer program in other terminal
	- Syntax : KafkaWordCount <zkQuorum> <group> <topics> <numThreads>
	- Command : bin/run-example streaming.KafkaWordCount 142.93.126.190:2181 my-consumer-group topic1 1

6. Consumer program can also be started using direct method
	- Syntax : DirectKafkaWordCount <brokers> <topics>
	- Command : bin/run-example streaming.DirectKafkaWordCount 142.93.126.190:9092 topic1


