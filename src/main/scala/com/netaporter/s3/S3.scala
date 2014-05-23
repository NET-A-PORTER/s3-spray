package com.netaporter.s3

import akka.actor.{ ActorRef, ActorLogging, Actor }
import akka.util.Timeout
import com.netaporter.s3.S3.requests._
import com.netaporter.s3.S3.responses._
import scala.util.control.NoStackTrace
import scala.concurrent.Future
import spray.client.pipelining._
import scala.concurrent.duration._
import spray.http.HttpRequest
import com.netaporter.s3.S3.requests.GetBucket
import com.netaporter.s3.S3.responses.DeleteBucketSuccess
import scala.util.Failure
import com.netaporter.s3.S3.responses.PutBucketSuccess
import com.netaporter.s3.S3.requests.DeleteBucket
import com.netaporter.s3.S3.responses.S3Response
import spray.http.HttpResponse
import com.netaporter.s3.S3.responses.S3Request
import scala.util.Success
import com.netaporter.s3.S3.responses.ListBucketResult
import com.netaporter.s3.S3.requests.PutBucket

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

    case class GetObject(bucket: String, objectName: String)
  }

  object responses {
    class S3Failure extends Exception with NoStackTrace
    case object Unauthorized extends S3Failure
    case object Forbidden extends S3Failure

    case class ListBucketResult(bucket: String, contents: Seq[Content])
    case class Content(key: String)

    case class DeleteBucketSuccess(bucket: String)
    case class PutBucketSuccess(bucket: String)

    case class GetObjectResult(data: Seq[Byte])

    case class S3Request(http: HttpRequest)
    case class S3Response(http: HttpResponse)
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

  val scheme = context.system.settings.config.getString("s3.scheme")
  val publishEvents = context.system.settings.config.getBoolean("s3.publish-event-stream")

  val publishRequest: RequestTransformer = { r =>
    if (publishEvents) { context.system.eventStream.publish(S3Request(r)) }; r
  }

  val publishResponse: ResponseTransformer = { r =>
    if (publishEvents) { context.system.eventStream.publish(S3Response(r)) }; r
  }

  val basePipeline = (
    signS3(accessKeyId, secretAccessKey)
    ~> logRequest(log)
    ~> publishRequest
    ~> sendReceive(transport)
    ~> logResponse(log)
    ~> publishResponse
  )

  def receive = {
    case PutBucket(bucket, location) =>
      val pipeline = basePipeline ~> s3Unmarshal[Unit]
      val res = pipeline(Put(s"$scheme://$bucket.s3.amazonaws.com/")).map(x => PutBucketSuccess(bucket))
      pipeToSender(res)

    case DeleteBucket(bucket) =>
      val pipeline = basePipeline ~> s3Unmarshal[Unit]
      val res = pipeline(Delete(s"$scheme://$bucket.s3.amazonaws.com/")).map(x => DeleteBucketSuccess(bucket))
      pipeToSender(res)

    case GetBucket(bucket, prefix) =>
      val pipeline = getBucket(prefix) ~> basePipeline ~> s3Unmarshal[ListBucketResult]
      val res = pipeline(Get(s"$scheme://$bucket.s3.amazonaws.com/"))
      pipeToSender(res)

    case GetObject(bucket, objectName) =>
      val pipeline = basePipeline ~> s3Unmarshal[GetObjectResult]
      val res = pipeline(Get(s"$scheme://$bucket.s3.amazonaws.com/$objectName"))
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
