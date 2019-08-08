curl -i -X POST \
	-H "Accept:application/json" \
	-H  "Content-Type:application/json" http://localhost:8083/connectors/ \
	-d '{"name": "csv-source-orders",
	 "config": {"tasks.max": "1",
		"connector.class": "com.github.jcustenborder.kafka.connect.spooldir.SpoolDirCsvSourceConnector",
		"input.file.pattern": "^orders.*.csv$",
	        "input.path": "/tmp/source",
		"finished.path": "/tmp/finished",
		"error.path": "/tmp/error",
		"halt.on.error": "false",
		"topic": "orders",
		"value.schema":"{\"name\":\"com.github.jcustenborder.kafka.connect.model.Value\",\"type\":\"STRUCT\",\"isOptional\":false,\"fieldSchemas\":{\"order_id\":{\"type\":\"INT64\",\"isOptional\":false},\"customer_id\":{\"type\":\"INT64\",\"isOptional\":false},\"order_ts\":{\"type\":\"STRING\",\"isOptional\":false},\"product\":{\"type\":\"STRING\",\"isOptional\":false},\"order_total_usd\":{\"type\":\"STRING\",\"isOptional\":false}}}",
		"key.schema":"{\"name\":\"com.github.jcustenborder.kafka.connect.model.Key\",\"type\":\"STRUCT\",\"isOptional\":false,\"fieldSchemas\":{\"order_id\":{\"type\":\"INT64\",\"isOptional\":false}}}",
		"csv.first.row.as.header": "true"
			}
	}'
