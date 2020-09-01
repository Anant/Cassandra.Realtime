# Setup Cassandra API
First we will setup a REST API using NodeJS or Flask.

## Open Cassandra API in Gitpod
https://gitpod.io/#https://github.com/Anant/cassandra.api

## Follow instructions in the [README](https://github.com/Anant/cassandra.api/blob/master/README.md):

## Make the port public

## get url for future reference
gp url 8000

# Open Cassandra Realtime in Gitpod
We are going to use the gitpod branch (they provide a url as [explained here](https://www.gitpod.io/docs/context-urls/#branch-context))

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
cd $PROJECT_HOME
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
cd $PROJECT_HOME/python
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

First, edit the gitpod-project.properties file with the url of your running cassandra.api instance. 
- You will need to change the `api.host` key, to something like `https://8000-c0f5dade-a15f-4d23-b52b-468e334d6abb.ws-us02.gitpod.io`.
- Note that if you don't do this, the consumer will still run, but will just fail to write to Cassandra, since its current setting isn't stopping on errors.
```
vim $PROJECT_HOME/kafka-to-cassandra-worker/src/main/resources/gitpod-project.properties
#...
```

Now package and run the project:

```
cd $PROJECT_HOME
mvn -f ./kafka-to-cassandra-worker/pom.xml clean package

# there should now be two jars in ./kafka-to-cassandra-worker/target, one with-dependencies, one without. We'll use the one with dependencies
mvn -f ./kafka-to-cassandra-worker/pom.xml exec:java -Dexec.mainClass="org.anant.KafkaAvroConsumer" -Dexec.args="kafka-to-cassandra-worker/target/classes/gitpod-project.properties"
```

You can confirm we are consuming the correct topic using AKHQ, at `http://8085<gitpod-url-for-akhq>/ui/docker-kafka-server/topic`. 
- Offset is at `latest`, so you won't see anything unless you have messages actively coming in.
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
cd $PROJECT_HOME/python
python3 data_importer.py --config-file-path configs/gitpod-rest-proxy-config.ini
```

# Process messages using Kafka Streams and writing to Cassandra using Processor API
You can use the Kafka processor API if you want to send messages to Cassandra using the REST API we are using.

```
cd $PROJECT_HOME
mvn  -f ./kafka-to-cassandra-worker/pom.xml exec:java -Dexec.mainClass="org.anant.KafkaStreamsAvroConsumer" -Dexec.args="kafka-to-cassandra-worker/target/classes/gitpod-project.properties"
```

# Writing to Cassandra using Kafka Connect
We used the Processor API to show what it would look like to write to Cassandra using Kafka Streams and a REST API, but it is generally recommended to use Kafka Connect. We will be using the [Datastax connector](https://www.confluent.io/hub/datastax/kafka-connect-cassandra-sink) since it has a free license, but there is also a Confluence Cassandra connector as well as other third party connectors available if you are interested. 

## Setup Kafka Connect
The Datastax Kafka connector also has instructions and a download link from [the Datastax website](https://docs.datastax.com/en/kafka/doc/kafka/install/kafkaInstall.html) as well as [Confluent Hub](https://www.confluent.io/hub/datastax/kafka-connect-cassandra-sink).

### Create a connector properties file
We provide a `connect-standalone.properties.example` that is setup to run `kafka-connect-cassandra-sink-1.4.0.jar`. However, you will need to change:

  1) the name of the astra credentials zip file (cloud.secureConnectBundle). The path should be fine.
  2) Topic settings, particularly keyspace and tablename, unless tablename is already leaves, then only change keyspace (topic.record-cassandra-leaves-avro.<my_ks>.leaves.mapping)
  3) Astra db's password and username (auth.password)

Fields that require changing are marked by `### TODO make sure to change!` in the example file.

```
cd $PROJECT_HOME/kafka/connect
cp connect-standalone.properties.gitpod-example connect-standalone.properties
vim connect-standalone.properties
# ...
```

The worker properties file we provide (found at $PROJECT_HOME/kafka/connect/worker-properties/gitpod-avro-worker.properties) should work fine without modification.

### Setup Connect with Astra
- If you have not already, make sure that your Datastax astra secure connect bundle is downloaded 
- Place the secure creds bundle into astra.credentials

  ```
  mv ./path/to/astra.credentials/secure-connect-<database-name-in-astra>.zip $PROJECT_HOME/kafka/connect/astra.credentials/
  ```

## Start Kafka Connect

Before this, the kafka connect job tried to start but likely crashed since it needed your `connect-standalone.properties` file. Start it using:

```
$CONFLUENT_HOME/bin/connect-standalone $PROJECT_HOME/kafka/connect/worker-properties/gitpod-avro-worker.properties $PROJECT_HOME/kafka/connect/connect-standalone.properties
```

Don't forget to send some more messages:
```
cd $PROJECT_HOME/python
python3 data_importer.py --config-file-path configs/gitpod-config.ini
```

If you're not sure if it's working or not, before sending messages to Kafka using the data_importer.py, in the astra console you can delete records previously created using:

```
TRUNCATE <your_ks>.leaves;
```

Then send messages, and run a count
```
SELECT COUNT(*) FROM <your_ks>.leaves;
```
