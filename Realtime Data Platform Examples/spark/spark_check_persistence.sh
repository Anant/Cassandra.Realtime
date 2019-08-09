docker exec -i cassandra cqlsh -e 'select count(*) from streaming_test.words_table'
