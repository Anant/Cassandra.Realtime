CREATE KEYSPACE csvFiles WITH replication = {'class':'SimpleStrategy', 'replication_factor' : 1};
create table csvFiles.orders(order_id int, customer_id int, order_ts text, product text, order_total_usd int, primary key (order_id,customer_id));
