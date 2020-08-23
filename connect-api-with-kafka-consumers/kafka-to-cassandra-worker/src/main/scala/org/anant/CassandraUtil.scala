package org.anant

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scalaj.http._
import scala.util.{Try, Success, Failure}
import scala.collection.mutable.Map


object CassandraUtil {

  def writeToDb(apiHost : String, jsonData : String, callback : Try[HttpResponse[String]] => Unit, options : Map[String, Boolean]) {
    val debugMode : Boolean = options("debugMode")

    // async http request
    val f : Future[HttpResponse[String]] = Future {
      val response: HttpResponse[String] = Http(s"${apiHost}/api/leaves").postData(jsonData : String).header("content-type", "application/json").asString;

      if (199 < response.code && response.code < 300) {
        println("Successful write to C*!")

      } else if (response.code > 399) {
        throw new Exception(s"Error sending to Cassandra: Code: ${response.code}. Message: ${response.body}")

      } else {
        if (debugMode) {
          println(s"Another code: ${response.code}")
          println(s"Response: ${response.body}")
        }
      }

      // output of the future
      response
    }

    f onComplete callback
  }
}
