FROM confluentinc/cp-kafka-connect:5.5.1
# NOTE user needs to set, by using export. Docker-compose will pass the vars in from the system env vars into here, unless defaults here are actually true
# replace your astra-db name for "astra" below
# export ASTRA_SECURE_BUNDLE_NAME="secure-connect-astra.zip"
ARG ASTRA_SECURE_BUNDLE_NAME

RUN ["mkdir", "-p", "/config"]
RUN ["mkdir", "-p", "/plugins"]
RUN ["mkdir", "-p", "/astra.credentials"]
COPY ./astra.credentials/$ASTRA_SECURE_BUNDLE_NAME /astra.credentials/$ASTRA_SECURE_BUNDLE_NAME
COPY ./connect-standalone.properties /config/connect-standalone.properties
# We want to use their "run" script still, which gets called by the image's EXEC in their dockerfile, since it sets up everything
# - see https://github.com/confluentinc/cp-docker-images/blob/2d1072207790a06b88b79fb129f72bb41b67532c/debian/kafka-connect-base/Dockerfile#L52
# However, we need to overwrite their "launch" script, so that we launch in standalone mode and with the Cassandra connector and our config file
#
COPY ./launch /etc/confluent/docker/launch

# copy connector jar into our plugin path. Note that by default the confluent kafka connect docker image uses /etc/kafka-connect/jars/* instead. But we only want this one connector, which they don't include.
COPY ./kafka-connect-cassandra-sink-1.4.0.jar /plugins/kafka-connect-cassandra-sink-1.4.0.jar
