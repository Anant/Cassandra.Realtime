# Getting started with Cassandra and Spark!

This project is part of the **Event Driven Toolkit for Kafka & Cassandra** initiative from Anant
where we build step-by-step and distributed message processing architecture.

![Splash](/screenshots/splash.png)

## 📚 Table of Contents

✨ This is episode 3

| Description and Link | Tools
|---|---|
| 1. [Reminders on Episode 1, start Cassandra API](#1-reminders-on-episode-1-setup-cassandra-api) | Node, Python,Astra |
| 2. [ Start and Setup Apache Kafka™ ](#2-start-and-setup-apache-kafka) | Api, Kafka |
| 3. [ Write into Cassandra](#3-consume-from-kafka-write-to-cassandra) | Astra, Kafka |
| 4. [Run Apache Spark Jobs against DataStax Astra](#4-run-apache-spark-jobs-against-datastax-astra) | Astra, Spark, Kafka |

## 1. Reminders on Episode 1, setup Cassandra API

This work has been realized during first workshop. The procedure is described step-by-step in the following [README](https://github.com/Anant/cassandra.api/blob/master/README.md).

For reference, recording of first episode is [available on youtube](https://www.youtube.com/watch?v=kRYMwOl6Uo4)

ℹ️ **Informations** : During this session we implemented the API both in NodeJS (express) and Python (Flask) pick the one you like most for today. We recommend naming your db table `leaves` in order to keep it simple when following this demo, but you can use a different tablename, as long as you change the tablename throughout the rest of the demo to use the same table.

### 1.a - Open Cassandra.API in Gitpod

[Gitpod](http://www.gitpod.io/?utm_source=datastax&utm_medium=referral&utm_campaign=datastaxworkshops) is an IDE 100% online based on Eclipse Theia. To initialize your environment simply click on the button below *(CTRL + Click to open in new tab)*

- To initialize the **Cassandra API** in Gitpod 
- Click on the button below *(CTRL + Click to open in new tab)* =>  [![Open in Gitpod](https://gitpod.io/button/open-in-gitpod.svg)](https://gitpod.io/#https://github.com/Anant/cassandra.api)

*Expected Output*
![gitpod_api](/screenshots/gitpod_cassandra_api.png)

### 1.b - Setup the Cassandra.API in Gitpod

To allow best connectivity make sure your REST API's port 8000 is exposed, so that we can send requests to it later:
![Make port 8000 public](/screenshots/open-port-8000.png)

ℹ️ **Informations** : If you don't use this gitpod workspace frequently enough, it will timeout and spin down. If this happens, you can just reopen the workspace and restart the server (using `npm start` for NodeJS or `python3 app.py` for Python).

### 1.c - Get url for future reference

When we will tell Kafka Consumer where to send events we will need the public URL for the API.

- **✅ To get it use:**

```bash
gp url 8000
```

*Expected Output*
![Get url for future reference](/screenshots/get-url-for-rest-api.png)

*This is what you have running as of now:*
![Get url for future reference](/screenshots/episode1.png)

## 2. Start and Setup Apache Kafka™ 

### 2.a - Open Cassandra.Realtime in Gitpod
As before, initialize your environment by simply click on the button below *(CTRL + Click to open in new tab)*. This will open a **second** gitpod workspaces. They will communicate to each other.

[![Open in Gitpod](https://gitpod.io/button/open-in-gitpod.svg)](https://gitpod.io/#https://github.com/Anant/cassandra.realtime)

💡 **ProTip** : To view README in preview mode from Gitpod, right click on the file and select `Open With > Preview`:
![Open README Preview](/screenshots/open-readme-preview.png )

```
⚠️ By default Autosave is not enabled in Gitpod. Don't forget to save your modifications with CTRL+S
```

### 2.b - Setup Kafka

Make sure Kafka services are up by running `confluent local start`. Note that you don't need to start kafka connect yet (and indeed, it won't work until we set it up later on in this demo), but the others should be up. 

- **✅  You can check with the confluent cli:**

```bash
confluent local status

# if some are not up yet (running again doesn't hurt anything, so you can just run this either way):
confluent local start
```

*Expected Output*
![confluent local start](/screenshots/confluent-local-services-start.png)

ℹ️ **Informations** : that the specific command you use in the Confluent CLI depends on the version of CLI you are using. Newer versions of the CLI will require you to use `confluent local services start`. In gitpod, we downloaded v.1.6.0 for you, so you can use the shorter syntax: `confluent local <cmd>`.

### 2.c - Create a topic

If you are in gitpod, we set `$CONFLUENT_HOME` for you. It points to where your confluent binary directory is (`/home/gitpod/lib/confluent-5.5.1`). If you are not running this in gitpod, you will have to set `$CONFLUENT_HOME` yourself.

- **✅ Execute this to create a topic `record-cassandra-leaves-avro`**

```bash
  $CONFLUENT_HOME/bin/kafka-topics --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic record-cassandra-leaves-avro
```

*Expected Output*
![create a topic](/screenshots/create-kafka-topic.png)

- **✅ Check that topic `record-cassandra-leaves-avro` now exist**

```bash
$CONFLUENT_HOME/bin/kafka-topics --list --zookeeper localhost:2181
```

*Expected Output*
![check topic](/screenshots/check-kafka-topic.png)

### 2.d Create the Kafka schema for topic's messages value

Make sure your python environment has `requests` and other modules installed. 

- **✅ You can install it using our requirements.txt file:**

```bash
cd $PROJECT_HOME
pip install -r python/requirements.txt
```

If you are in gitpod, we set `$PROJECT_HOME` for you. It is an absolute path to where this directory is inside this repo (`/workspace/cassandra.realtime`). If you are not running this in gitpod, you will have to set `$PROJECT_HOME` yourself.

*Expected Output*
![install requirements](screenshots/install-requirements.txt.png)

- **✅  Create the schema**

```bash
python ./kafka/create-schema.py http://localhost:8081 record-cassandra-leaves ./kafka/leaves-record-schema.avsc
```
*Expected Output*
![create schema](/screenshots/create-schema.png)

- **✅  Check that schema exists**

```bash
curl http://127.0.0.1:8081/subjects
# should return: ["record-cassandra-leaves-value"]
```

- **✅  Alternatively you can check AKHQ. Run this to start AKHQ**

```bash
java -Dmicronaut.config.files=$PROJECT_HOME/kafka/akhq/gitpod-akhq-config.yml -jar ${BINARY_DIR}/akhq.jar
```

You can see the AKHQ GUI at `http://127.0.0.1:8080/`. If you are using gitpod, we exposed `8080` for you by default. You can double check by clicking down here.

*Expected Output*
![view ports](/screenshots/open-ports-popup.png)


>💡 **ProTip** : Use this single-line command to open a preview for port 8080 in gitpod:  
 
```bash
gp preview $(gp url 8080)
```
> To see the AKHQ Schema registry view specifically:
```
gp preview $(gp url 8080)/ui/docker-kafka-server/schema
```

*Expected Output*
![schema registry](/screenshots/akhq-schema-registry.png)


### 2.e - Import the data into Kafka

We are now ready to start sending messages to Kafka.

- **✅  Import data with importer**

```bash
cd $PROJECT_HOME/python
pip install -r requirements.txt
python3 data_importer.py --config-file-path configs/gitpod-config.ini
```

*Expected Output*
![produce to Kafka](/screenshots/produce-to-kafka-stdout.png)

- **✅  Confirm that the message arrived in Kafka Topics**

You can check the topic that has the schema using `kafka-avro-console-consumer`:
*(🚨🚨🚨 : can potentially have lots of output)*

```bash
$CONFLUENT_HOME/bin/kafka-avro-console-consumer --topic record-cassandra-leaves-avro --bootstrap-server localhost:9092 --from-beginning --property schema.registry.url=http://localhost:8081
```

## 3. Consume from Kafka, write to Cassandra

### 3.a - Execute the scala job to pick up messages from Kafka, deserialize and write them to Cassandra

- **✅  Edit the `gitpod-project.properties`file with the url of your running cassandra.api instance.**

You will need to change the `api.host` key. It will look something like `api.host=https://8000-c0f5dade-a15f-4d23-b52b-468e334d6abb.ws-us02.gitpod.io`. Again you can find it by running the following command in the gitpod instance running cassandra.api: `gp url 8000`.

Change the `cassandra.keyspace` as well to whatever your keyspace is in Astra.

> ℹ️ **Note** : if you don't do this, the consumer will still run, but will just fail to write to Cassandra, since its current setting isn't stopping on errors.

```bash
cd $PROJECT_HOME/kafka-to-cassandra-worker/src/main/resources/
cp gitpod-project.properties.example gitpod-project.properties
vim gitpod-project.properties
#...
```

- **✅ Package the project**

```bash
cd $PROJECT_HOME
mvn -f ./kafka-to-cassandra-worker/pom.xml clean package
```

This will install dependencies and package your jar. If you make changes to your `gitpod-project.properties` file, make sure to run `mvn clean package again`, using `-f` flag to point to the `pom.xml` file. 

- **✅ Run the project**

There should now be two jars in `./kafka-to-cassandra-worker/target`, one with-dependencies, one without. We'll use the one with dependencies:

```bash
cd $PROJECT_HOME
mvn -f ./kafka-to-cassandra-worker/pom.xml exec:java -Dexec.mainClass="org.anant.KafkaAvroConsumer" -Dexec.args="kafka-to-cassandra-worker/target/classes/gitpod-project.properties"
```

> **Note:** if your Cassandra.api gitpod workspace timed out, you might need to reopen it and restart the REST API server. Offset is at `latest`, so you won't see anything unless you have messages actively coming in.

- **✅ Send more messages whenever you want to by re-running the python script**

```bash
cd $PROJECT_HOME/python
python data_importer.py --config-file-path configs/gitpod-config.ini
```

- **✅ confirm we are consuming the correct topic using AKHQ, at `/ui/docker-kafka-server/topic`.**
  
```bash
gp preview $(gp url 8080)/ui/docker-kafka-server/topic
```

(If AKHQ was already on that page, make sure to refresh the view). You should see our consumer group (`send-to-cassandra-api-consumer`) listed as a consumer on topic `record-cassandra-leaves-avro`:

*Expected Output*
![Topics in AKHQ](/screenshots/akhq-topics.png)

### 3.b - Sending messages to Kafka using Kafka REST Proxy

- **✅ Check your topics**

```bash
curl http://localhost:8082/topics/
curl http://localhost:8082/topics/record-cassandra-leaves-avro
```

- **✅ Send using data importer's rest proxy mode**

```bash
cd $PROJECT_HOME/python
python3 data_importer.py --config-file-path configs/gitpod-rest-proxy-config.ini
```

There should now be new messages for you to consume in your Kafka topic.

*Expected output*
![Rest Proxy](/screenshots/rest-proxy.png)

### 3.c - Process messages using Kafka Streams and writing to Cassandra using Processor API

You can use the Kafka processor API if you want to send messages to Cassandra using the REST API we are using.

- **✅ Send message to Cassandra**

```bash
cd $PROJECT_HOME
mvn  -f ./kafka-to-cassandra-worker/pom.xml exec:java -Dexec.mainClass="org.anant.KafkaStreamsAvroConsumer" -Dexec.args="kafka-to-cassandra-worker/target/classes/gitpod-project.properties"
```

Make sure to keep sending messages in another terminal or nothing will happen. You can use the same command as before:
```bash
cd $PROJECT_HOME/python
python3 data_importer.py --config-file-path configs/gitpod-rest-proxy-config.ini
```

### 3.d - Writing to Cassandra using Kafka Connect

We used the Processor API to show what it would look like to write to Cassandra using Kafka Streams and a REST API, but it is generally recommended to use Kafka Connect. We will be using the [Datastax connector](https://www.confluent.io/hub/datastax/kafka-connect-cassandra-sink), but there is also a Confluence Cassandra connector as well as other third party connectors available if you are interested. 

### 3.e - Setup Kafka Connect

The Datastax Kafka connector also has instructions and a download link from [the Datastax website](https://docs.datastax.com/en/kafka/doc/kafka/install/kafkaInstall.html) as well as [Confluent Hub](https://www.confluent.io/hub/datastax/kafka-connect-cassandra-sink).

### 3.f - Create a connector properties file

We provide a `connect-standalone.properties.example` that is setup to run `kafka-connect-cassandra-sink-1.4.0.jar`. However, you will need to change:

1. the name of the astra credentials zip file (cloud.secureConnectBundle). The path should be fine.
2. Topic settings, particularly keyspace and tablename, unless tablename is already leaves, then only change keyspace `(topic.record-cassandra-leaves-avro.<my_ks>.leaves.mapping)`
3. Astra dbs password and username (auth.password)

Fields that require changing are marked by `### TODO make sure to change!` in the example file.

- **✅ Edit `connect-standalone.properties.example`**

```bash
cd $PROJECT_HOME/kafka/connect
cp connect-standalone.properties.gitpod-example connect-standalone.properties
vim connect-standalone.properties
# ...
```

The worker properties file we provide (found at `$PROJECT_HOME/kafka/connect/worker-properties/gitpod-avro-worker.properties`) should work fine without modification in gitpod. However, if you are not using gitpod, you will need to change `/workspace/cassandra.realtime` in the plugin path if you are not using gitpod, to whatever your $PROJECT_HOME is. 

### 3.g - Setup Connect with Astra

REMINDER create you Astra Account [here](https://dtsx.io/workshop) 

If you have not already, make sure that your Datastax astra secure connect bundle is downloaded.

- **✅ Get the secure cloud bundle**

Display the summary screen and locate the `connect` button.

![pic](https://github.com/datastaxdevs/shared-assets/blob/master/astra/summary-1000-connect.png?raw=true)

On the connect screen pick `drivers`

![pic](https://github.com/datastaxdevs/shared-assets/blob/master/astra/connect-rest-driver.png?raw=true)

Finally click the download secure bundle button to download the zip of right-click to the button to get the url 

![pic](https://github.com/datastaxdevs/shared-assets/blob/master/astra/connect-driver-1000.png?raw=true)

- **✅ Place the secure creds bundle into astra.credentials**

If you copied the link....
```bash
cd $PROJECT_HOME/kafka/connect/astra.credentials/
curl -L "<YOU_LINK>"  > secure-connect-<database-name-in-astra>.zip
```

if you have the zip, upload file to gitpod with menu or drag and drop it into `$PROJECT_HOME/kafka/connect/astra.credentials/`
```bash
mv ./path/to/astra.credentials/secure-connect-<database-name-in-astra>.zip $PROJECT_HOME/kafka/connect/astra.credentials/
```

### 3.h - Start Kafka Connect

Start Kafka connect using your `connect-standalone.properties` file. First you will have to stop the service that the confluent cli started. 

- **✅ Start Kafka-Connect**

```bash
confluent local stop connect
$CONFLUENT_HOME/bin/connect-standalone $PROJECT_HOME/kafka/connect/worker-properties/gitpod-avro-worker.properties $PROJECT_HOME/kafka/connect/connect-standalone.properties
```

*Expected output*
![kafka connect logs](/screenshots/kafka-connect-logs-gp.png)

- **✅ Send more messages in a separate terminal**

```bash
cd $PROJECT_HOME/python
python3 data_importer.py --config-file-path configs/gitpod-config.ini
```

If you're not sure if it's working or not, before sending messages to Kafka using the data_importer.py, in the astra console you can delete records previously created using:

```sql
TRUNCATE <your_ks>.leaves;
```

Then send messages, and run a count

```sql
SELECT COUNT(*) FROM <your_ks>.leaves;
```
![astra count](/screenshots/astra-count-all-leaves.png)

## 4. Run Apache Spark Jobs Against DataStax Astra

### 4.a Setup

- **✅ Download Apache Spark 3.0.1**
```bash
curl -L -s https://apache.osuosl.org/spark/spark-3.0.1/spark-3.0.1-bin-hadoop2.7.tgz | tar xvz -C /workspace/cassandra.realtime/spark
```

- **✅ Download sbt 1.4.3**
```bash
curl -L -s https://github.com/sbt/sbt/releases/download/v1.4.3/sbt-1.4.3.tgz | tar xvz -C /workspace/cassandra.realtime/spark
```

- **✅ Drag-and-drop a Copy of Your Secure Connect Bundle into the `/spark` directory**

- **✅ Create 2 tables in DataStax Astra**
For Astra Studio
```sql
CREATE TABLE leaves_by_tag (
   tag text,
   title text,
   tags list<text>,
   url text,
   PRIMARY KEY ((tag), title)
);

CREATE TABLE tags (
   tag text,
   count int,
   PRIMARY KEY (tag)
);
```

For CQLSH
```sql
CREATE TABLE <your-keyspace>.leaves_by_tag (
   tag text,
   title text,
   tags list<text>,
   url text,
   PRIMARY KEY ((tag), title)
);

CREATE TABLE <your-keyspace>.tags (
   tag text,
   count int,
   PRIMARY KEY (tag)
);
```

### 4.b Start Apache Spark in Standalone Cluster Mode with 1 worker
- **✅ Open a new terminal and start master**
```bash
cd $PROJECT_HOME/spark/spark-3.0.1-bin-hadoop2.7/
./sbin/start-master.sh
```

- **✅ Start worker**
```./sbin/start-slave.sh <master-url>```

>💡 **ProTip** : Use this single-line command to open a preview for port 8080 in gitpod to get the Spark master URL:  
 
```bash
gp preview $(gp url 8080)
```

### 4.c Start sbt Server in `spark-cassandra` directory
- **✅ Open a new terminal and start sbt server**
```bash
cd $PROJECT_HOME/spark/spark-cassandra/
../sbt/bin/sbt
```
*Expected Output*
This may take a minute, but you should see this when done:


### 4.d Add User Specific Configs into Job files
- **✅ Open `/spark-cassandra/src/main/scala/leavesByTag.scala**`
  - **Edit lines 21-25 with your specific configs**
  - **Save file**
- **✅ Open `/spark-cassandra/src/main/scala/tags.scala**`
  - **Edit lines 13-17 with your specific configs**
  - **Save file**

### 4.e Create Fat JAR
- **✅ Run `assembly` in sbt server terminal**
*Expected Output*

### 4.f Run 1st Apache Spark Job
In the first job, we are going to read the Kafka stream, manipulate the data, and save the data into the leaves_by_tag table we created earlier.

- **✅ Go to the terminal that we used to start Apache Spark in standalone mode and run the below code block with your specific database name in the designated spot for the --files option**
```bash
./bin/spark-submit --class sparkCassandra.LeavesByTag \
--files /workspace/cassandra.realtime/spark/secure-connect-<your-db-name>.zip \
/workspace/cassandra.realtime/spark/spark-cassandra/target/scala-2.12/spark-cassandra-assembly-0.1.0-SNAPSHOT.jar
```

*Expected Output Once the Job is Watching for the Kafka Stream*

### 4.g Run Kafka Connect

- **✅ If you stopped Kafka Connect, restart it in a seperate terminal**

```bash
$CONFLUENT_HOME/bin/connect-standalone $PROJECT_HOME/kafka/connect/worker-properties/gitpod-avro-worker.properties $PROJECT_HOME/kafka/connect/connect-standalone.properties
```

*Expected output*
![kafka connect logs](/screenshots/kafka-connect-logs-gp.png)

- **✅ Send more messages in a separate terminal**

```bash
cd $PROJECT_HOME/python
python3 data_importer.py --config-file-path configs/gitpod-config.ini
```

### 4.h Confirm Data Was Written to Astra
- **✅ Stop Spark Job with `CTRL + C` once there is a steady stream of the following in the terminal**
*Expected Output*

- **✅ Check count of rows with the tag of 'spark' in CQLSH or Astra Studio**
CQLSH:
```sql 
select tag, count(*) from <your-keyspace>.leaves_by_tag where tag='spark';
```

*Expected Output*

Astra Studio:
```sql
select tag, count(*) from leaves_by_tag where tag='spark';
```

*Expected Output*

### 4.i Run Second Apache Spark Job
In this job, we are going to take the data we sent via Kafka into the leaves table, transform it with Apache Spark, and write the transformed data into the tags table we created during setup.

- **✅ Run the following code block in terminal you previously ran the first Spark Job. Again, be sure to input your specific database name in the --files option where designated
```bash
./bin/spark-submit --class sparkCassandra.Tags \
--files /workspace/cassandra.realtime/spark/secure-connect-<your-database-name>.zip \
/workspace/cassandra.realtime/spark/spark-cassandra/target/scala-2.12/spark-cassandra-assembly-0.1.0-SNAPSHOT.jar
```

The job will complete on its own, so you do not have to manually end it. 

*Expected Output*

### 4.j Confirm Data was Written To Astra
- **✅ Check count of rows with the tag of 'spark' in CQLSH or Astra Studio**
CQLSH:
```sql
select * from <your-keyspace>.tags where tag='spark';
```

*Expected Output*

Astra Studio:
```sql
select * from tags where tag='spark';
```

*Expected Output*

## THE END



