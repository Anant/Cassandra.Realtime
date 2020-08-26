# Open Cassandra API in Gitpod
https://gitpod.io/#https://github.com/Anant/cassandra.api

## Follow instructions in the Readme, including
- Install dependencies
- Setup Astra
- Upload your astra secure bundle into gitpod

You can use any api, but just make sure
## Run the API server
python astra.api/leaves.api.python/app.py

## Make the port public

## get url for future reference
gp url 8000

# Open Cassandra Realtime in Gitpod
We are going to use the gitpod branch (they provide a url as [explainet'sd here](https://www.gitpod.io/docs/context-urls/#branch-context))

	https://gitpod.io/#https://github.com/Anant/cassandra.realtime/tree/gitpod

## Setup Kafka
We setup some of the services for you, but some might have failed. You don't need to start kafka connect yet (and indeed, it won't work until we set it up later on in this demo), but the others should be up. 

You can check with the confluent cli:
```
confluent local services status
# if some are not up yet:
confluent local services start
```

## Create a topic
We are only going to create a single topic with a schema for this demo.

  ```
  $CONFLUENT_HOME/bin/kafka-topics --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic record-cassandra-leaves-avro
  ```

Check that the topic exists
  ```
  $CONFLUENT_HOME/bin/kafka-topics --list --zookeeper localhost:2181
  ```
```

#### Create the Kafka schema for topic's messages value

Make sure your python environment has `requests` module installed. You can install it using our requirements.txt file
```
cd /workspace/cassandra.realtime/connect-api-with-kafka-consumers
pip install -r python/requirement.txt
```

Now create the schema
```
python ./kafka/create-schema.py http://localhost:8081 record-cassandra-leaves ./kafka/leaves-record-schema.avsc
```

### check that the schema exists
```
curl http://127.0.0.1:8081/subjects
```
or alternatively you can check AKHQ for all kafka resources getting created at `http://127.0.0.1:8085/` 

# Import the data into Kafka

```
cd ./python
pip install -r requirement.txt
python3 data_importer.py --config-file-path configs/gitpod-config.ini
```

## check the message arrived in kafka topics

check topic that has schema using `kafka-avro-console-consumer`:
(WARNING: can potentially have lots of output)
```
$CONFLUENT_HOME/bin/kafka-avro-console-consumer --topic record-cassandra-leaves-avro --bootstrap-server localhost:9092 --from-beginning --property schema.registry.url=http://localhost:8081

# Consume from Kafka, write to Cassandra

#### Execute the scala job to pick up messages from Kafka, deserialize and write them to Cassandra
```
cd $PROJECT_HOME
mvn -f ./kafka-to-cassandra-worker/pom.xml clean package

# there should now be two jars in ./kafka-to-cassandra-worker/target, one with-dependencies, one without. We'll use the one with dependencies
mvn -f ./kafka-to-cassandra-worker/pom.xml exec:java -Dexec.mainClass="org.anant.KafkaAvroConsumer" -Dexec.args="kafka-to-cassandra-worker/target/classes/project.properties"
```

You can confirm we are consuming the correct topic using AKHQ, at `http://localhost:8085/ui/docker-kafka-server/topic`. 
- By default we are getting all messages every time by using offset of "earliest", but you can turn that off by setting "debug-mode" to false in your properties file.
- Send more messages whenever you want to by re-running the python script from the python dir:
    ```
    cd $PROJECT_HOME/python
    python data_importer.py --config-file-path configs/gitpod-config.ini
    ```


# Sending messages to Kafka using Kafka REST Proxy

Check your topics 
```
curl http://localhost:8082/topics/
curl http://localhost:8082/topics/record-cassandra-leaves-avro
```

Send using data importer's rest proxy mode:
```
python3 data_importer.py --config-file-path configs/gitpod-rest-proxy-config.ini
```

# Process messages using Kafka Streams and writing to Cassandra using Processor API
You can use the Kafka processor API if you want to send messages to Cassandra using the REST API we are using.

```
mvn exec:java -Dexec.mainClass="org.anant.KafkaStreamsAvroConsumer" -Dexec.args="target/classes/project.properties"
```

# Writing to Cassandra using Kafka Connect
We used the Processor API to show what it would look like to write to Cassandra using Kafka Streams and a REST API, but it is generally recommended to use Kafka Connect. We will be using the [Datastax connector](https://www.confluent.io/hub/datastax/kafka-connect-cassandra-sink) since it has a free license, but there is also a Confluence Cassandra connector as well as other third party connectors available if you are interested. 

## Setup Kafka Connect
The Datastax Kafka connector also has instructions and a download link from [the Datastax website](https://docs.datastax.com/en/kafka/doc/kafka/install/kafkaInstall.html) as well as [Confluent Hub](https://www.confluent.io/hub/datastax/kafka-connect-cassandra-sink).


See `kafka/connect` and `kafka/plugins`. These are copied into the kafka connect docker image for you already when you run `docker-compose up`. The confluent docker image we use provides a `kafka-connect.properties` (which are the worker properties) for us at `/etc/kafka-connect/kafka-connect.properties`, generated using the env vars we passed in using our docker-compose.yml (see [here](https://github.com/confluentinc/cp-docker-images/blob/2d1072207790a06b88b79fb129f72bb41b67532c/debian/kafka-connect-base/include/etc/confluent/docker/configure#L54) for where they do that).

- You can see the worker properties they provide as defaults by running: 
    ```
    docker exec kafka-connect cat /etc/kafka-connect/kafka-connect.properties
    ```

## Setup worker properties
We already made a `connect-standalone.properties.example` that is setup to run `kafka-connect-cassandra-sink-1.4.0.jar`. However, you will need to change:
  1) the name of the astra credentials zip file, (cloud.secureConnectBundle). The path should be fine.
  2) Topic settings, particularly keyspace and tablename, unless tablename is already leaves, then only change keyspace (topic.record-cassandra-leaves-avro.<my_ks>.leaves.mapping)
  3) Astra db's password and username (auth.password)

Fields that require changing are marked by `### TODO make sure to change!` in the example file.

```
cp kafka/connect/connect-standalone.properties.example kafka/connect/connect-standalone.properties
vim kafka/connect/connect-standalone.properties
# ...
```

## Setup Connect with Astra
- If you have not already, make sure that your Datastax astra secure connect bundle is downloaded 
- Place the secure creds bundle into astra.credentials

  ```
  mv ./path/to/astra.credentials/secure-connect-<database-name-in-astra>.zip ./kafka/connect/astra.credentials/
  ```

- set system env vars to specify where your bundle is and what its name is
  ```
  # this is filename
  export ASTRA_SECURE_BUNDLE_NAME="secure-connect-<db-name>.zip"
  ```

## Rebuild and Restart Kafka Connect

Before this, the kafka connect job tried to start but likely crashed since it needed your `connect-standalone.properties` file. Rebuild it, which will copy your `connect-standalone.properties` file over, then restart the container. 

```
docker-compose -up -d --build
```

- args passed in are as follows: `connect-standalone <worker.properties> <connector.properties>`

# Using Kafka Connect
## Upload Astra secure bundle
Place it here in Gitpod: 
`$PROJECT_HOME/kafka/connect/astra.credentials/`
## Set your configs
```
cp $PROJECT_HOME/kafka/connect/connect-standalone.properties.gitpod-example $PROJECT_HOME/kafka/connect/connect-standalone.properties
vim $PROJECT_HOME/kafka/connect/connect-standalone.properties
# ...
```

## Run Kafka Connect
${CONFLUENT_HOME}/bin/connect-standalone $PROJECT_HOME/kafka/connect/connect-standalone.properties
