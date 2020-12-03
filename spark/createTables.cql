//Create Tables in Astra Studio

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


//Create Tables in CQLSH

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