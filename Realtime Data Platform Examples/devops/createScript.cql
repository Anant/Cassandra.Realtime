CREATE KEYSPACE akka
    WITH replication = {'class':'SimpleStrategy', 'replication_factor' : 1};

create table akka.salesjan2009(
        Account_Created text,
        Payment_Type text,
        Name text,
        Last_Login text,
        Transaction_date text,
        Latitude text,
        Country text,
        Longitude text,
        Price text,
        City text,
        Product text,
        State text,
    primary key (Transaction_date)
   );
