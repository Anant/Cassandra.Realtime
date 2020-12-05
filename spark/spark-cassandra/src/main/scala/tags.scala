package sparkCassandra

import org.apache.spark.sql.functions._
import com.datastax.spark.connector._
import org.apache.spark.sql.cassandra._
import org.apache.spark.sql.SparkSession

object Tags {

    def main(args: Array[String]){

        val spark = SparkSession
            .builder()
            .getOrCreate()

        val dbName = spark.sparkContext.getConf.get("spark.database.name")
        val keyspace = spark.sparkContext.getConf.get("spark.keyspace.name")

        spark.conf.set(s"spark.sql.catalog.$dbName", "com.datastax.spark.connector.datasource.CassandraCatalog")

        import spark.implicits._
        
        spark.sql(s"use $dbName.$keyspace")

        val tagsDF = spark.sql("select tags as tag from leaves").withColumn("tag", explode($"tag")).groupBy("tag").count()

        tagsDF.write.cassandraFormat("tags", keyspace).mode("append").save()

        spark.stop()
    }
}