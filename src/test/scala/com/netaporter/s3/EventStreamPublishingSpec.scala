package com.netaporter.s3

import akka.testkit.{ TestActorRef, TestProbe, TestKit }
import akka.actor.{ Props, ActorSystem }
import org.scalatest.{ Matchers, FlatSpecLike }
import com.netaporter.s3.S3.requests.GetBucket
import spray.http.{ HttpResponse, HttpRequest }
import spray.http.StatusCodes._
import com.netaporter.s3.S3.responses.{ S3Response, S3Request }

class EventStreamPublishingSpec
    extends TestKit(ActorSystem())
    with FlatSpecLike
    with Matchers {

  val transport = TestProbe()
  val s3 = TestActorRef(Props(classOf[S3], transport.ref, "test-access-id", "test-secret"), "s3")

  "S3 Actor" should "publish requests and responses on the Akka event stream" in {
    val subscriber = TestProbe()
    system.eventStream.subscribe(subscriber.ref, classOf[S3Request])
    system.eventStream.subscribe(subscriber.ref, classOf[S3Response])

    s3 ! GetBucket("test-bucket")
    val s3Request = subscriber.expectMsgType[S3Request]
    val transportRequest = transport.expectMsgType[HttpRequest]
    s3Request.http should equal(transportRequest)

    val transportResponse = HttpResponse(EnhanceYourCalm)
    transport.reply(transportResponse)
    val s3Response = subscriber.expectMsgType[S3Response]
    s3Response.http should equal(transportResponse)
  }
}
