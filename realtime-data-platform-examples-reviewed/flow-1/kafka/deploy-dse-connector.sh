### https://docs.datastax.com/en/kafka/doc/kafka/kafkaNowFunction.html
curl \
     -X "POST" "http://172.20.10.18:8084/connectors/" \
     -H "Content-Type: application/json" \
     -d '{
      "name": "kafka-dse-connector-messages",
      "config": {
        "connector.class": "com.datastax.kafkaconnector.DseSinkConnector",
                "tasks.max": "1",
                "topics": "testMessage-avro",
                "contactPoints": "172.20.10.9",
                "loadBalancing.localDc": "dc1",
                "port": 9042,
                "maxConcurrentRequests": 10,
                "maxNumberOfRecordsInBatch": 20,
                "queryExecutionTimeout": 1000,
                "connectionPoolLocalSize": 1,
                "jmx": false,
                "compression": "None",
                "auth.provider": "None",

                "topic.testMessage-avro.customerkeyspace.messages_avro.mapping": "message_insert_time=now(), message_date_time=value.MessageDateTime, message_id=value.MessageID, message_type=value.MessageType, message_value=value.MessageValue",
                "topic.testMessage-avro.customerkeyspace.messages_avro.consistencyLevel": "ONE",
                "topic.testMessage-avro.customerkeyspace.messages_avro.ttl": -1,
                "topic.testMessage-avro.customerkeyspace.messages_avro.nullToUnset": "true",
                "topic.testMessage-avro.customerkeyspace.messages_avro.deletesEnabled": "true",

                "topic.testMessage-avro.codec.locale": "en_US",
                "topic.testMessage-avro.codec.timeZone": "UTC",
                "topic.testMessage-avro.codec.timestamp": "yyyy-MM-dd HH:mm:ss",
                "topic.testMessage-avro.codec.date": "ISO_LOCAL_DATE",
                "topic.testMessage-avro.codec.time": "ISO_LOCAL_TIME",
                "topic.testMessage-avro.codec.unit": "MILLISECONDS"
            }
        }'
