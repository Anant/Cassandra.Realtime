ThisBuild / scalaVersion     := "2.12.8"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.anant"
ThisBuild / organizationName := "Anant"

resolvers += ("confluent" at "http://packages.confluent.io/maven/").withAllowInsecureProtocol(true)

libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % "3.0.5",
    "org.apache.spark" %% "spark-core" % "3.0.1" % "provided",
    "org.apache.spark" %% "spark-sql" % "3.0.1" % "provided",
    "com.datastax.spark" %% "spark-cassandra-connector" % "3.0.0",
    "org.apache.spark" %% "spark-sql-kafka-0-10" % "3.0.1",
    "org.apache.spark" %% "spark-avro" % "3.0.1",
    "za.co.absa" %% "abris" % "4.0.1",
    "io.confluent" % "kafka-avro-serializer" % "5.3.4",
    "io.confluent" % "kafka-schema-registry-client" % "5.5.1",
    "io.confluent" % "common-config" % "5.3.4",
    "io.confluent" % "common-utils" % "5.3.4"
)

mergeStrategy in assembly := {
  case PathList("org", "apache", "spark", "unused", "UnusedStubClass.class") => MergeStrategy.first
  case PathList("javax", "ws", "rs", "client", "ClientBuilder.class") => MergeStrategy.first
  case PathList("javax", "ws", "rs", "client", "FactoryFinder.class") => MergeStrategy.first
  case PathList("javax", "ws", "rs", "core", "Response$ResponseBuilder.class") => MergeStrategy.first
  case PathList("javax", "ws", "rs", "core", "Response$Status$Family.class") => MergeStrategy.first
  case PathList("javax", "ws", "rs", "core", "Response$Status.class") => MergeStrategy.first
  case PathList("javax", "ws", "rs", "core", "Response$StatusType.class") => MergeStrategy.first
  case PathList("javax", "ws", "rs", "core", "Response.class") => MergeStrategy.first
  case PathList("javax", "ws", "rs", "ext", "FactoryFinder.class") => MergeStrategy.first
  case PathList("javax", "ws", "rs", "ext", "MessageBodyWriter.class") => MergeStrategy.first
  case PathList("javax", "ws", "rs", "ext", "RuntimeDelegate.class") => MergeStrategy.first
  case PathList("javax", "ws", "rs", "sse", "FactoryFinder.class") => MergeStrategy.first
  case PathList("javax", "ws", "rs", "sse", "SseEventSource$Builder.class") => MergeStrategy.first
  case PathList("javax", "ws", "rs", "sse", "SseEventSource.class") => MergeStrategy.first
  case PathList("module-info.class") => MergeStrategy.first

  case x => (mergeStrategy in assembly).value(x)
}
