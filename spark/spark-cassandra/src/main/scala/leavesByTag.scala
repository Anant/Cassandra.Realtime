package sparkCassandra

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.sql.SQLContext
import org.apache.spark.sql.functions._
import com.datastax.spark.connector._
import com.datastax.spark.connector.rdd._
import org.apache.spark.sql.cassandra._
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.avro.functions._
import java.nio.file.{Files, Paths}
import za.co.absa.abris.config.AbrisConfig
import org.apache.spark.sql.ForeachWriter
import org.apache.spark.sql.Row

object LeavesByTag {

    def main(args: Array[String]){

        val username = ""
        val password = ""
        val masterURL = ""
        val dbName = ""
        val keyspace = ""

        val spark = SparkSession
            .builder()
            .appName("Leaves")
            .master(masterURL)
            .config("spark.cassandra.connection.config.cloud.path", s"secure-connect-$dbName.zip")
            .config("spark.cassandra.auth.username", username)
            .config("spark.cassandra.auth.password", password)
            .config("spark.sql.extensions", "com.datastax.spark.connector.CassandraSparkExtensions")
            .getOrCreate()
        
        import spark.implicits._

        val df = spark
        .readStream
        .format("kafka")
        .option("kafka.bootstrap.servers", "localhost:9092")
        .option("subscribe", "record-cassandra-leaves-avro")
        .option("mode", "permissive")
        .load()

        val abrisConfig = AbrisConfig
            .fromConfluentAvro
            .downloadReaderSchemaByLatestVersion
            .andTopicNameStrategy("record-cassandra-leaves-avro")
            .usingSchemaRegistry("http://localhost:8081")

        import za.co.absa.abris.avro.functions.from_avro
        val setupDF = df.select(from_avro(col("value"), abrisConfig) as 'data)

        setupDF.createOrReplaceTempView("data")

        val leaves_by_tag = spark.sql("select data.tags as tag, data.title as title, data.url as url, data.tags as tags from data").withColumn("tag", explode($"tag"))

        val leavesByTagQuery = leaves_by_tag.writeStream
        .option("checkpointLocation", "/tmp/checkpoint/leavesByTag")
        .cassandraFormat("leaves_by_tag", keyspace)
        .outputMode("append")
        .start()

        leavesByTagQuery.awaitTermination();
    
        spark.stop()
    }
}
