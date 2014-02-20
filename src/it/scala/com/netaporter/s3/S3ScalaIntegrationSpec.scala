package com.netaporter.s3

import org.scalatest.{BeforeAndAfterAll, WordSpecLike, Matchers}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import akka.actor.{Props, ActorSystem}
import spray.can.Http
import akka.io.IO
import java.util.UUID
import com.netaporter.s3.S3.requests.{DeleteBucket, GetBucket, PutBucket}
import com.netaporter.s3.S3.responses.{DeleteBucketSuccess, ListBucketResult, PutBucketSuccess}
import scala.concurrent.duration._

class S3ScalaIntegrationSpec
  extends TestKit(ActorSystem())
  with ImplicitSender
  with WordSpecLike
  with Matchers
  with BeforeAndAfterAll {

  val accessKeyId = sys.env("S3_ACCESS_KEY_ID")
  val secretAccessKey = sys.env("S3_SECRET_ACCESS_KEY")

  val s3 = TestActorRef(Props(classOf[S3], IO(Http), accessKeyId, secretAccessKey))

  override def afterAll() { system.shutdown() }

  val dur = 10.seconds

  "S3" should {
    "PUT, GET and DELETE a bucket" in {
      val bucketName = UUID.randomUUID.toString
      s3 ! PutBucket(bucketName)
      expectMsg(dur, PutBucketSuccess(bucketName))

      s3 ! GetBucket(bucketName)
      val res = expectMsgType[ListBucketResult](dur)
      res.bucket should equal(bucketName)
      res.contents should have size 0

      s3 ! DeleteBucket(bucketName)
      expectMsg(dur, DeleteBucketSuccess(bucketName))
    }
  }
}