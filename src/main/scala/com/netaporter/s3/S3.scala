package com.netaporter.s3

import akka.actor.{ ActorRef, ActorLogging, Actor }
import akka.util.Timeout
import com.netaporter.s3.S3.requests.{ DeleteBucket, PutBucket, GetBucket }
import com.netaporter.s3.S3.responses.{ DeleteBucketSuccess, PutBucketSuccess, S3Failure, ListBucketResult }
import scala.util.control.NoStackTrace
import scala.concurrent.Future
import scala.util.{ Failure, Success }
import spray.client.pipelining._
import scala.concurrent.duration._

object S3 {
  object requests {

    case class PutBucket(bucket: String, location: Location = US)
    case class DeleteBucket(bucket: String)
    case class GetBucket(bucket: String, prefix: String = "")

    case class Location(name: String)
    val EU = Location("EU")
    val US = Location("")
    val EUWest1 = Location("eu-west-1")
    val USWest1 = Location("us-west-1")
    val USWest2 = Location("us-west-2")
    val APSouthEast1 = Location("ap-southeast-1")
    val APSouthEast2 = Location("ap-southeast-2")
    val APNorthEast1 = Location("ap-northeast-1")
    val SAEast1 = Location("sa-east-1")
  }

  object responses {
    class S3Failure extends Exception with NoStackTrace
    case object Unauthorized extends S3Failure
    case object Forbidden extends S3Failure

    case class ListBucketResult(bucket: String, contents: Seq[Content])
    case class Content(key: String)

    case class DeleteBucketSuccess(bucket: String)
    case class PutBucketSuccess(bucket: String)
  }
}
class S3(transport: ActorRef, accessKeyId: String, secretAccessKey: String)
    extends Actor
    with ActorLogging
    with S3RequestBuilding
    with S3Unmarshallers {

  import context.dispatcher

  implicit val _timeout = Timeout(
    context.system.settings.config.getMilliseconds("s3.timeout").toLong.millis
  )

  val basePipeline = (
    signS3(accessKeyId, secretAccessKey)
    ~> logRequest(log)
    ~> sendReceive(transport)
    ~> logResponse(log)
  )

  def receive = {
    case PutBucket(bucket, location) =>
      val pipeline = basePipeline ~> s3Unmarshal[Unit]
      val res = pipeline(Put(s"http://$bucket.s3.amazonaws.com")).map(x => PutBucketSuccess(bucket))
      pipeToSender(res)

    case DeleteBucket(bucket) =>
      val pipeline = basePipeline ~> s3Unmarshal[Unit]
      val res = pipeline(Delete(s"http://$bucket.s3.amazonaws.com")).map(x => DeleteBucketSuccess(bucket))
      pipeToSender(res)

    case GetBucket(bucket, prefix) =>
      val pipeline = getBucket(prefix) ~> basePipeline ~> s3Unmarshal[ListBucketResult]
      val res = pipeline(Get(s"http://$bucket.s3.amazonaws.com"))
      pipeToSender(res)
  }

  def pipeToSender[T](f: Future[T]) = {
    val replyTo = sender
    f onComplete {
      case Success(msg) => replyTo ! msg
      case Failure(fail: S3Failure) => replyTo ! fail
      case fail => replyTo ! fail
    }
  }
}
