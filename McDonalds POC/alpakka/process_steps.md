1) cd /root/alpakka
2) Start kafka and zookeeper
   docker-compose -f Docker-compose_kafka.yml up -d
3) strat csvsamples
   sbt doc-examples/run
4) select option 1 to start FetchHttpEvery30SecondsAndConvertCsvToJsonToKafka process
5) login to kafka container
   docker exec -it kafka_1 bash
6) list topics created by above process
   kafka-topics --zookeeper zookeeper:2181 --list
   output:
     __confluent.support.metrics
     topic1
7)describe topics
   kafka-console-consumer --bootstrap-server zookeeper:2181 --topic topic1
   //remarks facing issue at this point.Expected topic1 content is displayed. However nothing is displayed. 
