The Dockerfile : creates the image to run the python code that publishes the message kafka using the excel. Three files ExcelKafkaConfig.ini, ExcelMsgPublishToKafka.py, KafkaTestMessages.xlsx is copied to /usr/src/app/. The requirement.txt is used as part of the docker file creation

docker-compose file is to create kafka and cassandra dockers. 

run.sh  creates the topic in kafka docker. 

sparkCode: spark code consumes the data from kafka topic and push the data to cassandra table.

Pending: to connect all the dockers and run e2e
