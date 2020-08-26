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

# Using Kafka Connect
## Upload Astra secure bundle
Place it here in Gitpod: 
/workspace/cassandra.realtime/connect-api-with-kafka-consumers/kafka/connect/astra.credentials/

## Set your configs
```
cp /workspace/cassandra.realtime/connect-api-with-kafka-consumers/kafka/connect/connect-standalone.properties.gitpod-example /workspace/cassandra.realtime/connect-api-with-kafka-consumers/kafka/connect/connect-standalone.properties
vim /workspace/cassandra.realtime/connect-api-with-kafka-consumers/kafka/connect/connect-standalone.properties
# ...
```

## Run Kafka Connect
${CONFLUENT_HOME}/bin/connect-standalone /workspace/cassandra.realtime/connect-api-with-kafka-consumers/kafka/connect/connect-standalone.properties
