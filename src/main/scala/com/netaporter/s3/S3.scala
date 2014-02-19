package com.netaporter.s3

import akka.actor.{ ActorRef, ActorLogging, Actor }
import akka.util.Timeout
import com.netaporter.s3.S3.requests.GetBucket
import com.netaporter.s3.S3.responses.ListBucketResult

object S3 {
  object requests {
    case class GetBucket(bucket: String, prefix: String = "")
  }

  object responses {
    case class ListBucketResult(bucket: String, contents: Seq[Content])
    case class Content(key: String)
  }
}
class S3(transport: ActorRef, accessKeyId: String, secretAccessKey: String)(implicit t: Timeout)
  extends Actor 
  with ActorLogging
  with S3RequestBuilding
  with S3Unmarshallers {

  val basePipeline = (
      signS3(accessKeyId, secretAccessKey)
      ~> logRequest(log)
      ~> sendReceive(transport)
      ~> logResponse(log)
    )

  def receive = {
    case GetBucket(bucket, prefix) =>
      val pipeline = getBucket(prefix) ~> basePipeline ~> unmarshal[ListBucketResult]
      pipeline(Get(s"http://$bucket.s3.amazonaws.com")) pipeTo sender
  }
}
